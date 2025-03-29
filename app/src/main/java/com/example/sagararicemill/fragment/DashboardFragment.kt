package com.example.sagararicemill.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.RecentOrdersAdapter
import com.example.sagararicemill.adapters.StockLevelsAdapter
import com.example.sagararicemill.adapters.TopShopsAdapter
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.RiceBag
import com.example.sagararicemill.models.Shop
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TopShopInfo(
    val name: String,
    val totalSales: Double,
    val address: String?,
    val contact: String?
)

class DashboardFragment : Fragment() {

    private lateinit var textViewTotalSales: TextView
    private lateinit var textViewOutstandingPayments: TextView
    private lateinit var textViewTotalOrders: TextView
    private lateinit var textViewAverageOrderValue: TextView

    private lateinit var recyclerViewRecentOrders: RecyclerView
    private lateinit var recyclerViewStockLevels: RecyclerView
    private lateinit var recyclerViewTopShops: RecyclerView

    private lateinit var lineChartSalesTrend: LineChart

    private lateinit var pieChart: PieChart

    private val db = FirebaseFirestore.getInstance()

    private val TAG = "DashboardFragment"

    private val recentOrders = mutableListOf<Order>()
    private val stockLevels = mutableListOf<RiceBag>()
    private val topShops = mutableListOf<TopShopInfo>() // now holding detailed info

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize UI components
        textViewTotalSales = view.findViewById(R.id.textViewTotalSales)
        textViewOutstandingPayments = view.findViewById(R.id.textViewOutstandingPayments)
        textViewTotalOrders = view.findViewById(R.id.textViewTotalOrders)
        textViewAverageOrderValue = view.findViewById(R.id.textViewAverageOrderValue)
        lineChartSalesTrend = view.findViewById(R.id.lineChartSalesTrend)

        recyclerViewRecentOrders = view.findViewById(R.id.recyclerViewRecentOrders)
        recyclerViewStockLevels = view.findViewById(R.id.recyclerViewStockLevels)
        recyclerViewTopShops = view.findViewById(R.id.recyclerViewTopShops)

        recyclerViewRecentOrders.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewStockLevels.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewTopShops.layoutManager = LinearLayoutManager(requireContext())

        recyclerViewRecentOrders.adapter = RecentOrdersAdapter(requireContext(), recentOrders)
        recyclerViewStockLevels.adapter = StockLevelsAdapter(requireContext(), stockLevels)
        recyclerViewTopShops.adapter = TopShopsAdapter(requireContext(), topShops)

        pieChart = view.findViewById(R.id.pieChart)

        // Fetch and Display Data
        fetchTotalSales()
        fetchOutstandingPayments()
        fetchTotalOrdersAndAverageValue()
        fetchRecentOrders()
        fetchStockLevels()
        fetchTopShopsLast30Days()
        fetchLast7DaysSalesTrend()
        fetchPieChartData()

