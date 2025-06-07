//package com.example.sagararicemill.fragment
//
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import com.example.sagararicemill.R
//import com.example.sagararicemill.models.Bill
//import com.example.sagararicemill.models.Order
//import com.example.sagararicemill.models.Shop
//import com.github.mikephil.charting.charts.BarChart
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.charts.PieChart
//import com.github.mikephil.charting.data.*
//import com.github.mikephil.charting.formatter.PercentFormatter
//import com.google.firebase.firestore.FieldPath
//import com.google.firebase.firestore.FirebaseFirestore
//import java.util.*
//
//class SalesReportPageFragment : Fragment() {
//
//    private val TAG = "SalesReportPageFragment"
//
//    private lateinit var reportType: String
//    private var customStart: com.google.firebase.Timestamp? = null
//    private var customEnd: com.google.firebase.Timestamp? = null
//    private lateinit var textViewTotalRevenue: TextView
//    private lateinit var barChartRiceType: BarChart
//    private lateinit var pieChartStore: PieChart
//    private lateinit var lineChartTrends: LineChart
//    private val db = FirebaseFirestore.getInstance()
//    var riceTypeRevenue = mutableMapOf<String, Double>()
//    var storeRevenue = mutableMapOf<String, Double>()
//    var dailyRevenueTrend = mutableMapOf<String, Double>()
//    var totalRevenue = 0.0
//    var totalOrders = 0
//    var avgOrderValue = 0.0
//
//    companion object {
//        private const val ARG_REPORT_TYPE = "report_type"
//        private const val ARG_START_DATE = "start_date"
//        private const val ARG_END_DATE = "end_date"
//        fun newInstance(reportType: String, start: com.google.firebase.Timestamp?, end: com.google.firebase.Timestamp?): SalesReportPageFragment {
//            val fragment = SalesReportPageFragment()
//            val args = Bundle().apply {
//                putString(ARG_REPORT_TYPE, reportType)
//                putParcelable(ARG_START_DATE, start)
//                putParcelable(ARG_END_DATE, end)
//            }
//            fragment.arguments = args
//            return fragment
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        reportType = arguments?.getString(ARG_REPORT_TYPE) ?: "Daily"
//        customStart = arguments?.getParcelable(ARG_START_DATE)
//        customEnd = arguments?.getParcelable(ARG_END_DATE)
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_sales_report_page, container, false)
//
//        textViewTotalRevenue = view.findViewById(R.id.textViewTotalRevenue)
//        barChartRiceType = view.findViewById(R.id.barChartRiceType)
//        pieChartStore = view.findViewById(R.id.pieChartStore)
//        lineChartTrends = view.findViewById(R.id.lineChartTrends)
//
//        fetchSalesData()
//        return view
//    }
//
//    private fun fetchSalesData() {
//        val (start, end) = getTimeRange()
//
//        riceTypeRevenue.clear()
//        storeRevenue.clear()
//        dailyRevenueTrend.clear()
//        totalRevenue = 0.0
//        totalOrders = 0
//
//        Log.d(TAG, "Fetching data from ${start.toDate()} to ${end.toDate()}")
//
//        db.collection("bills")
//            .whereGreaterThanOrEqualTo("billDate", start)
//            .whereLessThanOrEqualTo("billDate", end)
//            .get()
//            .addOnSuccessListener { billDocs ->
//                if (billDocs.isEmpty) {
//                    Log.d(TAG, "No bills found for the given time range")
//                    showNoDataMessage()
//                    return@addOnSuccessListener
//                }
//
//                val bills = billDocs.toObjects(Bill::class.java)
//                val orderIds = bills.flatMap { it.orderIds }.distinct()
//                val shopIds = bills.map { it.shopId }.distinct()
//
//                Log.d(TAG, "Found ${bills.size} bills, ${orderIds.size} unique orderIds, ${shopIds.size} unique shopIds")
//
//                fetchShops(shopIds) { shopCache ->
//                    if (orderIds.isEmpty()) {
//                        Log.d(TAG, "No orders linked to bills")
//                        processBillsWithoutOrders(bills, shopCache)
//                    } else {
//                        fetchOrders(orderIds, bills, shopCache)
//                    }
//                }
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(requireContext(), "Error fetching bills: ${e.message}", Toast.LENGTH_LONG).show()
//                Log.e(TAG, "Bill fetch failed", e)
//            }
//    }
//
//    private fun getTimeRange(): Pair<com.google.firebase.Timestamp, com.google.firebase.Timestamp> {
//        return if (customStart != null && customEnd != null) {
//            Pair(customStart!!, customEnd!!)
//        } else {
//            when (reportType) {
//                "Daily" -> {
//                    val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
//                    Pair(com.google.firebase.Timestamp(today.time), com.google.firebase.Timestamp(today.apply { add(Calendar.DAY_OF_YEAR, 1) }.time))
//                }
//                "Weekly" -> {
//                    val weekStart = Calendar.getInstance().apply {
//                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
//                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
//                    }
//                    Pair(com.google.firebase.Timestamp(weekStart.time), com.google.firebase.Timestamp(weekStart.apply { add(Calendar.DAY_OF_YEAR, 7) }.time))
//                }
//                "Monthly" -> {
//                    val monthStart = Calendar.getInstance().apply {
//                        set(Calendar.DAY_OF_MONTH, 1)
//                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
//                    }
//                    Pair(com.google.firebase.Timestamp(monthStart.time), com.google.firebase.Timestamp(monthStart.apply { add(Calendar.MONTH, 1) }.time))
//                }
//                else -> Pair(com.google.firebase.Timestamp.now(), com.google.firebase.Timestamp.now())
//            }
//        }
//    }
//
//    private fun fetchShops(shopIds: List<String>, callback: (Map<String, String>) -> Unit) {
//        val shopCache = mutableMapOf<String, String>()
//        if (shopIds.isEmpty()) {
//            Log.d(TAG, "No shop IDs to fetch")
//            callback(shopCache)
//            return
//        }
//        db.collection("shops")
//            .whereIn(FieldPath.documentId(), shopIds)
//            .get()
//            .addOnSuccessListener { shopDocs ->
//                if (shopDocs.isEmpty) {
//                    Log.w(TAG, "No shops found for IDs: $shopIds")
//                }
//                shopDocs.forEach { doc ->
//                    val shop = doc.toObject(Shop::class.java)
//                    shopCache[shop.id] = shop.name
//                    Log.d(TAG, "Shop cached: ${shop.id} -> ${shop.name}")
//                }
//                callback(shopCache)
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Shop fetch failed", e)
//                callback(shopCache) // Proceed with empty cache
//            }
//    }
//
//    private fun fetchOrders(orderIds: List<String>, bills: List<Bill>, shopCache: Map<String, String>) {
//        db.collection("orders")
//            .whereIn("id", orderIds)
//            .get()
//            .addOnSuccessListener { orderDocs ->
//                if (orderDocs.isEmpty) {
//                    Log.w(TAG, "No orders found for IDs: $orderIds")
//                }
//                val orders = orderDocs.toObjects(Order::class.java)
//                Log.d(TAG, "Fetched ${orders.size} orders")
//                processSalesData(bills, orders, shopCache)
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Order fetch failed", e)
//                processBillsWithoutOrders(bills, shopCache)
//            }
//    }
//
//    private fun processBillsWithoutOrders(bills: List<Bill>, shopCache: Map<String, String>) {
//        bills.forEach { bill ->
//            val revenue = if (bill.paymentStatus == "Paid") bill.amount else bill.paidAmount
//            totalRevenue += revenue
//            val shopName = shopCache[bill.shopId] ?: "Unknown (${bill.shopId})"
//            storeRevenue[shopName] = (storeRevenue[shopName] ?: 0.0) + revenue
//
//            val dateKey = bill.billDate?.toDate()?.let { java.text.SimpleDateFormat("yyyy-MM-dd").format(it) } ?: "Unknown"
//            dailyRevenueTrend[dateKey] = (dailyRevenueTrend[dateKey] ?: 0.0) + revenue
//            Log.d(TAG, "Processed bill ${bill.id}: Revenue=$revenue, Shop=$shopName")
//        }
//        updateUI()
//    }
//
//    private fun processSalesData(bills: List<Bill>, orders: List<Order>, shopCache: Map<String, String>) {
//        bills.forEach { bill ->
//            val revenue = if (bill.paymentStatus == "Paid") bill.amount else bill.paidAmount
//            totalRevenue += revenue
//            val shopName = shopCache[bill.shopId] ?: "Unknown (${bill.shopId})"
//            storeRevenue[shopName] = (storeRevenue[shopName] ?: 0.0) + revenue
//
//            val dateKey = bill.billDate?.toDate()?.let { java.text.SimpleDateFormat("yyyy-MM-dd").format(it) } ?: "Unknown"
//            dailyRevenueTrend[dateKey] = (dailyRevenueTrend[dateKey] ?: 0.0) + revenue
//
//            val billOrders = orders.filter { bill.orderIds.contains(it.id) }
//            totalOrders += billOrders.size
//
//            billOrders.forEach { order ->
//                val orderRevenue = order.quantity * order.price
//                riceTypeRevenue[order.riceName] = (riceTypeRevenue[order.riceName] ?: 0.0) + orderRevenue
//                Log.d(TAG, "Order ${order.id}: Rice=${order.riceName}, Revenue=$orderRevenue")
//            }
//            Log.d(TAG, "Processed bill ${bill.id}: Revenue=$revenue, Shop=$shopName, Orders=${billOrders.size}")
//        }
//
//        avgOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0.0
//        Log.d(TAG, "Final data: riceTypeRevenue=$riceTypeRevenue, storeRevenue=$storeRevenue")
//        updateUI()
//    }
//
//    private fun updateUI() {
//        textViewTotalRevenue.text = """
//            Total Revenue: Rs ${String.format("%.2f", totalRevenue)}
//            Total Orders: $totalOrders
//            Avg Order: Rs ${String.format("%.2f", avgOrderValue)}
//        """.trimIndent()
//
//        setupBarChart()
//        setupPieChart()
//        setupLineChart()
//    }
//
//    private fun showNoDataMessage() {
//        textViewTotalRevenue.text = "No data available for this period"
//        barChartRiceType.setNoDataText("No rice type data")
//        pieChartStore.setNoDataText("No store data")
//        lineChartTrends.setNoDataText("No trend data")
//    }
//
//    private fun setupBarChart() {
//        if (riceTypeRevenue.isEmpty()) {
//            barChartRiceType.setNoDataText("No rice type revenue data available")
//            Log.w(TAG, "BarChart: riceTypeRevenue is empty")
//            return
//        }
//
//        val entries = riceTypeRevenue.entries.mapIndexed { index, entry ->
//            BarEntry(index.toFloat(), entry.value.toFloat())
//        }
//        val dataSet = BarDataSet(entries, "Revenue by Rice Type").apply {
//            colors = listOf(Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW)
//            valueTextSize = 12f
//        }
//        val barData = BarData(dataSet).apply { barWidth = 0.5f }
//        barChartRiceType.apply {
//            data = barData
//            description.isEnabled = false
//            setFitBars(true)
//            xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
//                override fun getFormattedValue(value: Float): String {
//                    return riceTypeRevenue.keys.elementAtOrNull(value.toInt()) ?: ""
//                }
//            }
//            xAxis.textSize = 12f
//            animateY(1000)
//            invalidate()
//            Log.d(TAG, "BarChart setup with ${entries.size} entries")
//        }
//    }
//
//    private fun setupPieChart() {
//        if (storeRevenue.isEmpty()) {
//            pieChartStore.setNoDataText("No store revenue data available")
//            Log.w(TAG, "PieChart: storeRevenue is empty")
//            return
//        }
//
//        val entries = storeRevenue.map { (store, revenue) ->
//            PieEntry(revenue.toFloat(), store)
//        }
//        val dataSet = PieDataSet(entries, "Revenue by Store").apply {
//            colors = listOf(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN)
//            valueTextSize = 12f
//            valueFormatter = PercentFormatter(pieChartStore)
//        }
//        val pieData = PieData(dataSet)
//        pieChartStore.apply {
//            data = pieData
//            description.isEnabled = false
//            setUsePercentValues(true)
//            setEntryLabelColor(Color.BLACK)
//            setEntryLabelTextSize(12f)
//            animateY(1000)
//            invalidate()
//            Log.d(TAG, "PieChart setup with ${entries.size} entries")
//        }
//    }
//
//    private fun setupLineChart() {
//        if (dailyRevenueTrend.isEmpty()) {
//            lineChartTrends.setNoDataText("No trend data available")
//            Log.w(TAG, "LineChart: dailyRevenueTrend is empty")
//            return
//        }
//
//        val entries = dailyRevenueTrend.entries.sortedBy { it.key }.mapIndexed { index, entry ->
//            Entry(index.toFloat(), entry.value.toFloat())
//        }
//        val dataSet = LineDataSet(entries, "Daily Revenue Trend").apply {
//            color = Color.BLUE
//            valueTextSize = 12f
//            setDrawCircles(true)
//            setCircleColor(Color.BLUE)
//        }
//        val lineData = LineData(dataSet)
//        lineChartTrends.apply {
//            data = lineData
//            description.isEnabled = false
//            xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
//                override fun getFormattedValue(value: Float): String {
//                    return dailyRevenueTrend.keys.elementAtOrNull(value.toInt())?.substring(5) ?: ""
//                }
//            }
//            xAxis.textSize = 12f
//            animateX(1000)
//            invalidate()
//            Log.d(TAG, "LineChart setup with ${entries.size} entries")
//        }
//    }
//
//    fun getBarChartBitmap(): Bitmap? = barChartRiceType.getChartBitmap()
//    fun getPieChartBitmap(): Bitmap? = pieChartStore.getChartBitmap()
//    fun getLineChartBitmap(): Bitmap? = lineChartTrends.getChartBitmap()
//}

