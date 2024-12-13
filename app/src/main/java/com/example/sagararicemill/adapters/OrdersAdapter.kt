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
        val textViewOrderItem: TextView = itemView.findViewById(R.id.textViewOrderItem)
        val textViewOrderQuantity: TextView = itemView.findViewById(R.id.textViewOrderQuantity)
        val textViewOrderPrice: TextView = itemView.findViewById(R.id.textViewOrderPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int = orders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.textViewOrderItem.text = order.size
        holder.textViewOrderQuantity.text = order.quantity.toString()
        holder.textViewOrderPrice.text = "Rs %.2f".format(order.totalPrice)
    }
}
