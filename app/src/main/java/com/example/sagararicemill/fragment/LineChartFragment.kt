package com.example.sagararicemill.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sagararicemill.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*

class LineChartFragment : Fragment() {
    private lateinit var lineChart: LineChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_line_chart, container, false)
        lineChart = view.findViewById(R.id.lineChart)
        return view
    }

    fun updateLineData(
        currentYearEntries: List<Entry>, currentYearLabel: String,
        previousYearEntries: List<Entry>, previousYearLabel: String
    ) {
        val currentDataSet = LineDataSet(currentYearEntries, currentYearLabel).apply {
            color = resources.getColor(R.color.teal_700, null)
            valueTextColor = resources.getColor(R.color.black, null)
        }

        val previousDataSet = LineDataSet(previousYearEntries, previousYearLabel).apply {
            color = resources.getColor(R.color.purple_700, null)
            valueTextColor = resources.getColor(R.color.black, null)
        }

        val lineData = LineData(currentDataSet, previousDataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }
}
