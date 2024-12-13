package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Shop
import java.util.*
import kotlin.collections.ArrayList

class ShopAdapter(
    private val context: Context,
    private var shopList: List<Shop>,
    private val onItemClick: (Shop) -> Unit,
    private val onItemLongClick: (Shop) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>(), Filterable {

    private var shopListFull: List<Shop> = ArrayList(shopList)

    inner class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewShopName)
        val textViewAddress: TextView = itemView.findViewById(R.id.textViewShopAddress)
        val textViewContact: TextView = itemView.findViewById(R.id.textViewShopContact)
        val textViewDue: TextView = itemView.findViewById(R.id.textViewShopDue)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(shopList[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(shopList[position])
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val currentShop = shopList[position]
        holder.textViewName.text = currentShop.name
        holder.textViewAddress.text = "Address: ${currentShop.address}"
        holder.textViewContact.text = "Contact: ${currentShop.contact}"
        holder.textViewDue.text = "Outstanding Due: Rs ${String.format("%.2f", currentShop.outstandingDue)}"
    }

    override fun getItemCount(): Int {
        return shopList.size
    }

    // Implement Filterable for search functionality
    override fun getFilter(): Filter {
        return shopFilter
    }

    private val shopFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = if (constraint.isNullOrEmpty()) {
                shopListFull
            } else {
                val filterPattern = constraint.toString().trim().toLowerCase(Locale.getDefault())
                shopListFull.filter {
                    it.name.toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                            it.address.toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                            it.contact.contains(filterPattern)
                }
            }

            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            shopList = results?.values as List<Shop>
            notifyDataSetChanged()
        }
    }

    // Update the full list when data changes
    fun updateList(newList: List<Shop>) {
        shopListFull = newList
        shopList = ArrayList(newList)
        notifyDataSetChanged()
    }
}
