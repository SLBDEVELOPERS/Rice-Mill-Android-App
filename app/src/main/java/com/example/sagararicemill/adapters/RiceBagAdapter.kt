package com.example.sagararicemill.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.sagararicemill.models.RiceBag
import com.example.sagararicemill.R

class RiceBagAdapter(private val context: Activity, private val riceBagList: List<RiceBag>) :
    ArrayAdapter<RiceBag>(context, R.layout.list_item_rice_bag, riceBagList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item_rice_bag, null, true)

        val textViewName: TextView = rowView.findViewById(R.id.textViewName)
        val textViewSize: TextView = rowView.findViewById(R.id.textViewSize)
        val textViewPrice: TextView = rowView.findViewById(R.id.textViewPrice)
        val textViewStock: TextView = rowView.findViewById(R.id.textViewStock)

        val riceBag = riceBagList[position]

        textViewName.text = "Name: ${riceBag.name}"
        textViewSize.text = "Size: ${riceBag.size}"
        textViewPrice.text = "Price: \$${riceBag.price}"
        textViewStock.text = "Stock: ${riceBag.stock}"

        return rowView
    }
}
