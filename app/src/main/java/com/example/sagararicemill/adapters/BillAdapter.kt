package com.example.sagararicemill.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.PaymentHistory
import com.example.sagararicemill.models.Shop
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BillAdapter(
    context: Context,
    private val bills: List<Bill>,
    private val onMakePaymentClick: (Bill) -> Unit,
    private val onMarkChequeReturned: (Bill) -> Unit
) : ArrayAdapter<Bill>(context, 0, bills) {

    private val db = FirebaseFirestore.getInstance()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val bill = bills[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_outstanding_bill, parent, false)

        val textViewBillId = view.findViewById<TextView>(R.id.textViewBillId)
        val textViewShopName = view.findViewById<TextView>(R.id.textViewShopName)
        val textViewAmount = view.findViewById<TextView>(R.id.textViewAmount)
        val textViewDueAmount = view.findViewById<TextView>(R.id.textViewDueAmount)
        val textViewPaymentMethod = view.findViewById<TextView>(R.id.textViewPaymentMethod)
        val textViewPaymentStatus = view.findViewById<TextView>(R.id.textViewPaymentStatus)
        val textViewDueDate = view.findViewById<TextView>(R.id.textViewDueDate)
        val buttonMakePayment = view.findViewById<Button>(R.id.buttonMakePayment)
        val recyclerViewPaymentHistory = view.findViewById<RecyclerView>(R.id.recyclerViewPaymentHistory)
        val buttonToggleHistory = view.findViewById<Button>(R.id.buttonToggleHistory)

        textViewBillId.text = "Bill ID: ${bill.id}"
        textViewAmount.text = "Amount: Rs ${String.format("%.2f", bill.amount)}"
        textViewDueAmount.text = "Due Amount: Rs ${String.format("%.2f", bill.amount - bill.paidAmount)}"
        textViewPaymentMethod.text = "Payment Method: ${bill.paymentMethod}"
        textViewPaymentStatus.text = "Status: ${bill.paymentStatus}"

        if (bill.orderIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val order = db.collection("orders").whereIn(FieldPath.documentId(),bill.orderIds).get().await().toObjects(Order::class.java)

                    db.collection("shops").document(order.first().shopId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val shop = document.toObject(Shop::class.java)
                                shop?.let {
                                    textViewShopName.text = "Shop: ${shop.name ?: "Unknown"}"
                                }
                            }
                        }
                        .addOnFailureListener { exception ->

                        }
                } catch (e: Exception) {
                    textViewShopName.text = "Shop: Error fetching"
                }
            }
        } else {
            textViewShopName.text = "Shop: N/A"
        }

        if (bill.dueDate != null) {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            textViewDueDate.text = "Due Date: ${dateFormat.format(bill.dueDate!!.toDate())}"
            textViewDueDate.visibility = View.VISIBLE
        } else {
            textViewDueDate.visibility = View.GONE
        }

        // Setup Payment History RecyclerView
        val paymentHistoryAdapter = PaymentHistoryAdapter(context, bill.paymentHistory)
        recyclerViewPaymentHistory.layoutManager = LinearLayoutManager(context)
        recyclerViewPaymentHistory.adapter = paymentHistoryAdapter
        recyclerViewPaymentHistory.visibility = View.GONE

        // Toggle Payment History Visibility
        buttonToggleHistory.setOnClickListener {
            if (recyclerViewPaymentHistory.visibility == View.GONE) {
                recyclerViewPaymentHistory.visibility = View.VISIBLE
                buttonToggleHistory.text = "Hide Payment History"
            } else {
                recyclerViewPaymentHistory.visibility = View.GONE
                buttonToggleHistory.text = "Show Payment History"
            }
        }

        buttonMakePayment.setOnClickListener {
            onMakePaymentClick(bill)
        }

        val buttonMarkChequeReturned = view.findViewById<Button>(R.id.buttonMarkChequeReturned)

        // Show "Mark Cheque as Returned" button only for cheque payments
        if (bill.paymentMethod == "Cheque" && bill.paymentStatus != "Paid") {
            buttonMarkChequeReturned.visibility = View.VISIBLE
            buttonMarkChequeReturned.setOnClickListener {
                //onMarkChequeReturned(bill)
                onMarkChequeReturned(bill)
            }
        } else {
            buttonMarkChequeReturned.visibility = View.GONE
        }


        return view
    }

}
