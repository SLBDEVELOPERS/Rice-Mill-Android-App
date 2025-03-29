package com.example.sagararicemill.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Order

class OrdersAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textViewRiceName: TextView = itemView.findViewById(R.id.textViewRiceName)
        val textViewSize: TextView = itemView.findViewById(R.id.textViewSize)
        val textViewQuantity: TextView = itemView.findViewById(R.id.textViewQuantity)
        val textViewPrice: TextView = itemView.findViewById(R.id.textViewPrice)
        val textViewTotalPrice: TextView = itemView.findViewById(R.id.textViewTotalPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int = orders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.textViewRiceName.text = order.riceName
        holder.textViewSize.text = order.size
        holder.textViewQuantity.text = order.quantity.toString()
        holder.textViewPrice.text = String.format("%.2f", order.price)
        holder.textViewTotalPrice.text = String.format("%.2f", order.totalPrice)
    }
}