package com.example.sagararicemill.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.Shop
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SalesReportPageFragment : Fragment() {

    private val TAG = "SalesReportPageFragment"
    private lateinit var reportType: String
    private var customStart: com.google.firebase.Timestamp? = null
    private var customEnd: com.google.firebase.Timestamp? = null
    private lateinit var textViewSummary: TextView
    private lateinit var barChartRiceRevenue: BarChart
    private lateinit var pieChartPaymentMethod: PieChart
    private lateinit var lineChartOrderTrend: LineChart

    private val db = FirebaseFirestore.getInstance()
    private var riceRevenue = mutableMapOf<String, Double>()
    private var paymentMethodRevenue = mutableMapOf<String, Double>()
    private var orderTrend = mutableMapOf<String, Int>()
    private var totalRevenue = 0.0
    private var totalOrders = 0

    companion object {
        private const val ARG_REPORT_TYPE = "report_type"
        private const val ARG_START_DATE = "start_date"
        private const val ARG_END_DATE = "end_date"
        fun newInstance(reportType: String, start: com.google.firebase.Timestamp?, end: com.google.firebase.Timestamp?): SalesReportPageFragment {
            val fragment = SalesReportPageFragment()
            val args = Bundle().apply {
                putString(ARG_REPORT_TYPE, reportType)
                putParcelable(ARG_START_DATE, start)
                putParcelable(ARG_END_DATE, end)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportType = arguments?.getString(ARG_REPORT_TYPE) ?: "Daily"
        customStart = arguments?.getParcelable(ARG_START_DATE)
        customEnd = arguments?.getParcelable(ARG_END_DATE)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sales_report_page, container, false)

        textViewSummary = view.findViewById(R.id.textViewSummary)
        barChartRiceRevenue = view.findViewById(R.id.barChartRiceRevenue)
        pieChartPaymentMethod = view.findViewById(R.id.pieChartPaymentMethod)
        lineChartOrderTrend = view.findViewById(R.id.lineChartOrderTrend)

        fetchReportData()
        return view
    }

    private fun fetchReportData() {
        val (start, end) = getTimeRange()
        Log.d(TAG, "Fetching data from ${start.toDate()} to ${end.toDate()}")

        // Clear previous data
        riceRevenue.clear()
        paymentMethodRevenue.clear()
        orderTrend.clear()
        totalRevenue = 0.0
        totalOrders = 0

        // Fetch orders first, then link with bills and shops
        db.collection("orders")
            .whereGreaterThanOrEqualTo("orderDate", start)
            .whereLessThanOrEqualTo("orderDate", end)
            .get()
            .addOnSuccessListener { orderDocs ->
                if (orderDocs.isEmpty) {
                    Log.w(TAG, "No orders found in range")
                    showNoDataMessage()
                    return@addOnSuccessListener
                }

                val orders = orderDocs.toObjects(Order::class.java)
                totalOrders = orders.size
                Log.d(TAG, "Fetched $totalOrders orders")

                // Process orders for rice revenue and order trend
                orders.forEach { order ->
                    val revenue = order.price * order.quantity
                    riceRevenue[order.riceName] = (riceRevenue[order.riceName] ?: 0.0) + revenue
                    totalRevenue += revenue

                    val dateKey = order.orderDate?.toDate()?.let { SimpleDateFormat("yyyy-MM-dd").format(it) } ?: "Unknown"
                    orderTrend[dateKey] = (orderTrend[dateKey] ?: 0) + 1
                    Log.d(TAG, "Order ${order.id}: Rice=${order.riceName}, Revenue=$revenue, Date=$dateKey")
                }

                // Fetch related bills for payment method data
                val orderIds = orders.map { it.id }
                fetchBills(orderIds)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Order fetch failed", e)
                Toast.makeText(requireContext(), "Failed to fetch orders: ${e.message}", Toast.LENGTH_LONG).show()
                showNoDataMessage()
            }
    }

    private fun fetchBills(orderIds: List<String>) {
        if (orderIds.isEmpty()) {
            Log.d(TAG, "No order IDs to fetch bills")
            fetchShops(emptyList())
            return
        }

        db.collection("bills")
            .whereArrayContainsAny("orderIds", orderIds)
            .get()
            .addOnSuccessListener { billDocs ->
                val bills = billDocs.toObjects(Bill::class.java)
                Log.d(TAG, "Fetched ${bills.size} bills")

                bills.forEach { bill ->
                    val revenue = if (bill.paymentStatus == "Paid") bill.amount else bill.paidAmount
                    paymentMethodRevenue[bill.paymentMethod] = (paymentMethodRevenue[bill.paymentMethod] ?: 0.0) + revenue
                    Log.d(TAG, "Bill ${bill.id}: PaymentMethod=${bill.paymentMethod}, Revenue=$revenue")
                }

                fetchShops(bills.map { it.shopId }.distinct())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Bill fetch failed", e)
                fetchShops(emptyList()) // Proceed without bills
            }
    }

    private fun fetchShops(shopIds: List<String>) {
        val shopCache = mutableMapOf<String, String>()
        if (shopIds.isEmpty()) {
            Log.d(TAG, "No shops to fetch")
            updateUI()
            return
        }

        db.collection("shops")
            .get() // Fetch all shops for simplicity, adjust if needed
            .addOnSuccessListener { shopDocs ->
                shopDocs.forEach { doc ->
                    val shop = doc.toObject(Shop::class.java)
                    shopCache[shop.id] = shop.name
                }
                Log.d(TAG, "Fetched ${shopCache.size} shops: $shopCache")
                updateUI()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Shop fetch failed", e)
                updateUI() // Proceed without shop names
            }
    }

    private fun getTimeRange(): Pair<com.google.firebase.Timestamp, com.google.firebase.Timestamp> {
        return if (customStart != null && customEnd != null) {
            Pair(customStart!!, customEnd!!)
        } else {
            val calendar = Calendar.getInstance()
            when (reportType) {
                "Daily" -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                    val start = com.google.firebase.Timestamp(calendar.time)
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val end = com.google.firebase.Timestamp(calendar.time)
                    Pair(start, end)
                }
                "Weekly" -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                    val start = com.google.firebase.Timestamp(calendar.time)
                    calendar.add(Calendar.DAY_OF_YEAR, 7)
                    val end = com.google.firebase.Timestamp(calendar.time)
                    Pair(start, end)
                }
                "Monthly" -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                    val start = com.google.firebase.Timestamp(calendar.time)
                    calendar.add(Calendar.MONTH, 1)
                    val end = com.google.firebase.Timestamp(calendar.time)
                    Pair(start, end)
                }
                else -> Pair(com.google.firebase.Timestamp.now(), com.google.firebase.Timestamp.now())
            }
        }
    }

    private fun updateUI() {
        textViewSummary.text = "Total Revenue: Rs ${String.format("%.2f", totalRevenue)}\nTotal Orders: $totalOrders"
        Log.d(TAG, "Updating UI with riceRevenue=$riceRevenue, paymentMethodRevenue=$paymentMethodRevenue, orderTrend=$orderTrend")

        setupBarChart()
        setupPieChart()
        setupLineChart()
    }

    private fun showNoDataMessage() {
        textViewSummary.text = "No data available"
        barChartRiceRevenue.setNoDataText("No rice revenue data")
        pieChartPaymentMethod.setNoDataText("No payment method data")
        lineChartOrderTrend.setNoDataText("No order trend data")
    }

    private fun setupBarChart() {
        if (riceRevenue.isEmpty()) {
            barChartRiceRevenue.setNoDataText("No rice revenue data available")
            Log.w(TAG, "BarChart: No rice revenue data")
            return
        }

        val entries = riceRevenue.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat()) // Use entry.value for revenue
        }
        val dataSet = BarDataSet(entries, "Revenue by Rice Type").apply {
            colors = listOf(Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW, Color.MAGENTA)
            valueTextSize = 12f
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.0f", value)
                }
            })
        }
        val barData = BarData(dataSet).apply { barWidth = 0.5f }
        barChartRiceRevenue.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return riceRevenue.keys.elementAtOrNull(value.toInt()) ?: ""
                }
            }
            xAxis.textSize = 10f
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateY(1000)
            notifyDataSetChanged()
            invalidate()
            Log.d(TAG, "BarChart set up with ${entries.size} entries: $riceRevenue")
        }
    }

    private fun setupPieChart() {
        if (paymentMethodRevenue.isEmpty()) {
            pieChartPaymentMethod.setNoDataText("No payment method data available")
            Log.w(TAG, "PieChart: No payment method data")
            return
        }

        val entries = paymentMethodRevenue.map { (method, revenue) ->
            PieEntry(revenue.toFloat(), method)
        }
        val dataSet = PieDataSet(entries, "Revenue by Payment Method").apply {
            colors = listOf(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN, Color.BLUE)
            valueTextSize = 12f
            valueFormatter = PercentFormatter(pieChartPaymentMethod)
        }
        val pieData = PieData(dataSet)
        pieChartPaymentMethod.apply {
            data = pieData
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            animateY(1000)
            notifyDataSetChanged()
            invalidate()
            Log.d(TAG, "PieChart set up with ${entries.size} entries: $paymentMethodRevenue")
        }
    }

    private fun setupLineChart() {
        if (orderTrend.isEmpty()) {
            lineChartOrderTrend.setNoDataText("No order trend data available")
            Log.w(TAG, "LineChart: No order trend data")
            return
        }

        val sortedEntries = orderTrend.entries.sortedBy { it.key }.mapIndexed { index, entry ->
            Entry(index.toFloat(), entry.value.toFloat())
        }
        val dataSet = LineDataSet(sortedEntries, "Orders Over Time").apply {
            color = Color.BLUE
            valueTextSize = 12f
            setDrawCircles(true)
            setCircleColor(Color.BLUE)
            setDrawValues(false)
        }
        val lineData = LineData(dataSet)
        lineChartOrderTrend.apply {
            data = lineData
            description.isEnabled = false
            xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return orderTrend.keys.sorted().elementAtOrNull(value.toInt())?.substring(5) ?: ""
                }
            }
            xAxis.textSize = 10f
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateX(1000)
            notifyDataSetChanged()
            invalidate()
            Log.d(TAG, "LineChart set up with ${sortedEntries.size} entries: $orderTrend")
        }
    }

    fun getBarChartBitmap(): Bitmap? = barChartRiceRevenue.getChartBitmap()
    fun getPieChartBitmap(): Bitmap? = pieChartPaymentMethod.getChartBitmap()
    fun getLineChartBitmap(): Bitmap? = lineChartOrderTrend.getChartBitmap()
}