package com.example.sagararicemill.models

import com.google.firebase.Timestamp

data class Order(
    var id: String = "",
    var shopId: String = "",
    var riceBagId: String = "",
    var size: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0,
    var totalPrice: Double = 0.0,
    var deliveryStatus: String = "Pending",  // Specific to each order
    var orderDate: Timestamp? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "shopId" to shopId,
            "riceBagId" to riceBagId,
            "size" to size,
            "price" to price,
            "quantity" to quantity,
            "totalPrice" to totalPrice,
            "deliveryStatus" to deliveryStatus,
            "orderDate" to orderDate!!
        )
    }
}

