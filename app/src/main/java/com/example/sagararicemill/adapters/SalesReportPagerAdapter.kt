package com.example.sagararicemill.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.sagararicemill.activities.SalesReportActivity
import com.example.sagararicemill.fragment.SalesReportPageFragment
import com.google.firebase.Timestamp

class SalesReportPagerAdapter(activity: SalesReportActivity) : FragmentStateAdapter(activity) {
    private var customStart: Timestamp? = null
    private var customEnd: Timestamp? = null

    override fun getItemCount(): Int = 3 // Daily, Weekly, Monthly

    override fun createFragment(position: Int): Fragment {
        val reportType = when (position) {
            0 -> "Daily"
            1 -> "Weekly"
            2 -> "Monthly"
            else -> "Daily"
        }
        return SalesReportPageFragment.newInstance(reportType, customStart, customEnd)
    }

    fun updateDateRange(start: Timestamp, end: Timestamp) {
        customStart = start
        customEnd = end
        notifyDataSetChanged()
    }
}