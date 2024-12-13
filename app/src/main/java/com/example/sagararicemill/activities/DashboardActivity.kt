package com.example.sagararicemill.activities

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.RecentOrdersAdapter
import com.example.sagararicemill.adapters.StockLevelsAdapter
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.RiceBag
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var textViewTotalSales: TextView
    private lateinit var textViewOutstandingPayments: TextView
    private lateinit var recyclerViewRecentOrders: RecyclerView
    private lateinit var recyclerViewStockLevels: RecyclerView

    private val db = FirebaseFirestore.getInstance()

    private val TAG = "DashboardActivity"

    private val recentOrders = mutableListOf<Order>()
    private val stockLevels = mutableListOf<RiceBag>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize UI components
        textViewTotalSales = findViewById(R.id.textViewTotalSales)
        textViewOutstandingPayments = findViewById(R.id.textViewOutstandingPayments)
        recyclerViewRecentOrders = findViewById(R.id.recyclerViewRecentOrders)
        recyclerViewStockLevels = findViewById(R.id.recyclerViewStockLevels)

        // Setup RecyclerViews
        recyclerViewRecentOrders.layoutManager = LinearLayoutManager(this)
        recyclerViewStockLevels.layoutManager = LinearLayoutManager(this)

        val recentOrdersAdapter = RecentOrdersAdapter(this, recentOrders)
        recyclerViewRecentOrders.adapter = recentOrdersAdapter

        val stockLevelsAdapter = StockLevelsAdapter(this, stockLevels)
        recyclerViewStockLevels.adapter = stockLevelsAdapter

        // Fetch and Display Data
        fetchTotalSales()
        fetchOutstandingPayments()
        fetchRecentOrders()
        fetchStockLevels()
    }

    /**
     * Fetches and calculates the total sales from all issued bills.
     */
    private fun fetchTotalSales() {
        db.collection("bills")
            .whereEqualTo("paymentStatus", "Paid")
            .get()
            .addOnSuccessListener { documents ->
                var totalSales = 0.0
                for (document in documents) {
                    val bill = document.toObject(Bill::class.java)
                    totalSales += bill.amount
                }
                textViewTotalSales.text = "Rs ${String.format("%.2f", totalSales)}"
                Log.d(TAG, "Total Sales: Rs $totalSales")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching total sales: ", exception)
                Toast.makeText(this, "Error fetching total sales: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fetches and calculates the total outstanding payments.
     */
    private fun fetchOutstandingPayments() {
        db.collection("bills")
            .whereIn("paymentStatus", listOf("Unpaid", "Partially Paid"))
            .get()
            .addOnSuccessListener { documents ->
                var outstandingPayments = 0.0
                for (document in documents) {
                    val bill = document.toObject(Bill::class.java)
                    val remaining = bill.amount - bill.paidAmount
                    outstandingPayments += remaining
                }
                textViewOutstandingPayments.text = "Rs ${String.format("%.2f", outstandingPayments)}"
                Log.d(TAG, "Outstanding Payments: Rs $outstandingPayments")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching outstanding payments: ", exception)
                Toast.makeText(this, "Error fetching outstanding payments: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fetches the 5 most recent orders.
     */
    private fun fetchRecentOrders() {
        db.collection("orders")
            .orderBy("orderDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                recentOrders.clear()
                for (document in documents) {
                    val order = document.toObject(Order::class.java)
                    order.id = document.id
                    recentOrders.add(order)
                }
                recyclerViewRecentOrders.adapter?.notifyDataSetChanged()
                Log.d(TAG, "Fetched ${recentOrders.size} recent orders.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching recent orders: ", exception)
                Toast.makeText(this, "Error fetching recent orders: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fetches current stock levels of all rice bags.
     */
    private fun fetchStockLevels() {
        db.collection("rice_bags").get()
            .addOnSuccessListener { documents ->
                stockLevels.clear()
                for (document in documents) {
                    val riceBag = document.toObject(RiceBag::class.java)
                    riceBag.id = document.id
                    stockLevels.add(riceBag)
                }
                recyclerViewStockLevels.adapter?.notifyDataSetChanged()
                Log.d(TAG, "Fetched ${stockLevels.size} rice bags for stock levels.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching stock levels: ", exception)
                Toast.makeText(this, "Error fetching stock levels: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