        return view
    }

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
                Toast.makeText(
                    requireContext(),
                    "Error fetching total sales: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

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
                textViewOutstandingPayments.text =
                    "Rs ${String.format("%.2f", outstandingPayments)}"
                Log.d(TAG, "Outstanding Payments: Rs $outstandingPayments")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching outstanding payments: ", exception)
                Toast.makeText(
                    requireContext(),
                    "Error fetching outstanding payments: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchTotalOrdersAndAverageValue() {
        db.collection("orders").get()
            .addOnSuccessListener { documents ->
                var totalOrdersCount = 0
                var totalValue = 0.0
                for (doc in documents) {
                    val order = doc.toObject(Order::class.java)
                    totalOrdersCount += 1
                    totalValue += order.totalPrice
                }
                textViewTotalOrders.text = totalOrdersCount.toString()

                val aov = if (totalOrdersCount > 0) totalValue / totalOrdersCount else 0.0
                textViewAverageOrderValue.text = "Rs ${String.format("%.2f", aov)}"
                Log.d(TAG, "Total Orders: $totalOrdersCount, AOV: $aov")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching total orders/AOV: ", exception)
                Toast.makeText(
                    requireContext(),
                    "Error fetching total orders/AOV: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchRecentOrders() {
        db.collection("orders")
            .orderBy("orderDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                recentOrders.clear()
                val shopIds = mutableListOf<String>()
                for (document in documents) {
                    val order = document.toObject(Order::class.java)
                    order.id = document.id
                    recentOrders.add(order)
                    shopIds.add(order.shopId)
                }

                if(!documents.isEmpty){
                    // Fetch shop names for the orders
                    db.collection("shops").whereIn(FieldPath.documentId(), shopIds).get()
                        .addOnSuccessListener { shopDocs ->
                            val shopMap = mutableMapOf<String, String>()
                            //Log.d(TAG, "Fetched shops: ${shopDocs.documents}")
                            Log.d(TAG, "fetchRecentOrders: shopIds " + shopIds);
                            for (shopDoc in shopDocs) {
                                val shop = shopDoc.toObject(Shop::class.java)
                                shop.id = shopDoc.id
                                Log.d(TAG, "Fetched shops: ${shop.id}")
                                shopMap[shop.id] = shop.name
                            }

                            // Update recentOrders with shop names
                            for (order in recentOrders) {
                                order.shopName = shopMap[order.shopId] ?: "Unknown Shop"
                            }

                            recyclerViewRecentOrders.adapter?.notifyDataSetChanged()
                            Log.d(TAG, "Fetched ${recentOrders.size} recent orders with shop names.")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error fetching shop names: ", e)
                            Toast.makeText(
                                requireContext(),
                                "Error fetching shop names: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching recent orders: ", exception)
                Toast.makeText(
                    requireContext(),
                    "Error fetching recent orders: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /*private fun fetchRecentOrders() {
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
                Toast.makeText(requireContext(), "Error fetching recent orders: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    } */

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
                Toast.makeText(
                    requireContext(),
                    "Error fetching stock levels: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchTopShopsLast30Days() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val startDate = cal.time
        val startTS = com.google.firebase.Timestamp(startDate)

        db.collection("orders")
            .whereGreaterThanOrEqualTo("orderDate", startTS)
            .get()
            .addOnSuccessListener { documents ->
                val shopSalesMap = HashMap<String, Double>()
                val shopIdsSet = HashSet<String>()
                for (doc in documents) {
                    val order = doc.toObject(Order::class.java)
                    shopSalesMap[order.shopId] =
                        shopSalesMap.getOrDefault(order.shopId, 0.0) + order.totalPrice
                    shopIdsSet.add(order.shopId)
                }

                if (shopIdsSet.isEmpty()) {
                    // No shops
                    topShops.clear()
                    recyclerViewTopShops.adapter?.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Fetch shop details
                db.collection("shops").whereIn(FieldPath.documentId(), shopIdsSet.toList()).get()
                    .addOnSuccessListener { shopDocs ->
                        val shopInfoMap = mutableMapOf<String, Shop>()
                        for (sdoc in shopDocs) {
                            val shop = sdoc.toObject(Shop::class.java)
                            shop.id = sdoc.id
                            shopInfoMap[shop.id] = shop
                        }

                        val sortedShops =
                            shopSalesMap.entries.sortedByDescending { it.value }.take(5)
                        topShops.clear()
                        for (e in sortedShops) {
                            val shopObj = shopInfoMap[e.key]
                            val name = shopObj?.name ?: "Unknown Shop"
                            val address = shopObj?.address
                            val contact = shopObj?.contact
                            topShops.add(
                                TopShopInfo(
                                    name = name,
                                    totalSales = e.value,
                                    address = address,
                                    contact = contact
                                )
                            )
                        }
                        recyclerViewTopShops.adapter?.notifyDataSetChanged()
                        Log.d(TAG, "Fetched top shops: ${topShops.size}")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error fetching shop names: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching top shops: ", e)
                Toast.makeText(
                    requireContext(),
                    "Error fetching top shops: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchLast7DaysSalesTrend() {
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = cal.time
        val startTS = com.google.firebase.Timestamp(startDate)

        db.collection("orders")
            .whereGreaterThanOrEqualTo("orderDate", startTS)
            .get()
            .addOnSuccessListener { documents ->
                val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                val dailySales = HashMap<String, Double>()
                for (doc in documents) {
                    val order = doc.toObject(Order::class.java)
                    order.orderDate?.toDate()?.let { d ->
                        val key = dateFormat.format(d)
                        dailySales[key] = dailySales.getOrDefault(key, 0.0) + order.totalPrice
                    }
                }

                val sortedKeys = dailySales.keys.sortedBy {
                    val dayMonth = it.split("/")
                    val day = dayMonth[0].toInt()
                    val month = dayMonth[1].toInt()
                    month * 100 + day
                }

                val entries = mutableListOf<Entry>()
                var x = 0f
                for (k in sortedKeys) {
                    entries.add(Entry(x, dailySales[k]!!.toFloat()))
                    x += 1f
                }

                val dataSet = LineDataSet(entries, "Last 7 Days Sales")
                dataSet.color = Color.BLUE
                dataSet.valueTextColor = Color.BLACK

                val lineData = LineData(dataSet)
                lineChartSalesTrend.data = lineData
                lineChartSalesTrend.invalidate()

                val desc = Description()
                desc.text = "Daily Sales"
                lineChartSalesTrend.description = desc
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error fetching sales trend: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchPieChartData() {
        db.collection("orders")
            .get()
            .addOnSuccessListener { documents ->
                val sizeSalesMap =
                    mutableMapOf<String, Double>() // Map to store sales by rice bag size
                for (document in documents) {
                    val order = document.toObject(Order::class.java)
                    val size = order.size
                    sizeSalesMap[size] = sizeSalesMap.getOrDefault(size, 0.0) + order.totalPrice
                }

                // Convert the map to PieChart entries
                val entries = mutableListOf<PieEntry>()
                for ((size, totalSales) in sizeSalesMap) {
                    entries.add(PieEntry(totalSales.toFloat(), size))
                }

                setupPieChart(entries)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching pie chart data: ", exception)
                Toast.makeText(
                    requireContext(),
                    "Error fetching pie chart data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun setupPieChart(entries: List<PieEntry>) {
        Log.d(TAG, "setupPieChart: " + entries.size);
        val dataSet = PieDataSet(entries, "Sales by Rice Bag Size")
        val colors = List(entries.size) { Color.rgb((0..255).random(), (0..255).random(), (0..255).random()) }
        dataSet.colors = colors
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.invalidate()

        // Customize the chart
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1000, Easing.EaseInOutQuad)
    }

}
