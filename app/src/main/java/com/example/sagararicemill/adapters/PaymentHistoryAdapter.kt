package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.PaymentHistory
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryAdapter(
    private val context: Context,
    private val paymentHistories: List<PaymentHistory>
) : RecyclerView.Adapter<PaymentHistoryAdapter.PaymentHistoryViewHolder>() {

    inner class PaymentHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewPaymentDate: TextView = itemView.findViewById(R.id.textViewPaymentDate)
        val textViewAmountPaid: TextView = itemView.findViewById(R.id.textViewAmountPaid)
        val textViewPaymentMethod: TextView = itemView.findViewById(R.id.textViewPaymentMethod)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentHistoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_payment_history, parent, false)
        return PaymentHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentHistoryViewHolder, position: Int) {
        val payment = paymentHistories[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val paymentDate = payment.paymentDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
        holder.textViewPaymentDate.text = "Date: $paymentDate"
        holder.textViewAmountPaid.text = "Amount Paid: Rs ${String.format("%.2f", payment.amountPaid)}"
        holder.textViewPaymentMethod.text = "Method: ${payment.paymentMethod}"
    }

    override fun getItemCount(): Int {
        return paymentHistories.size
    }
}
