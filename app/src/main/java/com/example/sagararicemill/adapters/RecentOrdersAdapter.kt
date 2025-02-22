package com.example.sagararicemill.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Order
import java.text.SimpleDateFormat
import java.util.*

class RecentOrdersAdapter(
    private val context: Context,
    private val orders: List<Order>
) : RecyclerView.Adapter<RecentOrdersAdapter.RecentOrdersViewHolder>() {

    inner class RecentOrdersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewOrderId: TextView = itemView.findViewById(R.id.textViewOrderId)
        val textViewShopId: TextView = itemView.findViewById(R.id.textViewShopId)
        val textViewAmount: TextView = itemView.findViewById(R.id.textViewAmount)
        val textViewOrderDate: TextView = itemView.findViewById(R.id.textViewOrderDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentOrdersViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_recent_order, parent, false)
        return RecentOrdersViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecentOrdersViewHolder, position: Int) {
        val order = orders[position]
        holder.textViewOrderId.text = "Order ID: ${order.id}"
        holder.textViewShopId.text = "Shop: ${order.shopName}"
        holder.textViewAmount.text = "Amount: Rs ${String.format("%.2f", order.totalPrice)}"
        holder.textViewOrderDate.text = "Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
            order.orderDate!!.toDate())}"
    }

    override fun getItemCount(): Int {
        return orders.size
    }
}
