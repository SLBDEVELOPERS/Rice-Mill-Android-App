package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.fragment.TopShopInfo

class TopShopsAdapter(
    private val context: Context,
    private val topShops: List<TopShopInfo>
) : RecyclerView.Adapter<TopShopsAdapter.TopShopsViewHolder>() {

    class TopShopsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewShopName: TextView = itemView.findViewById(R.id.textViewShopName)
        val textViewShopSales: TextView = itemView.findViewById(R.id.textViewShopSales)
        val textViewShopAddress: TextView = itemView.findViewById(R.id.textViewShopAddress)
        val textViewShopContact: TextView = itemView.findViewById(R.id.textViewShopContact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopShopsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_top_shop, parent, false)
        return TopShopsViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopShopsViewHolder, position: Int) {
        val shopInfo = topShops[position]
        holder.textViewShopName.text = shopInfo.name
        holder.textViewShopSales.text = "Rs ${String.format("%.2f", shopInfo.totalSales)}"
        holder.textViewShopAddress.text = shopInfo.address ?: "No Address"
        holder.textViewShopContact.text = shopInfo.contact ?: "No Contact"
    }

    override fun getItemCount(): Int = topShops.size
}
