package com.example.sagararicemill.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.RiceBag

class LoadedRiceBagAdapter(
    private val context: Context,
    private var loadedRiceBagList: MutableList<RiceBag>
) : RecyclerView.Adapter<LoadedRiceBagAdapter.LoadedRiceBagViewHolder>() {

    private  val TAG = "LoadedRiceBagAdapter"
    
    inner class LoadedRiceBagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewNameSize: TextView = itemView.findViewById(R.id.textViewLoadedNameSize)
        val textViewQuantity: TextView = itemView.findViewById(R.id.textViewLoadedQuantity)
        val textViewPrice: TextView = itemView.findViewById(R.id.textViewLoadedPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadedRiceBagViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_loaded_rice_bag, parent, false)
        return LoadedRiceBagViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoadedRiceBagViewHolder, position: Int) {
        val riceBag = loadedRiceBagList[position]
        holder.textViewNameSize.text = "Name: ${riceBag.name} - ${riceBag.size}"
        holder.textViewQuantity.text = "Quantity: ${riceBag.stock}"
        holder.textViewPrice.text = "Price per Unit: \$${String.format("%.2f", riceBag.price)}"
    }

    override fun getItemCount(): Int {
        return loadedRiceBagList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<RiceBag>) {
        val newListCopy = ArrayList(newList) // Create a copy of the list
        Log.d(TAG, "Updating adapter with ${newListCopy.size} items")
        Log.d(TAG, "newListCopy before clear: $newListCopy")

        loadedRiceBagList.clear()
        Log.d(TAG, "loadedRiceBagList after clear: $loadedRiceBagList")

        loadedRiceBagList.addAll(newListCopy)
        Log.d(TAG, "loadedRiceBagList after addAll: $loadedRiceBagList")

        notifyDataSetChanged()
        Log.d(TAG, "Adapter list updated with ${newListCopy.size} items")
    }

    fun removeItem(position: Int) {
        loadedRiceBagList.removeAt(position)
        notifyItemRemoved(position)
    }
}
