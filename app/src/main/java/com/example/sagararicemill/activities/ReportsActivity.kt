package com.example.sagararicemill.activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.ReportsPagerAdapter
import com.example.sagararicemill.fragment.BarChartFragment
import com.example.sagararicemill.fragment.PieChartFragment
import com.example.sagararicemill.fragment.TextReportFragment
import com.example.sagararicemill.fragment.LineChartFragment
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.Shop
import com.example.sagararicemill.models.RiceBag
import com.example.sagararicemill.utils.PrinterHelper
import com.github.mikephil.charting.data.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ReportsActivity : AppCompatActivity() {

    private lateinit var spinnerReportType: Spinner
    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonGenerateReport: Button
    private lateinit var buttonPrintReport: FloatingActionButton
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    // Fragments
    private lateinit var textReportFragment: TextReportFragment
    private lateinit var pieChartFragment: PieChartFragment
    private lateinit var lineChartFragment: LineChartFragment
    private lateinit var barChartFragment: BarChartFragment

    private var startDate: Date? = null
    private var endDate: Date? = null
    private var selectedReportType = ""

    private lateinit var printerHelper: PrinterHelper
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        printerHelper = PrinterHelper(this)

        //val toolbar: Toolbar = findViewById(R.id.toolbar)
        //setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reports"

        spinnerReportType = findViewById(R.id.spinnerReportType)
        editTextStartDate = findViewById(R.id.editTextStartDate)
        editTextEndDate = findViewById(R.id.editTextEndDate)
        buttonGenerateReport = findViewById(R.id.buttonGenerateReport)
        buttonPrintReport = findViewById(R.id.buttonPrintReport)
        tabLayout = findViewById(R.id.tabLayoutReports)
        viewPager = findViewById(R.id.viewPagerReports)

        // Initialize fragments
        textReportFragment = TextReportFragment()
        pieChartFragment = PieChartFragment()
        lineChartFragment = LineChartFragment()
        barChartFragment = BarChartFragment()

        val adapter = ReportsPagerAdapter(
            this,
            textReportFragment,
            pieChartFragment,
            lineChartFragment,
            barChartFragment
        )
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Text Report"
                1 -> tab.text = "Pie Chart"
                2 -> tab.text = "Line Chart"
                3 -> tab.text = "Bar Chart"
            }
        }.attach()

        // Set up date pickers
        editTextStartDate.setOnClickListener { showDatePickerDialog(editTextStartDate, true) }
        editTextEndDate.setOnClickListener { showDatePickerDialog(editTextEndDate, false) }

        val reportTypes = arrayOf("Monthly Sales", "Shop Performance", "Inventory Analysis")
        spinnerReportType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reportTypes)
        spinnerReportType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedReportType = reportTypes[position]
            }
        }

        buttonGenerateReport.setOnClickListener { generateReport() }

