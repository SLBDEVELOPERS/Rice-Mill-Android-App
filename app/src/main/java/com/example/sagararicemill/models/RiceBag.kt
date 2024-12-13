package com.example.sagararicemill.models

data class RiceBag(
    var id: String = "",
    var name: String = "",
    var size: String = "", // e.g., 5kg, 10kg
    var price: Double = 0.0,
    var stock: Int = 0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "size" to size,
            "price" to price,
            "stock" to stock
        )
    }
}
