package com.example.sagararicemill.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.MenuItem

class HomeAdapter(private val items: List<MenuItem>) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    inner class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewIcon)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu_card, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = items[position]
        holder.textViewTitle.text = item.title
        holder.imageViewIcon.setImageResource(item.iconRes)

        val color = when (item.title) {
            "Manage Shops" -> Color.parseColor("#007BFF") // Blue
            "Stock Management" -> Color.parseColor("#FFA500") // Orange
            "Distribute Rice" -> Color.parseColor("#28A745") // Green
            "Billing" -> Color.parseColor("#20C997") // Teal
            "Reports" -> Color.parseColor("#6F42C1") // Purple
            "Fleet Management" -> Color.parseColor("#DC3545") // Red
            else -> Color.parseColor("#007BFF") // Default color
        }

        val drawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.circle_background)?.mutate()
        drawable?.setTint(color)
        holder.imageViewIcon.background = drawable

        holder.itemView.setOnClickListener {
            item.action.invoke()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
