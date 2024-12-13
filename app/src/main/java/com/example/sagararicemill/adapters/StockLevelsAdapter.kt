package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.RiceBag

class StockLevelsAdapter(
    private val context: Context,
    private val riceBags: List<RiceBag>
) : RecyclerView.Adapter<StockLevelsAdapter.StockLevelsViewHolder>() {

    inner class StockLevelsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewRiceBagSize: TextView = itemView.findViewById(R.id.textViewRiceBagSize)
        val textViewStock: TextView = itemView.findViewById(R.id.textViewStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockLevelsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_stock_level, parent, false)
        return StockLevelsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockLevelsViewHolder, position: Int) {
        val riceBag = riceBags[position]
        holder.textViewRiceBagSize.text = "Size: ${riceBag.size}"
        holder.textViewStock.text = "Stock: ${riceBag.stock}"
    }

    override fun getItemCount(): Int {
        return riceBags.size
    }
}
