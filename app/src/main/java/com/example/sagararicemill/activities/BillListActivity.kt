package com.example.sagararicemill.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.BillListAdapter
import com.example.sagararicemill.models.Bill
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date

class BillListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerViewBills: androidx.recyclerview.widget.RecyclerView
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var textViewNoBills: TextView

    private lateinit var spinnerPaymentMethodFilter: Spinner
    private lateinit var buttonDateFilter: Button

    private var bills = mutableListOf<Bill>()
    private lateinit var adapter: BillListAdapter

    private var selectedPaymentMethod: String? = null
    private var selectedDate: Date? = null // The chosen date by user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill_list)

        recyclerViewBills = findViewById(R.id.recyclerViewBills)
        progressBarLoading = findViewById(R.id.progressBarLoading)
        textViewNoBills = findViewById(R.id.textViewNoBills)
        spinnerPaymentMethodFilter = findViewById(R.id.spinnerPaymentMethodFilter)
        buttonDateFilter = findViewById(R.id.buttonDateFilter)

        recyclerViewBills.layoutManager = LinearLayoutManager(this)

        adapter = BillListAdapter(bills) { bill ->
            val intent = Intent(this, BillActivity::class.java)
            intent.putExtra("billId", bill.id)
            startActivity(intent)
        }
        recyclerViewBills.adapter = adapter

        setupPaymentMethodFilter()

        buttonDateFilter.setOnClickListener {
            showDatePickerDialog()
        }

        fetchBills()
    }

    private fun setupPaymentMethodFilter() {
        val methods = listOf("All", "Cash", "Cheque", "Credit")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, methods)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaymentMethodFilter.adapter = spinnerAdapter
        spinnerPaymentMethodFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPaymentMethod = if (position == 0) null else methods[position]
                fetchBills() // Re-fetch with filter
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, y, m, d ->
            // User chose a date (y, m, d)
            val chosenCal = Calendar.getInstance()
            chosenCal.set(y, m, d, 0, 0, 0)
            chosenCal.set(Calendar.MILLISECOND, 0)
            selectedDate = chosenCal.time
            fetchBills() // Re-fetch with the new date filter
        }, year, month, day)

        datePicker.show()
    }

    private fun fetchBills() {
        showLoading(true)

        var query: Query = db.collection("bills")

        // Apply payment method filter if selected
        if (selectedPaymentMethod != null) {
            query = query.whereEqualTo("paymentMethod", selectedPaymentMethod)
        }

        // If you want to filter by date or date range, add a whereGreaterThan/whereLessThan here
        if (selectedDate != null) {
            val startOfDay = Calendar.getInstance()
            startOfDay.time = selectedDate!!
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            startOfDay.set(Calendar.MILLISECOND, 0)

            val endOfDay = Calendar.getInstance()
            endOfDay.time = selectedDate!!
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)
            endOfDay.set(Calendar.MILLISECOND, 999)

            val startTimestamp = Timestamp(startOfDay.time)
            val endTimestamp = Timestamp(endOfDay.time)

            query = query.whereGreaterThanOrEqualTo("billDate", startTimestamp)
                .whereLessThanOrEqualTo("billDate", endTimestamp)
        }


        query.orderBy("billDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                bills.clear()
                for (doc in docs) {
                    val bill = doc.toObject(Bill::class.java)
                    bill.id = doc.id
                    bills.add(bill)
                }
                adapter.notifyDataSetChanged()
                showLoading(false)

                textViewNoBills.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching bills: ${it.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    private fun showLoading(loading: Boolean) {
        progressBarLoading.visibility = if (loading) View.VISIBLE else View.GONE
        recyclerViewBills.visibility = if (loading) View.GONE else View.VISIBLE
        textViewNoBills.visibility = View.GONE
    }
}
