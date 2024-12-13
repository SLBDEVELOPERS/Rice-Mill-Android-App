package com.example.sagararicemill.activities

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.BillAdapter
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.PaymentHistory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.util.*

class OutstandingBillsActivity : AppCompatActivity() {

    private lateinit var listViewOutstandingBills: ListView
    private lateinit var billAdapter: BillAdapter
    private val outstandingBills = mutableListOf<Bill>()

    private val db = FirebaseFirestore.getInstance()

    private val TAG = "OutstandingBillsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outstanding_bills)

        // Initialize UI components
        listViewOutstandingBills = findViewById(R.id.listViewOutstandingBills)

        // Initialize Adapter
        billAdapter = BillAdapter(this, outstandingBills) { bill ->
            showMakePaymentDialog(bill)
        }
        listViewOutstandingBills.adapter = billAdapter

        // Fetch Outstanding Bills
        fetchOutstandingBills()
    }

    /**
     * Fetches bills with paymentStatus as "Unpaid" or "Partially Paid".
     */
    private fun fetchOutstandingBills() {
        db.collection("bills")
            .whereIn("paymentStatus", listOf("Unpaid", "Partially Paid"))
            .get()
            .addOnSuccessListener { documents ->
                outstandingBills.clear()
                for (document in documents) {
                    val bill = document.toObject(Bill::class.java)
                    bill.id = document.id
                    outstandingBills.add(bill)
                }
                billAdapter.notifyDataSetChanged()
                Log.d(TAG, "Fetched ${outstandingBills.size} outstanding bills.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching outstanding bills: ", exception)
                Toast.makeText(this, "Error fetching outstanding bills: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Shows a dialog to make a payment on the selected bill.
     */
    private fun showMakePaymentDialog(bill: Bill) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Make Payment for Bill ID: ${bill.id}")

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_make_payment, null)
        builder.setView(view)

        val editTextAmount = view.findViewById<EditText>(R.id.editTextPaymentAmount)
        val radioGroupPaymentMethod = view.findViewById<RadioGroup>(R.id.radioGroupPaymentMethod)
        val radioCash = view.findViewById<RadioButton>(R.id.radioCash)
        val radioCheque = view.findViewById<RadioButton>(R.id.radioCheque)
        val radioCredit = view.findViewById<RadioButton>(R.id.radioCredit)

        // Pre-select Cash as default
        radioCash.isChecked = true

        // Show/Hide additional payment details based on selection
        val layoutChequeDetails = view.findViewById<LinearLayout>(R.id.layoutChequeDetails)
        val layoutCreditDetails = view.findViewById<LinearLayout>(R.id.layoutCreditDetails)

        radioGroupPaymentMethod.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioCash -> {
                    layoutChequeDetails.visibility = LinearLayout.GONE
                    layoutCreditDetails.visibility = LinearLayout.GONE
                }
                R.id.radioCheque -> {
                    layoutChequeDetails.visibility = LinearLayout.VISIBLE
                    layoutCreditDetails.visibility = LinearLayout.GONE
                }
                R.id.radioCredit -> {
                    layoutChequeDetails.visibility = LinearLayout.GONE
                    layoutCreditDetails.visibility = LinearLayout.VISIBLE
                }
                else -> {
                    layoutChequeDetails.visibility = LinearLayout.GONE
                    layoutCreditDetails.visibility = LinearLayout.GONE
                }
            }
        }

        builder.setPositiveButton("Submit") { dialog, _ ->
            val paymentAmountStr = editTextAmount.text.toString().trim()
            val paymentAmount = paymentAmountStr.toDoubleOrNull()

            if (paymentAmount == null || paymentAmount <= 0) {
                Toast.makeText(this, "Enter a valid payment amount.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (paymentAmount > (bill.amount - bill.paidAmount)) {
                Toast.makeText(this, "Payment exceeds outstanding amount.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Get selected payment method
            val selectedPaymentMethodId = radioGroupPaymentMethod.checkedRadioButtonId
            val selectedPaymentMethod = when (selectedPaymentMethodId) {
                R.id.radioCash -> "Cash"
                R.id.radioCheque -> "Cheque"
                R.id.radioCredit -> "Credit"
                else -> "Cash" // Default
            }

            // Capture additional payment details
            val paymentDetails = when (selectedPaymentMethod) {
                "Cheque" -> {
                    val chequeNumber = view.findViewById<EditText>(R.id.editTextChequeNumber).text.toString().trim()
                    val bankName = view.findViewById<EditText>(R.id.editTextBankName).text.toString().trim()

                    if (chequeNumber.isEmpty()) {
                        Toast.makeText(this, "Enter cheque number.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    if (bankName.isEmpty()) {
                        Toast.makeText(this, "Enter bank name.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    com.example.sagararicemill.models.PaymentDetails(
                        chequeNumber = chequeNumber,
                        bankName = bankName
                    )
                }
                "Credit" -> {
                    val creditTermStr = view.findViewById<EditText>(R.id.editTextCreditTerm).text.toString().trim()
                    val creditTermDays = creditTermStr.toIntOrNull()

                    if (creditTermDays == null || creditTermDays <= 0) {
                        Toast.makeText(this, "Enter valid credit term days.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Calculate new due date
                    val dueDate = Calendar.getInstance()
                    dueDate.add(Calendar.DAY_OF_YEAR, creditTermDays)

                    com.example.sagararicemill.models.PaymentDetails(
                        creditTermDays = creditTermDays
                    )
                }
                else -> null // Cash does not require additional details
            }

            // Process the payment
            processPayment(bill, paymentAmount, selectedPaymentMethod, paymentDetails)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    /**
     * Processes the payment by updating the bill in Firestore.
     */
    private fun processPayment(
        bill: Bill,
        paymentAmount: Double,
        paymentMethod: String,
        paymentDetails: com.example.sagararicemill.models.PaymentDetails?
    ) {
        // Create a new payment history entry
        val paymentHistory = PaymentHistory(
            paymentDate = Timestamp(Date()),
            amountPaid = paymentAmount,
            paymentMethod = paymentMethod
        )

        // Calculate new paidAmount
        val newPaidAmount = bill.paidAmount + paymentAmount

        // Determine new paymentStatus
        val newPaymentStatus = when {
            newPaidAmount >= bill.amount -> "Paid"
            newPaidAmount > 0 -> "Partially Paid"
            else -> "Unpaid"
        }

        // Prepare the updated bill data
        val updatedBill = hashMapOf<String, Any>(
            "paidAmount" to newPaidAmount,
            "paymentStatus" to newPaymentStatus,
            "paymentHistory" to FieldValue.arrayUnion(paymentHistory.toMap())
        )

        // If payment method is Credit or Cheque, handle dueDate and paymentDetails
        if (paymentMethod == "Credit" && paymentDetails?.creditTermDays != null) {
            val dueDate = Calendar.getInstance()
            dueDate.add(Calendar.DAY_OF_YEAR, paymentDetails.creditTermDays!!)
            updatedBill["dueDate"] = Timestamp(dueDate.time)
            updatedBill["paymentDetails"] = paymentDetails.toMap()
        }

        if (paymentMethod == "Cheque") {
            updatedBill["paymentDetails"] = paymentDetails?.toMap() ?: HashMap<String, Any>()
        }

        // Update the bill in Firestore
        db.collection("bills").document(bill.id)
            .update(updatedBill)
            .addOnSuccessListener {
                Toast.makeText(this, "Payment successful.", Toast.LENGTH_SHORT).show()
                fetchOutstandingBills()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error processing payment: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error processing payment for Bill ID: ${bill.id}", e)
            }
    }
}
