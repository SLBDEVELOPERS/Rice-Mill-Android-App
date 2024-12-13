package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.PaymentHistory

class BillAdapter(
    context: Context,
    private val bills: List<Bill>,
    private val onMakePaymentClick: (Bill) -> Unit
) : ArrayAdapter<Bill>(context, 0, bills) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val bill = bills[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_outstanding_bill, parent, false)

        val textViewBillId = view.findViewById<TextView>(R.id.textViewBillId)
        val textViewAmount = view.findViewById<TextView>(R.id.textViewAmount)
        val textViewDueAmount = view.findViewById<TextView>(R.id.textViewDueAmount)
        val textViewPaymentMethod = view.findViewById<TextView>(R.id.textViewPaymentMethod)
        val textViewPaymentStatus = view.findViewById<TextView>(R.id.textViewPaymentStatus)
        val textViewDueDate = view.findViewById<TextView>(R.id.textViewDueDate)
        val buttonMakePayment = view.findViewById<Button>(R.id.buttonMakePayment)
        val recyclerViewPaymentHistory = view.findViewById<RecyclerView>(R.id.recyclerViewPaymentHistory)
        val buttonToggleHistory = view.findViewById<Button>(R.id.buttonToggleHistory)

        textViewBillId.text = "Bill ID: ${bill.id}"
        textViewAmount.text = "Amount: Rs ${String.format("%.2f", bill.amount)}"
        textViewDueAmount.text = "Due Amount: Rs ${String.format("%.2f", bill.amount - bill.paidAmount)}"
        textViewPaymentMethod.text = "Payment Method: ${bill.paymentMethod}"
        textViewPaymentStatus.text = "Status: ${bill.paymentStatus}"

        if (bill.dueDate != null) {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            textViewDueDate.text = "Due Date: ${dateFormat.format(bill.dueDate!!.toDate())}"
            textViewDueDate.visibility = View.VISIBLE
        } else {
            textViewDueDate.visibility = View.GONE
        }

        // Setup Payment History RecyclerView
        val paymentHistoryAdapter = PaymentHistoryAdapter(context, bill.paymentHistory)
        recyclerViewPaymentHistory.layoutManager = LinearLayoutManager(context)
        recyclerViewPaymentHistory.adapter = paymentHistoryAdapter
        recyclerViewPaymentHistory.visibility = View.GONE

        // Toggle Payment History Visibility
        buttonToggleHistory.setOnClickListener {
            if (recyclerViewPaymentHistory.visibility == View.GONE) {
                recyclerViewPaymentHistory.visibility = View.VISIBLE
                buttonToggleHistory.text = "Hide Payment History"
            } else {
                recyclerViewPaymentHistory.visibility = View.GONE
                buttonToggleHistory.text = "Show Payment History"
            }
        }

        buttonMakePayment.setOnClickListener {
            onMakePaymentClick(bill)
        }

        return view
    }
}
