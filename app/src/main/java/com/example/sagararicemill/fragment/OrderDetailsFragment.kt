package com.example.sagararicemill.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Order
import com.google.firebase.firestore.FirebaseFirestore

class OrderDetailsFragment : Fragment() {

    private lateinit var textViewOrderId: TextView
    private lateinit var textViewShopName: TextView
    private lateinit var textViewRiceBagSize: TextView
    private lateinit var textViewQuantity: TextView
    private lateinit var textViewPrice: TextView
    private lateinit var textViewTotalPrice: TextView
    private lateinit var textViewDeliveryStatus: TextView
    private lateinit var textViewOrderDate: TextView

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "OrderDetailsFragment"

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_order_details, container, false)

        // Initialize Views
        textViewOrderId = view.findViewById(R.id.textViewOrderId)
        textViewShopName = view.findViewById(R.id.textViewShopName)
        textViewRiceBagSize = view.findViewById(R.id.textViewRiceBagSize)
        textViewQuantity = view.findViewById(R.id.textViewQuantity)
        textViewPrice = view.findViewById(R.id.textViewPrice)
        textViewTotalPrice = view.findViewById(R.id.textViewTotalPrice)
        textViewDeliveryStatus = view.findViewById(R.id.textViewDeliveryStatus)
        textViewOrderDate = view.findViewById(R.id.textViewOrderDate)

        // Get Order ID from Arguments
        val orderId = arguments?.getString("orderId")
        if (orderId != null) {
            fetchOrderDetails(orderId)
        } else {
            Toast.makeText(requireContext(), "Order ID not provided.", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    /**
     * Fetches order details from Firestore.
     */
    private fun fetchOrderDetails(orderId: String) {
        db.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val order = document.toObject(Order::class.java)
                    if (order != null) {
                        populateOrderDetails(order)
                        fetchShopName(order.shopId)
                    }
                } else {
                    Toast.makeText(requireContext(), "Order not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching order: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fetches the shop name based on shopId.
     */
    private fun fetchShopName(shopId: String) {
        db.collection("shops").document(shopId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val shopName = document.getString("name") ?: "Unknown"
                    textViewShopName.text = "Shop: $shopName"
                } else {
                    textViewShopName.text = "Shop: Unknown"
                }
            }
            .addOnFailureListener { exception ->
                textViewShopName.text = "Shop: Unknown"
                Toast.makeText(requireContext(), "Error fetching shop name: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Populates the order details in the UI.
     */
    private fun populateOrderDetails(order: Order) {
        textViewOrderId.text = "Order ID: ${order.id}"
        textViewRiceBagSize.text = "Rice Bag Size: ${order.size}"
        textViewQuantity.text = "Quantity: ${order.quantity}"
        textViewPrice.text = "Price per Unit: Rs ${String.format("%.2f", order.price)}"
        textViewTotalPrice.text = "Total Price: Rs ${String.format("%.2f", order.totalPrice)}"
        textViewDeliveryStatus.text = "Delivery Status: ${order.deliveryStatus}"
        textViewOrderDate.text = "Order Date: ${order.orderDate?.toDate()?.toString()}"
    }

    companion object {
        /**
         * Creates a new instance of OrderDetailsFragment with the provided order ID.
         */
        fun newInstance(orderId: String): OrderDetailsFragment {
            val fragment = OrderDetailsFragment()
            val args = Bundle()
            args.putString("orderId", orderId)
            fragment.arguments = args
            return fragment
        }
    }
}