//        buttonPrintReport.setOnClickListener {
//            val currentItem = viewPager.currentItem
//            when (currentItem) {
//                0 -> { // Text Report
//                    val content = textReportFragment.getCurrentReportContent()
//                    if (content.isEmpty()) {
//                        Toast.makeText(this, "No report to print", Toast.LENGTH_SHORT).show()
//                    } else {
//                        printerHelper.printReport(content)
//                    }
//                }
//                1 -> printerHelper.printReport("Printing Pie Chart summary...")
//                2 -> printerHelper.printReport("Printing Line Chart summary...")
//                3 -> printerHelper.printReport("Printing Bar Chart summary...")
//            }
//        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showDatePickerDialog(editText: EditText, isStart: Boolean) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            editText.setText(sdf.format(calendar.time))
            if (isStart) startDate = calendar.time else endDate = calendar.time
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        dialog.show()
    }

    private fun generateReport() {
        if (selectedReportType.isEmpty()) {
            Toast.makeText(this, "Select a report type", Toast.LENGTH_SHORT).show()
            return
        }
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Select start and end dates", Toast.LENGTH_SHORT).show()
            return
        }
        if (endDate!!.before(startDate)) {
            Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
            return
        }

        // We'll fetch:
        // 1) Orders in current range
        // 2) Orders in previous year range (for comparison)
        // We'll then compute monthly sales, shop performance, and top rice bags.

        val currentStartTS = Timestamp(startDate!!)
        val currentEndTS = Timestamp(endDate!!)

        // Previous year range
        val cal = Calendar.getInstance()
        cal.time = startDate!!
        cal.add(Calendar.YEAR, -1)
        val prevStartDate = cal.time
        cal.time = endDate!!
        cal.add(Calendar.YEAR, -1)
        val prevEndDate = cal.time

        val prevStartTS = Timestamp(prevStartDate)
        val prevEndTS = Timestamp(prevEndDate)

        val currentOrdersTask = db.collection("orders")
            .whereGreaterThanOrEqualTo("orderDate", currentStartTS)
            .whereLessThanOrEqualTo("orderDate", currentEndTS)
            .get()

        val previousOrdersTask = db.collection("orders")
            .whereGreaterThanOrEqualTo("orderDate", prevStartTS)
            .whereLessThanOrEqualTo("orderDate", prevEndTS)
            .get()

        // Once we have both sets of orders, we can process data
        Tasks.whenAllSuccess<QuerySnapshot>(currentOrdersTask, previousOrdersTask).addOnSuccessListener { results ->
            val currentOrdersQuery = results[0] as com.google.firebase.firestore.QuerySnapshot
            val previousOrdersQuery = results[1] as com.google.firebase.firestore.QuerySnapshot

            val currentOrders = currentOrdersQuery.documents.mapNotNull { it.toObject(Order::class.java)?.apply { id = it.id } }
            val previousOrders = previousOrdersQuery.documents.mapNotNull { it.toObject(Order::class.java)?.apply { id = it.id } }

            // Aggregate Data
            val currentMonthlySales = aggregateMonthlySales(currentOrders)
            val previousMonthlySales = aggregateMonthlySales(previousOrders)
            val shopSalesMap = aggregateShopPerformance(currentOrders)
            val riceBagSalesMap = aggregateRiceBagSales(currentOrders)

            // We need shop names for pie chart
            val shopIds = shopSalesMap.keys.toList()
            val riceBagIds = riceBagSalesMap.keys.toList()

            val shopsTask: Task<QuerySnapshot> = if (shopIds.isNotEmpty()) {
                db.collection("shops").whereIn(FieldPath.documentId(), shopIds).get()
            } else {
                // Return a dummy QuerySnapshot-like result with no documents
                Tasks.forResult<QuerySnapshot>(null)
            }

            val riceBagsTask: Task<QuerySnapshot> = if (riceBagIds.isNotEmpty()) {
                db.collection("rice_bags").whereIn(FieldPath.documentId(), riceBagIds).get()
            } else {
                Tasks.forResult<QuerySnapshot>(null)
            }

            Tasks.whenAllSuccess<QuerySnapshot>(shopsTask, riceBagsTask).addOnSuccessListener { docs ->
                // Shops
                val shopNameMap = mutableMapOf<String, String>()
                if (docs[0] != null) {
                    val shopsQuery = docs[0] as com.google.firebase.firestore.QuerySnapshot
                    for (doc in shopsQuery) {
                        val shop = doc.toObject(Shop::class.java)
                        shop.id = doc.id
                        shopNameMap[shop.id] = shop.name
                    }
                }

                // Rice Bags
                val riceBagNameMap = mutableMapOf<String, String>()
                if (docs[1] != null) {
                    val riceBagsQuery = docs[1] as com.google.firebase.firestore.QuerySnapshot
                    for (doc in riceBagsQuery) {
                        val bag = doc.toObject(RiceBag::class.java)
                        bag.id = doc.id
                        riceBagNameMap[bag.id] = "${bag.name} - ${bag.size}"
                    }
                }

                // Now build Pie Entries for shop performance
                val pieEntries = shopSalesMap.map { (shopId, sales) ->
                    PieEntry(sales.toFloat(), shopNameMap[shopId] ?: shopId)
                }

                // Line chart entries from monthly sales
                val currentYearEntries = monthlySalesToEntries(currentMonthlySales)
                val previousYearEntries = monthlySalesToEntries(previousMonthlySales)

                // Bar chart: pick top 2 rice bags
                val topBags = riceBagSalesMap.entries.sortedByDescending { it.value }.take(2)
                val categoryAEntries = mutableListOf<BarEntry>()
                val categoryBEntries = mutableListOf<BarEntry>()
                // We'll just assign index = 0,1,... for each "month" or each data point, but we only have total sums here
                // For simplicity, just show one bar per bag
                if (topBags.size >= 2) {
                    // Bar at x=0 for first bag, x=0 for second bag as well, grouped
                    categoryAEntries.add(BarEntry(0f, topBags[0].value.toFloat()))
                    categoryBEntries.add(BarEntry(0f, topBags[1].value.toFloat()))
                } else if (topBags.size == 1) {
                    categoryAEntries.add(BarEntry(0f, topBags[0].value.toFloat()))
                    // No second category
                }

                // Build textual report
                val htmlReport = StringBuilder("<b>Report: $selectedReportType</b><br>")
                htmlReport.append("From: ${formatDate(startDate!!)} To: ${formatDate(endDate!!)}<br><br>")
                htmlReport.append("<b>Top Shops by Sales:</b><br>")
                shopSalesMap.entries.sortedByDescending { it.value }.forEach { (sid, val_) ->
                    htmlReport.append("${shopNameMap[sid] ?: sid}: \$${String.format("%.2f", val_)}<br>")
                }
                htmlReport.append("<br><b>Top Rice Bags:</b><br>")
                topBags.forEach { (rbid, val_) ->
                    htmlReport.append("${riceBagNameMap[rbid] ?: rbid}: \$${String.format("%.2f", val_)}<br>")
                }

                // Update fragments
                textReportFragment.updateReportContent(htmlReport.toString())
                pieChartFragment.updatePieData(pieEntries, "Shop Performance")
                lineChartFragment.updateLineData(currentYearEntries, "Current Year", previousYearEntries, "Previous Year")
                barChartFragment.updateBarData(categoryAEntries, if (topBags.isNotEmpty()) riceBagNameMap[topBags[0].key] ?: "Bag A" else "No Data",
                    categoryBEntries, if (topBags.size > 1) riceBagNameMap[topBags[1].key] ?: "Bag B" else "No Data")

                Toast.makeText(this, "Report generated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching shops/rice bags: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    private fun aggregateMonthlySales(orders: List<Order>): Map<String, Double> {
        // Aggregate by "MMM yyyy"
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val map = HashMap<String, Double>()
        for (o in orders) {
            o.orderDate?.toDate()?.let { d ->
                val key = sdf.format(d)
                map[key] = map.getOrDefault(key, 0.0) + o.totalPrice
            }
        }
        return map
    }

    private fun aggregateShopPerformance(orders: List<Order>): Map<String, Double> {
        val map = HashMap<String, Double>()
        for (o in orders) {
            val current = map.getOrDefault(o.shopId, 0.0)
            map[o.shopId] = current + o.totalPrice
        }
        return map
    }

    private fun aggregateRiceBagSales(orders: List<Order>): Map<String, Double> {
        val map = HashMap<String, Double>()
        for (o in orders) {
            val current = map.getOrDefault(o.riceBagId, 0.0)
            map[o.riceBagId] = current + o.totalPrice
        }
        return map
    }

    private fun monthlySalesToEntries(salesMap: Map<String, Double>): List<Entry> {
        // Sort by month. Since keys are "MMM yyyy", we can parse them back to date and sort.
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val sortedKeys = salesMap.keys.sortedBy { sdf.parse(it) }
        val entries = mutableListOf<Entry>()
        var x = 0f
        for (k in sortedKeys) {
            entries.add(Entry(x, salesMap[k]!!.toFloat()))
            x += 1f
        }
        return entries
    }
}
