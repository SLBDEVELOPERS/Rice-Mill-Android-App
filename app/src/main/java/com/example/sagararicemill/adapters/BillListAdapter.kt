package com.example.sagararicemill.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Bill
import java.text.SimpleDateFormat
import java.util.Locale

class BillListAdapter(private val bills: List<Bill>, private val onBillClick: (Bill) -> Unit)
    : RecyclerView.Adapter<BillListAdapter.BillViewHolder>() {

    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewBillId: TextView = itemView.findViewById(R.id.textViewBillId)
        val textViewBillDate: TextView = itemView.findViewById(R.id.textViewBillDate)
        val textViewBillAmount: TextView = itemView.findViewById(R.id.textViewBillAmount)
        val textViewPaymentMethod: TextView = itemView.findViewById(R.id.textViewPaymentMethod)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bill, parent, false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val bill = bills[position]
        holder.textViewBillId.text = "Bill ID: ${bill.id}"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
        holder.textViewBillDate.text = "Date: $dateStr"
        holder.textViewBillAmount.text = "Amount: Rs ${"%.2f".format(bill.amount)}"
        holder.textViewPaymentMethod.text = "Payment: ${bill.paymentMethod}"

        holder.itemView.setOnClickListener {
            onBillClick(bill)
        }
    }

    override fun getItemCount() = bills.size
}
