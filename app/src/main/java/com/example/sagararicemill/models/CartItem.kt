package com.example.sagararicemill.models

data class CartItem(
    var riceBagId: String = "",
    var size: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0,
    var name: String = "",
) {
    fun getTotalPrice(): Double {
        return price * quantity
    }
}
