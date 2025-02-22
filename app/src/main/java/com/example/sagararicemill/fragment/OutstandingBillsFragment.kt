package com.example.sagararicemill.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.BillAdapter
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.PaymentDetails
import com.example.sagararicemill.models.PaymentHistory
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class OutstandingBillsFragment : Fragment() {

    private lateinit var listViewOutstandingBills: ListView
    private lateinit var billAdapter: BillAdapter
    private val outstandingBills = mutableListOf<Bill>()

    private val db = FirebaseFirestore.getInstance()

    private val TAG = "OutstandingBillsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_outstanding_bills, container, false)

        // Initialize UI components
        listViewOutstandingBills = view.findViewById(R.id.listViewOutstandingBills)

        // Initialize Adapter
        billAdapter = BillAdapter(requireContext(), outstandingBills) { bill ->
            showMakePaymentDialog(bill)
        }
        listViewOutstandingBills.adapter = billAdapter

        // Fetch Outstanding Bills
        fetchOutstandingBills()

        return view
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
                Toast.makeText(requireContext(), "Error fetching outstanding bills: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Shows a dialog to make a payment on the selected bill.
     */
    private fun showMakePaymentDialog(bill: Bill) {
        val builder = AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
        builder.setTitle("Make Payment for Bill ID: ${bill.id}")

        val inflater = LayoutInflater.from(requireContext())
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
                    layoutChequeDetails.visibility = View.GONE
                    layoutCreditDetails.visibility = View.GONE
                }
                R.id.radioCheque -> {
                    layoutChequeDetails.visibility = View.VISIBLE
                    layoutCreditDetails.visibility = View.GONE
                }
                R.id.radioCredit -> {
                    layoutChequeDetails.visibility = View.GONE
                    layoutCreditDetails.visibility = View.VISIBLE
                }
                else -> {
                    layoutChequeDetails.visibility = View.GONE
                    layoutCreditDetails.visibility = View.GONE
                }
            }
        }

        builder.setPositiveButton("Submit") { dialog, _ ->
            val paymentAmountStr = editTextAmount.text.toString().trim()
            val paymentAmount = paymentAmountStr.toDoubleOrNull()

            if (paymentAmount == null || paymentAmount <= 0) {
                Toast.makeText(requireContext(), "Enter a valid payment amount.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (paymentAmount > (bill.amount - bill.paidAmount)) {
                Toast.makeText(requireContext(), "Payment exceeds outstanding amount.", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(requireContext(), "Enter cheque number.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    if (bankName.isEmpty()) {
                        Toast.makeText(requireContext(), "Enter bank name.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    PaymentDetails(
                        chequeNumber = chequeNumber,
                        bankName = bankName
                    )
                }
                "Credit" -> {
                    val creditTermStr = view.findViewById<EditText>(R.id.editTextCreditTerm).text.toString().trim()
                    val creditTermDays = creditTermStr.toIntOrNull()

                    if (creditTermDays == null || creditTermDays <= 0) {
                        Toast.makeText(requireContext(), "Enter valid credit term days.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Calculate new due date
                    val dueDate = Calendar.getInstance()
                    dueDate.add(Calendar.DAY_OF_YEAR, creditTermDays)

                    PaymentDetails(
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
        paymentDetails: PaymentDetails?
    ) {
        // Create a new payment history entry
        val paymentHistory = PaymentHistory(
            paymentDate = com.google.firebase.Timestamp(Date()),
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
            updatedBill["dueDate"] = com.google.firebase.Timestamp(dueDate.time)
            updatedBill["paymentDetails"] = paymentDetails.toMap()
        }

        if (paymentMethod == "Cheque") {
            updatedBill["paymentDetails"] = paymentDetails?.toMap() ?: HashMap<String, Any>()
        }

        // Update the bill in Firestore
        db.collection("bills").document(bill.id)
            .update(updatedBill)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Payment successful.", Toast.LENGTH_SHORT).show()
                fetchOutstandingBills()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error processing payment: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error processing payment for Bill ID: ${bill.id}", e)
            }
    }


    private fun markChequeAsReturned(bill: Bill) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Mark Cheque as Returned")
        builder.setMessage("Are you sure you want to mark this cheque as returned?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            // Update the bill status and add to returned_cheques collection
            val updates = hashMapOf<String, Any>(
                "paymentStatus" to "Returned",
                "chequeStatus" to "Returned" // Add a new field to track cheque status
            )

            db.collection("bills").document(bill.id)
                .update(updates)
                .addOnSuccessListener {
                    // Add the returned cheque to the returned_cheques collection
                    val returnedCheque = hashMapOf(
                        "billId" to bill.id,
                        "shopId" to "bill.shopId",
                        "shopName" to "bill.shopName",
                        "amount" to bill.amount,
                        "chequeNumber" to bill.paymentDetails?.chequeNumber,
                        "bankName" to bill.paymentDetails?.bankName,
                        "returnDate" to com.google.firebase.Timestamp(Date()),
                        "reason" to "Insufficient Funds" // You can allow the user to input a reason
                    )

                    db.collection("returned_cheques")
                        .add(returnedCheque)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Cheque marked as returned.", Toast.LENGTH_SHORT).show()
                            fetchOutstandingBills() // Refresh the list
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error marking cheque as returned: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating bill status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

}
