package com.example.sagararicemill.fragment

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.sagararicemill.R

class TextReportFragment : Fragment() {
    private lateinit var textViewReport: TextView
    private var reportContent: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_text_report, container, false)
        textViewReport = view.findViewById(R.id.textViewReportContent)
        return view
    }

    fun updateReportContent(htmlContent: String) {
        reportContent = htmlContent
        textViewReport.text = Html.fromHtml(htmlContent)
    }

    fun getCurrentReportContent() = reportContent
}
