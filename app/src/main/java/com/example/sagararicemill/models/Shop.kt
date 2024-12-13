package com.example.sagararicemill.models

data class Shop(
    var id: String = "",
    var name: String = "",
    var address: String = "",
    var contact: String = "",
    var outstandingDue: Double = 0.0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "address" to address,
            "contact" to contact
        )
    }
}
