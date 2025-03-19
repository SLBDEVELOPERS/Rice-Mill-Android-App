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
        val view = inflater.inflate(R.layout.fragment_outstanding_bills, container, false)

        listViewOutstandingBills = view.findViewById(R.id.listViewOutstandingBills)

        billAdapter = BillAdapter(requireContext(),outstandingBills, onMakePaymentClick = { bill -> showMakePaymentDialog(bill) },onMarkChequeReturned = { bill -> markChequeAsReturned(bill) })
        listViewOutstandingBills.adapter = billAdapter

        fetchOutstandingBills()

        return view
    }

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

        radioCash.isChecked = true

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

            val selectedPaymentMethodId = radioGroupPaymentMethod.checkedRadioButtonId
            val selectedPaymentMethod = when (selectedPaymentMethodId) {
                R.id.radioCash -> "Cash"
                R.id.radioCheque -> "Cheque"
                R.id.radioCredit -> "Credit"
                else -> "Cash"
            }

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

                    PaymentDetails(
                        creditTermDays = creditTermDays
                    )
                }
                else -> null
            }

            processPayment(bill, paymentAmount, selectedPaymentMethod, paymentDetails)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun processPayment(
        bill: Bill,
        paymentAmount: Double,
        paymentMethod: String,
        paymentDetails: PaymentDetails?
    ) {
        val paymentHistory = PaymentHistory(
            paymentDate = com.google.firebase.Timestamp(Date()),
            amountPaid = paymentAmount,
            paymentMethod = paymentMethod
        )

        val newPaidAmount = bill.paidAmount + paymentAmount

        val newPaymentStatus = when {
            newPaidAmount >= bill.amount -> "Paid"
            newPaidAmount > 0 -> "Partially Paid"
            else -> "Unpaid"
        }

        val updatedBill = hashMapOf<String, Any>(
            "paidAmount" to newPaidAmount,
            "paymentStatus" to newPaymentStatus,
            "paymentHistory" to FieldValue.arrayUnion(paymentHistory.toMap())
        )

        if (paymentMethod == "Credit" && paymentDetails?.creditTermDays != null) {
            val dueDate = Calendar.getInstance()
            dueDate.add(Calendar.DAY_OF_YEAR, paymentDetails.creditTermDays!!)
            updatedBill["dueDate"] = com.google.firebase.Timestamp(dueDate.time)
            updatedBill["paymentDetails"] = paymentDetails.toMap()
        }

        if (paymentMethod == "Cheque") {
            updatedBill["paymentDetails"] = paymentDetails?.toMap() ?: HashMap<String, Any>()
        }

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

    fun markChequeAsReturned(bill: Bill) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Mark Cheque as Returned")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Enter reason for return"
        builder.setView(input)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val reason = input.text.toString().trim()
            if (reason.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a reason.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val updates = hashMapOf<String, Any>(
                "paymentStatus" to "Returned",
                "chequeStatus" to "Returned"
            )

            db.collection("bills").document(bill.id)
                .update(updates)
                .addOnSuccessListener {
                    val returnedCheque = hashMapOf(
                        "billId" to bill.id,
//                        "shopId" to bill.shopId, // Assuming shopId exists in Bill
//                        "shopName" to bill.shopName, // Assuming shopName exists in Bill
                        "amount" to bill.amount,
                        "chequeNumber" to bill.paymentDetails?.chequeNumber,
                        "bankName" to bill.paymentDetails?.bankName,
                        "returnDate" to com.google.firebase.Timestamp(Date()),
                        "reason" to reason
                    )

                    db.collection("returned_cheques")
                        .add(returnedCheque)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Cheque marked as returned with reason: $reason", Toast.LENGTH_SHORT).show()
                            fetchOutstandingBills()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error marking cheque as returned: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating bill status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }
}