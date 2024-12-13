package com.example.sagararicemill.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sagararicemill.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*

class BarChartFragment : Fragment() {
    private lateinit var barChart: BarChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_bar_chart, container, false)
        barChart = view.findViewById(R.id.barChart)
        return view
    }

    fun updateBarData(
        categoryAEntries: List<BarEntry>, categoryALabel: String,
        categoryBEntries: List<BarEntry>, categoryBLabel: String
    ) {
        val dataSetA = BarDataSet(categoryAEntries, categoryALabel).apply {
            color = resources.getColor(R.color.green, null)
        }
        val dataSetB = BarDataSet(categoryBEntries, categoryBLabel).apply {
            color = resources.getColor(R.color.orange, null)
        }

        val barData = BarData(dataSetA, dataSetB)
        val groupSpace = 0.4f
        val barSpace = 0f
        val barWidth = 0.3f
        barData.barWidth = barWidth

        barChart.data = barData
        barChart.xAxis.setCenterAxisLabels(true)
        barChart.xAxis.granularity = 1f

        // Group the bars starting at x=0
        barChart.groupBars(0f, groupSpace, barSpace)
        barChart.invalidate()
    }
}
