package com.example.sagararicemill.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
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
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class SalesReportPageFragment : Fragment() {

    private lateinit var reportType: String
    private var customStart: com.google.firebase.Timestamp? = null
    private var customEnd: com.google.firebase.Timestamp? = null
    private lateinit var textViewTotalRevenue: TextView
    private lateinit var barChartRiceType: BarChart
    private lateinit var pieChartStore: PieChart
    private val db = FirebaseFirestore.getInstance()
    var riceTypeRevenue = mutableMapOf<String, Double>()
    var storeRevenue = mutableMapOf<String, Double>()
    var totalRevenue = 0.0

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sales_report_page, container, false)

        textViewTotalRevenue = view.findViewById(R.id.textViewTotalRevenue)
        barChartRiceType = view.findViewById(R.id.barChartRiceType)
        pieChartStore = view.findViewById(R.id.pieChartStore)

        fetchSalesData()
        return view
    }

    private fun fetchSalesData() {
        val (start, end) = if (customStart != null && customEnd != null) {
            Pair(customStart!!, customEnd!!)
        } else {
            when (reportType) {
                "Daily" -> {
                    val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                    Pair(com.google.firebase.Timestamp(today.time), com.google.firebase.Timestamp(today.apply { add(Calendar.DAY_OF_YEAR, 1) }.time))
                }
                "Weekly" -> {
                    val weekStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                    }
                    Pair(com.google.firebase.Timestamp(weekStart.time), com.google.firebase.Timestamp(weekStart.apply { add(Calendar.DAY_OF_YEAR, 7) }.time))
                }
                "Monthly" -> {
                    val monthStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                    }
                    Pair(com.google.firebase.Timestamp(monthStart.time), com.google.firebase.Timestamp(monthStart.apply { add(Calendar.MONTH, 1) }.time))
                }
                else -> Pair(com.google.firebase.Timestamp.now(), com.google.firebase.Timestamp.now())
            }
        }

        riceTypeRevenue.clear()
        storeRevenue.clear()
        val shopCache = mutableMapOf<String, String>()

        db.collection("bills")
            .whereGreaterThanOrEqualTo("billDate", start)
            .whereLessThan("billDate", end)
            .get()
            .addOnSuccessListener { billDocs ->
                val bills = billDocs.toObjects(Bill::class.java)
                val orderIds = bills.flatMap { it.orderIds }.distinct()

                if (orderIds.isNotEmpty()) {
                    db.collection("orders")
                        .whereIn("id", orderIds)
                        .get()
                        .addOnSuccessListener { orderDocs ->
                            val orders = orderDocs.toObjects(Order::class.java)

                            val shopIds = bills.map { it.shopId }.distinct()
                            db.collection("shops")
                                .whereIn(FieldPath.documentId(), shopIds)
                                .get()
                                .addOnSuccessListener { shopDocs ->
                                    shopDocs.forEach { doc ->
                                        val shop = doc.toObject(Shop::class.java)
                                        shopCache[shop.id] = shop.name
                                    }

                                    bills.forEach { bill ->
                                        val revenue = if (bill.paymentStatus == "Paid") bill.amount else bill.paidAmount
                                        val shopName = shopCache[bill.shopId] ?: "Unknown"
                                        storeRevenue[shopName] = (storeRevenue[shopName] ?: 0.0) + revenue

                                        val billOrders = orders.filter { bill.orderIds.contains(it.id) }
                                        billOrders.forEach { order ->
                                            val orderRevenue = order.quantity * order.price
                                            riceTypeRevenue[order.riceName] = (riceTypeRevenue[order.riceName] ?: 0.0) + orderRevenue
                                        }
                                    }

                                    totalRevenue = bills.sumOf { if (it.paymentStatus == "Paid") it.amount else it.paidAmount }
                                    textViewTotalRevenue.text = "Total Revenue: Rs ${String.format("%.2f", totalRevenue)}"

                                    setupBarChart()
                                    setupPieChart()
                                }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching sales: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBarChart() {

        val entries = riceTypeRevenue.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat()) // Map entry.value (revenue) to BarEntry
        }

//        val entries = riceTypeRevenue.mapIndexed { index, (riceType, revenue) ->
//            BarEntry(index.toFloat(), revenue.toFloat())
//        }
        val dataSet = BarDataSet(entries, "Revenue by Rice Type").apply {
            colors = listOf(Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW)
            valueTextSize = 12f
        }
        val barData = BarData(dataSet).apply { barWidth = 0.5f }
        barChartRiceType.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return riceTypeRevenue.keys.elementAt(value.toInt())
                }
            }
            xAxis.textSize = 12f
            animateY(1000)
            invalidate()
        }
    }

    private fun setupPieChart() {
        val entries = storeRevenue.map { (store, revenue) ->
            PieEntry(revenue.toFloat(), store)
        }
        val dataSet = PieDataSet(entries, "Revenue by Store").apply {
            colors = listOf(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN)
            valueTextSize = 12f
            valueFormatter = PercentFormatter(pieChartStore)
        }
        val pieData = PieData(dataSet)
        pieChartStore.apply {
            data = pieData
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1000)
            invalidate()
        }
    }

    fun getBarChartBitmap(): Bitmap? = barChartRiceType.getChartBitmap()
    fun getPieChartBitmap(): Bitmap? = pieChartStore.getChartBitmap()
}