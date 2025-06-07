package com.example.sagararicemill.activities

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.SalesReportPagerAdapter
import com.example.sagararicemill.fragment.SalesReportPageFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SalesReportActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var buttonDateRange: Button
    private lateinit var fabExport: FloatingActionButton
    private val db = FirebaseFirestore.getInstance()
    private var customStartDate: Calendar? = null
    private var customEndDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_report)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        buttonDateRange = findViewById(R.id.buttonDateRange)
        fabExport = findViewById(R.id.fabExport)

        // Setup ViewPager with adapter
        val adapter = SalesReportPagerAdapter(this)
        viewPager.adapter = adapter

        // Link TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Daily"
                1 -> "Weekly"
                2 -> "Monthly"
                else -> "Unknown"
            }
        }.attach()

        // Custom Date Range Picker
        buttonDateRange.setOnClickListener {
            showDateRangePicker()
        }

        // Export Functionality
        fabExport.setOnClickListener {
            exportData()
        }
    }

    private fun showDateRangePicker() {
        val today = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            customStartDate = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
            }
            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                customEndDate = Calendar.getInstance().apply {
                    set(endYear, endMonth, endDay, 23, 59, 59)
                }
                if (customStartDate != null && customEndDate != null) {
                    (viewPager.adapter as SalesReportPagerAdapter).updateDateRange(
                        com.google.firebase.Timestamp(customStartDate!!.time),
                        com.google.firebase.Timestamp(customEndDate!!.time)
                    )
                    buttonDateRange.text = "Custom: ${SimpleDateFormat("dd/MM/yyyy").format(customStartDate!!.time)} - ${SimpleDateFormat("dd/MM/yyyy").format(customEndDate!!.time)}"
                }
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun exportData() {
        val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}") as? SalesReportPageFragment
        currentFragment?.let { fragment ->
            // Export Charts as Images
            val barChartBitmap = fragment.getBarChartBitmap()
            val pieChartBitmap = fragment.getPieChartBitmap()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            barChartBitmap?.let {
                val barFile = File(storageDir, "BarChart_$timeStamp.png")
                barFile.outputStream().use { out -> it.compress(Bitmap.CompressFormat.PNG, 100, out) }
                Toast.makeText(this, "Bar Chart saved to ${barFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
            pieChartBitmap?.let {
                val pieFile = File(storageDir, "PieChart_$timeStamp.png")
                pieFile.outputStream().use { out -> it.compress(Bitmap.CompressFormat.PNG, 100, out) }
                Toast.makeText(this, "Pie Chart saved to ${pieFile.absolutePath}", Toast.LENGTH_LONG).show()
            }

            // Export Data as CSV
            val csvFile = File(storageDir, "SalesReport_$timeStamp.csv")
//            FileWriter(csvFile).use { writer ->
//                writer.append("Type,Label,Revenue\n")
//                fragment.riceTypeRevenue.forEach { (riceType, revenue) ->
//                    writer.append("Rice Type,$riceType,$revenue\n")
//                }
//                fragment.storeRevenue.forEach { (store, revenue) ->
//                    writer.append("Store,$store,$revenue\n")
//                }
//                writer.append("Total,,${fragment.totalRevenue}\n")
//            }
            Toast.makeText(this, "CSV saved to ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}