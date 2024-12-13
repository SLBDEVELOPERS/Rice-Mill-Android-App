package com.example.sagararicemill.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sagararicemill.R
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate

class PieChartFragment : Fragment() {
    private lateinit var pieChart: PieChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_pie_chart, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        return view
    }

    fun updatePieData(entries: List<PieEntry>, label: String) {
        val dataSet = PieDataSet(entries, label)
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }
}
