package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.CartItem


class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChange: (position: Int, newQuantity: Int) -> Unit,
    private val onRemoveItem: (position: Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // ViewHolder Class
    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewSize: TextView = view.findViewById(R.id.textViewCartSize)
        val textViewPrice: TextView = view.findViewById(R.id.textViewCartPrice)
        val editTextQuantity: EditText = view.findViewById(R.id.editTextCartQuantity)
        val textViewTotalPrice: TextView = view.findViewById(R.id.textViewCartTotalPrice)
        val buttonRemove: ImageButton = view.findViewById(R.id.buttonRemoveCartItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItems[position]

        holder.textViewSize.text = "Size: ${cartItem.size}"
        holder.textViewPrice.text = "Price: \$${cartItem.price}"
        holder.editTextQuantity.setText(cartItem.quantity.toString())
        holder.textViewTotalPrice.text = "Total: \$${String.format("%.2f", cartItem.getTotalPrice())}"

        // Handle Quantity Changes
        holder.editTextQuantity.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newQuantityStr = holder.editTextQuantity.text.toString().trim()
                val newQuantity = newQuantityStr.toIntOrNull() ?: cartItem.quantity
                if (newQuantity != cartItem.quantity && newQuantity > 0) {
                    onQuantityChange(position, newQuantity)
                } else if (newQuantity <= 0) {
                    Toast.makeText(context, "Quantity must be at least 1", Toast.LENGTH_SHORT).show()
                    holder.editTextQuantity.setText(cartItem.quantity.toString())
                }
            }
        }

        // Handle Remove Item
        holder.buttonRemove.setOnClickListener {
            onRemoveItem(position)
        }
    }

    override fun getItemCount(): Int = cartItems.size
}

