package com.example.sagararicemill.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.BillAdapter
import com.example.sagararicemill.models.Bill
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class BillListFragment : Fragment() {

    private lateinit var listViewBills: ListView
    private lateinit var billAdapter: BillAdapter
    private val bills = mutableListOf<Bill>()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "BillListFragment"

    companion object {
        private const val ARG_STATUS_FILTER = "status_filter"

        fun newInstance(statusFilter: List<String>): BillListFragment {
            val fragment = BillListFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_STATUS_FILTER, ArrayList(statusFilter))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bill_list, container, false)
        listViewBills = view.findViewById(R.id.listViewBills)

        billAdapter = BillAdapter(
            requireContext(), bills,
            onMakePaymentClick = { bill -> (parentFragment as OutstandingBillsFragment).showMakePaymentDialog(bill) },
            onMarkChequeReturned = { bill -> (parentFragment as OutstandingBillsFragment).markChequeAsReturned(bill) }
        )
        listViewBills.adapter = billAdapter

        val statusFilter = arguments?.getStringArrayList(ARG_STATUS_FILTER) ?: listOf("Unpaid", "Partially Paid")
        fetchBills(statusFilter)

        return view
    }

    private fun fetchBills(statusFilter: List<String>) {
        db.collection("bills")
            .whereIn("paymentStatus", statusFilter)
            .get()
            .addOnSuccessListener { documents ->
                bills.clear()
                for (document in documents) {
                    val bill = document.toObject(Bill::class.java)
                    bill.id = document.id
                    bills.add(bill)
                }
                billAdapter.notifyDataSetChanged()
                Log.d(TAG, "Fetched ${bills.size} bills with status $statusFilter.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching bills: ", exception)
                Toast.makeText(requireContext(), "Error fetching bills: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun refreshBills() {
        val statusFilter = arguments?.getStringArrayList(ARG_STATUS_FILTER) ?: listOf("Unpaid", "Partially Paid")
        fetchBills(statusFilter)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Ensure data is refreshed after restoration
        val statusFilter = arguments?.getStringArrayList(ARG_STATUS_FILTER) ?: listOf("Unpaid", "Partially Paid")
        fetchBills(statusFilter)
    }
}