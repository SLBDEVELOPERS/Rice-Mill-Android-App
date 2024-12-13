package com.example.sagararicemill.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.sagararicemill.fragment.BarChartFragment
import com.example.sagararicemill.fragment.LineChartFragment
import com.example.sagararicemill.fragment.PieChartFragment
import com.example.sagararicemill.fragment.TextReportFragment

class ReportsPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val textReportFragment: TextReportFragment,
    private val pieChartFragment: PieChartFragment,
    private val lineChartFragment: LineChartFragment,
    private val barChartFragment: BarChartFragment
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> textReportFragment
            1 -> pieChartFragment
            2 -> lineChartFragment
            else -> barChartFragment
        }
    }
}
