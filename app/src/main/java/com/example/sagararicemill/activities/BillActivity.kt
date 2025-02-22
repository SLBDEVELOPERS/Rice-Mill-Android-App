package com.example.sagararicemill.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.OrdersAdapter
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.Shop
import com.example.sagararicemill.utils.PrinterHelper
import com.google.firebase.firestore.FieldPath
import java.text.SimpleDateFormat
import java.util.Locale

class BillActivity : AppCompatActivity() {
    private val TAG = "BillActivity"
    private lateinit var printerHelper: PrinterHelper
    private val db = FirebaseFirestore.getInstance()
    private var billId: String? = null

    // View references
    private lateinit var textViewBillId: TextView
    private lateinit var textViewBillDate: TextView
    private lateinit var textViewShopName: TextView
    private lateinit var textViewPaymentMethod: TextView
    private lateinit var recyclerViewOrders: RecyclerView
    private lateinit var textViewTotalAmount: TextView
    private lateinit var buttonPrintBill: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill)

        printerHelper = PrinterHelper(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Bill Details"

        // Initialize views
        textViewBillId = findViewById(R.id.textViewBillId)
        textViewBillDate = findViewById(R.id.textViewBillDate)
        textViewShopName = findViewById(R.id.textViewShopName)
        textViewPaymentMethod = findViewById(R.id.textViewPaymentMethod)
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders)
        textViewTotalAmount = findViewById(R.id.textViewTotalAmount)
        buttonPrintBill = findViewById(R.id.buttonPrintBill)

        billId = intent.getStringExtra("billId")
        if (billId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid Bill ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerViewOrders.layoutManager = LinearLayoutManager(this)
        fetchBillAndOrders(billId!!)
    }

    private fun fetchBillAndOrders(billId: String) {
        db.collection("bills").document(billId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bill = document.toObject(Bill::class.java)
                    bill?.id = document.id
                    bill?.let {
                        if (it.orderIds.isNullOrEmpty()) {
                            Toast.makeText(this, "No orders associated with this bill", Toast.LENGTH_SHORT).show()
                            finish()
                            return@addOnSuccessListener
                        }
                        fetchOrdersForBill(it)
                    }
                } else {
                    Toast.makeText(this, "Bill not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching bill: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


    private fun fetchOrdersForBill(bill: Bill) {
        db.collection("orders")
            .whereIn(FieldPath.documentId(), bill.orderIds)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val orders = querySnapshot.documents.mapNotNull { doc ->
                    val order = doc.toObject(Order::class.java)
                    order?.id = doc.id
                    order
                }

                if (orders.isEmpty()) {
                    Toast.makeText(this, "No orders found for this bill", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Assuming all orders from the same shop
                val shopId = orders.first().shopId
                fetchShopAndBindData(shopId, orders, bill)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching orders: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun fetchShopAndBindData(shopId: String, orders: List<Order>, bill: Bill) {
        db.collection("shops").document(shopId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val shop = document.toObject(Shop::class.java)
                    if (shop != null) {
                        bindDataToViews(shop, orders, bill)
                    } else {
                        Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching shop: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


    private fun fetchAndPrintBill(billId: String) {
        db.collection("bills").document(billId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val bill = document.toObject(Bill::class.java)
                    bill?.id = document.id
                    bill?.let {
                        fetchOrderAndShop(it)
                    }
                } else {
                    Toast.makeText(this, "Bill not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching bill: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun fetchOrderAndShop(bill: Bill) {
        db.collection("orders").whereIn(FieldPath.documentId(),bill.orderIds)
            .get()
            .addOnSuccessListener { querySnapshot ->

                val orders = querySnapshot.documents.mapNotNull { doc ->
                    val order = doc.toObject(Order::class.java)
                    order?.id = doc.id
                    order
                }

                if (orders.isEmpty()) {
                    Toast.makeText(this, "No orders found for this bill", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Assuming all orders are from the same shop
                val shopId = orders.first().shopId
                fetchShopAndPrint(shopId, orders, bill)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching order: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun fetchShopAndPrint(shopId: String, orders: List<Order>, bill: Bill) {
        db.collection("shops").document(shopId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val shop = document.toObject(Shop::class.java)
                    shop?.let {
                        printerHelper.printBill(it, orders, bill)
                        Toast.makeText(this, "Bill sent to printer", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching shop: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


    private fun bindDataToViews(shop: Shop, orders: List<Order>, bill: Bill) {
        textViewBillId.text = "Bill ID: ${bill.id}"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val billDateStr = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
        textViewBillDate.text = "Date: $billDateStr"

        textViewShopName.text = "Shop: ${shop.name}"
        textViewPaymentMethod.text = "Payment Method: ${bill.paymentMethod}"

        // Set up the orders list
        val adapter = OrdersAdapter(orders)
        recyclerViewOrders.adapter = adapter

        val totalAmount = orders.sumOf { it.totalPrice }
        textViewTotalAmount.text = "Total: Rs %.2f".format(totalAmount)

        buttonPrintBill.setOnClickListener {
            printerHelper.printBill(shop, orders, bill)
            Toast.makeText(this, "Bill sent to printer", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        // Handle the back arrow button press
        finish()
        return true
    }

}
