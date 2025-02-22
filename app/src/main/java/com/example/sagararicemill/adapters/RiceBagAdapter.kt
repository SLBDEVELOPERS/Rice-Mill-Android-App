package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.RiceBag
import java.util.*
import kotlin.collections.ArrayList

class RiceBagAdapter(
    private val context: Context,
    private var riceBagList: List<RiceBag>,
    private val onItemClick: (RiceBag) -> Unit,
    private val onItemLongClick: (RiceBag) -> Unit
) : RecyclerView.Adapter<RiceBagAdapter.RiceBagViewHolder>(), Filterable {

    private var riceBagListFull: List<RiceBag> = ArrayList(riceBagList)

    inner class RiceBagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewSize: TextView = itemView.findViewById(R.id.textViewSize)
        val textViewPrice: TextView = itemView.findViewById(R.id.textViewPrice)
        val textViewStock: TextView = itemView.findViewById(R.id.textViewStock)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(riceBagList[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(riceBagList[position])
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiceBagViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_rice_bag, parent, false)
        return RiceBagViewHolder(view)
    }

    override fun onBindViewHolder(holder: RiceBagViewHolder, position: Int) {
        val currentBag = riceBagList[position]
        holder.textViewName.text = "Name: ${currentBag.name}"
        holder.textViewSize.text = "Size: ${currentBag.size}"
        holder.textViewPrice.text = "Price: \$${String.format("%.2f", currentBag.price)}"
        holder.textViewStock.text = "Stock: ${currentBag.stock}"

        // Show or hide low stock indicator
        if (currentBag.stock <= 5) {
            holder.itemView.findViewById<View>(R.id.viewLowStockIndicator).visibility = View.VISIBLE
        } else {
            holder.itemView.findViewById<View>(R.id.viewLowStockIndicator).visibility = View.GONE
        }

    }

    override fun getItemCount(): Int {
        return riceBagList.size
    }

    // Implement Filterable for search functionality
    override fun getFilter(): Filter {
        return riceBagFilter
    }

    private val riceBagFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = if (constraint.isNullOrEmpty()) {
                riceBagListFull
            } else {
                val filterPattern = constraint.toString().trim().toLowerCase(Locale.getDefault())
                riceBagListFull.filter {
                    it.name.toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                            it.size.toLowerCase(Locale.getDefault()).contains(filterPattern)
                }
            }

            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            riceBagList = results?.values as List<RiceBag>
            notifyDataSetChanged()
        }
    }

    // Update the list and notify adapter
    fun updateList(newList: List<RiceBag>) {
        riceBagList = newList
        riceBagListFull = ArrayList(newList)
        notifyDataSetChanged()
    }
}
