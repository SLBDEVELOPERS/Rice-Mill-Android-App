package com.example.sagararicemill.models

import com.google.firebase.Timestamp

data class Bill(
    var id: String = "",
    var orderIds: List<String> = listOf(),
    var amount: Double = 0.0,
    var billDate: Timestamp? = null,
    var paymentMethod: String = "",      // Existing field
    var paymentStatus: String = "Unpaid",// Existing field: Paid, Unpaid, Partially Paid
    var chequeStatus: String = "Pending",
    var dueDate: Timestamp? = null,      // Existing field: applicable for Credit and Cheque
    var paymentDetails: PaymentDetails? = null, // Existing field: additional payment info
    var paidAmount: Double = 0.0,        // New field: Total amount paid
    var paymentHistory: List<PaymentHistory> = listOf() // New field: History of payments
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf(
            "orderIds" to orderIds,
            "amount" to amount,
            "billDate" to billDate!!,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to paymentStatus,
            "chequeStatus" to chequeStatus,
            "paidAmount" to paidAmount,
            "paymentHistory" to paymentHistory.map { it.toMap() }
        )

        dueDate?.let {
            map["dueDate"] = it
        }

        paymentDetails?.let {
            map["paymentDetails"] = it.toMap()
        }

        return map
    }
}

data class PaymentDetails(
    var chequeNumber: String? = null,
    var bankName: String? = null,
    var creditTermDays: Int? = null // Applicable for Credit payments
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        chequeNumber?.let { map["chequeNumber"] = it }
        bankName?.let { map["bankName"] = it }
        creditTermDays?.let { map["creditTermDays"] = it }
        return map
    }
}

data class PaymentHistory(
    var paymentDate: Timestamp? = null,
    var amountPaid: Double = 0.0,
    var paymentMethod: String = "" // e.g., Cash, Cheque, Credit
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "paymentDate" to paymentDate!!,
            "amountPaid" to amountPaid,
            "paymentMethod" to paymentMethod
        )
    }
}



