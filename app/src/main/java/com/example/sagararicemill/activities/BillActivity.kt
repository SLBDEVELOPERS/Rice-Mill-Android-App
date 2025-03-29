package com.example.sagararicemill.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.OrdersAdapter
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.Shop
import com.example.sagararicemill.utils.PrinterHelper
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillActivity : AppCompatActivity() {
    private val TAG = "BillActivity"
    private lateinit var printerHelper: PrinterHelper
    private val db = FirebaseFirestore.getInstance()
    private var billId: String? = null

    // View references
    private lateinit var textViewBillId: TextView
    private lateinit var textViewBillDate: TextView
    private lateinit var textViewShopName: TextView
    private lateinit var textViewPaymentMethod: TextView
    private lateinit var recyclerViewOrders: RecyclerView
    private lateinit var textViewTotalAmount: TextView
    private lateinit var buttonPrintBill: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill)

        printerHelper = PrinterHelper(this)

        setSupportActionBar(findViewById(R.id.topAppBarShop))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        textViewBillId = findViewById(R.id.textViewBillId)
        textViewBillDate = findViewById(R.id.textViewBillDate)
        textViewShopName = findViewById(R.id.textViewShopName)
        textViewPaymentMethod = findViewById(R.id.textViewPaymentMethod)
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders)
        textViewTotalAmount = findViewById(R.id.textViewTotalAmount)
        buttonPrintBill = findViewById(R.id.buttonPrintBill)

        billId = intent.getStringExtra("billId")
        if (billId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid Bill ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerViewOrders.layoutManager = LinearLayoutManager(this)
        fetchBillAndOrders(billId!!)
    }

    private fun fetchBillAndOrders(billId: String) {
        db.collection("bills").document(billId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bill = document.toObject(Bill::class.java)
                    bill?.id = document.id
                    bill?.let {
                        if (it.orderIds.isNullOrEmpty()) {
                            Toast.makeText(
                                this,
                                "No orders associated with this bill",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                            return@addOnSuccessListener
                        }
                        fetchOrdersForBill(it)
                    }
                } else {
                    Toast.makeText(this, "Bill not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error fetching bill: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }


    private fun fetchOrdersForBill(bill: Bill) {
        db.collection("orders")
            .whereIn(FieldPath.documentId(), bill.orderIds)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val orders = querySnapshot.documents.mapNotNull { doc ->
                    val order = doc.toObject(Order::class.java)
                    order?.id = doc.id
                    order
                }

                if (orders.isEmpty()) {
                    Toast.makeText(this, "No orders found for this bill", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Assuming all orders from the same shop
                val shopId = orders.first().shopId
                fetchShopAndBindData(shopId, orders, bill)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error fetching orders: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }

    private fun fetchShopAndBindData(shopId: String, orders: List<Order>, bill: Bill) {
        db.collection("shops").document(shopId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val shop = document.toObject(Shop::class.java)
                    if (shop != null) {
                        bindDataToViews(shop, orders, bill)
                    } else {
                        Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error fetching shop: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }


    private fun fetchAndPrintBill(billId: String) {
        db.collection("bills").document(billId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val bill = document.toObject(Bill::class.java)
                    bill?.id = document.id
                    bill?.let {
                        fetchOrderAndShop(it)
                    }
                } else {
                    Toast.makeText(this, "Bill not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error fetching bill: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }

    private fun fetchOrderAndShop(bill: Bill) {
        db.collection("orders").whereIn(FieldPath.documentId(), bill.orderIds)
            .get()
            .addOnSuccessListener { querySnapshot ->

                val orders = querySnapshot.documents.mapNotNull { doc ->
                    val order = doc.toObject(Order::class.java)
                    order?.id = doc.id
                    order
                }

                if (orders.isEmpty()) {
                    Toast.makeText(this, "No orders found for this bill", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Assuming all orders are from the same shop
                val shopId = orders.first().shopId
                fetchShopAndPrint(shopId, orders, bill)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error fetching order: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }

    private fun fetchShopAndPrint(shopId: String, orders: List<Order>, bill: Bill) {
        db.collection("shops").document(shopId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val shop = document.toObject(Shop::class.java)
                    shop?.let {
                        printerHelper.printBill(it, orders, bill)
                        Toast.makeText(this, "Bill sent to printer", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error fetching shop: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }


    private fun bindDataToViews(shop: Shop, orders: List<Order>, bill: Bill) {
        textViewBillId.text = "Bill ID: ${bill.id}"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val billDateStr = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
        textViewBillDate.text = "Date: $billDateStr"

        textViewShopName.text = "Shop: ${shop.name}"
        textViewPaymentMethod.text = "Payment Method: ${bill.paymentMethod}"

        // Set up the orders list
        val adapter = OrdersAdapter(orders)
        recyclerViewOrders.adapter = adapter

        val totalAmount = orders.sumOf { it.totalPrice }
        textViewTotalAmount.text = "Total: Rs %.2f".format(totalAmount)

        buttonPrintBill.setOnClickListener {
            showPrintOrSendDialog(shop, orders, bill);
            //Toast.makeText(this, "Bill sent to printer", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showPrintOrSendDialog(shop: Shop, orderList: List<Order>, bill: Bill) {
        val options = arrayOf("Print Bill", "Export as PDF", "Share PDF")
        AlertDialog.Builder(this)
            .setTitle("Choose Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> printerHelper.printBill(shop, orderList, bill)
                    1 -> CoroutineScope(Dispatchers.Main).launch {
                        exportBillToPdf(shop, orderList, bill)
                    }
                    2 -> CoroutineScope(Dispatchers.Main).launch {
                        shareBillAsPdf(shop, orderList, bill)
                    }
                }
            }
            .show()
    }


    private fun exportBillToPdf(shop: Shop, orderList: List<Order>, bill: Bill) {
        try {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                return
            }

            val document = com.itextpdf.text.Document()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Bill_${bill.id}_$timeStamp.pdf"
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            PdfWriter.getInstance(document, FileOutputStream(file))

            document.open()

            // Add title
            val title = com.itextpdf.text.Paragraph("SAGARA RICE MILL - INVOICE")
            title.alignment = com.itextpdf.text.Element.ALIGN_CENTER
            title.font = com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA,
                16f,
                com.itextpdf.text.Font.BOLD
            )
            document.add(title)
            document.add(com.itextpdf.text.Paragraph(" "))

            // Bill details
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val billDate = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
            document.add(com.itextpdf.text.Paragraph("Bill: ${bill.id}    Date: $billDate"))
            document.add(com.itextpdf.text.Paragraph("Shop: ${shop.name}"))
            document.add(com.itextpdf.text.Paragraph("Address: ${shop.address}"))
            document.add(com.itextpdf.text.Paragraph(" "))

            // Table
            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(2.5f, 1.5f, 1f, 1.5f, 1.5f))

            // Headers
            arrayOf("Description", "Size", "Qty", "Rate", "Amount").forEach { header ->
                val cell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(header))
                cell.horizontalAlignment = com.itextpdf.text.Element.ALIGN_CENTER
                table.addCell(cell)
            }

            // Items
            for (order in orderList) {
                table.addCell(order?.riceName ?: "Unknown")
                table.addCell(order.size)
                table.addCell(order.quantity.toString())
                table.addCell(String.format("%.2f", order.price))
                table.addCell(String.format("%.2f", order.totalPrice))
            }
            document.add(table)
            document.add(com.itextpdf.text.Paragraph(" "))

            // Totals
            val subtotal = orderList.sumOf { it.totalPrice }
            val discount = 50.0
            val taxRate = 0.05
            val taxAmount = (subtotal - discount) * taxRate
            val transportFee = 100.0

            val totals = com.itextpdf.text.Paragraph()
            totals.add("Payment: ${bill.paymentMethod}\n")
            totals.add("Subtotal: Rs %.2f\n".format(subtotal))
            totals.add("Discount: Rs %.2f\n".format(discount))
            totals.add("Tax (5%%): Rs %.2f\n".format(taxAmount))
            totals.add("Transport: Rs %.2f\n".format(transportFee))
            totals.add("TOTAL: Rs %.2f".format(bill.amount))
            totals.alignment = com.itextpdf.text.Element.ALIGN_RIGHT
            document.add(totals)

            // Footer
            document.add(com.itextpdf.text.Paragraph(" "))
            document.add(com.itextpdf.text.Paragraph("Status: ${if (orderList.all { it.deliveryStatus == "Delivered" }) "Delivered" else "Partial"}"))
            document.add(com.itextpdf.text.Paragraph("Thank you!"))

            document.close()

            Toast.makeText(this, "PDF saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Open PDF with"))

        } catch (e: Exception) {
            Toast.makeText(this, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed() // Navigate back when arrow is clicked
        return true
    }

    private fun shareBillAsPdf(shop: Shop, orderList: List<Order>, bill: Bill) {
        try {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                return
            }

            val document = com.itextpdf.text.Document()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Bill_${bill.id}_$timeStamp.pdf"
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            PdfWriter.getInstance(document, FileOutputStream(file))

            document.open()

            // Add title
            val title = com.itextpdf.text.Paragraph("SAGARA RICE MILL - INVOICE")
            title.alignment = com.itextpdf.text.Element.ALIGN_CENTER
            title.font = com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA,
                16f,
                com.itextpdf.text.Font.BOLD
            )
            document.add(title)
            document.add(com.itextpdf.text.Paragraph(" "))

            // Bill details
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val billDate = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
            document.add(com.itextpdf.text.Paragraph("Bill: ${bill.id}    Date: $billDate"))
            document.add(com.itextpdf.text.Paragraph("Shop: ${shop.name}"))
            document.add(com.itextpdf.text.Paragraph("Address: ${shop.address}"))
            document.add(com.itextpdf.text.Paragraph(" "))

            // Table
            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(2.5f, 1.5f, 1f, 1.5f, 1.5f))

            // Headers
            arrayOf("Description", "Size", "Qty", "Rate", "Amount").forEach { header ->
                val cell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(header))
                cell.horizontalAlignment = com.itextpdf.text.Element.ALIGN_CENTER
                table.addCell(cell)
            }

            // Items
            for (order in orderList) {
                table.addCell(order?.riceName ?: "Unknown")
                table.addCell(order.size)
                table.addCell(order.quantity.toString())
                table.addCell(String.format("%.2f", order.price))
                table.addCell(String.format("%.2f", order.totalPrice))
            }
            document.add(table)
            document.add(com.itextpdf.text.Paragraph(" "))

            // Totals
            val subtotal = orderList.sumOf { it.totalPrice }
            val discount = 50.0
            val taxRate = 0.05
            val taxAmount = (subtotal - discount) * taxRate
            val transportFee = 100.0

            val totals = com.itextpdf.text.Paragraph()
            totals.add("Payment: ${bill.paymentMethod}\n")
            totals.add("Subtotal: Rs %.2f\n".format(subtotal))
            totals.add("Discount: Rs %.2f\n".format(discount))
            totals.add("Tax (5%%): Rs %.2f\n".format(taxAmount))
            totals.add("Transport: Rs %.2f\n".format(transportFee))
            totals.add("TOTAL: Rs %.2f".format(bill.amount))
            totals.alignment = com.itextpdf.text.Element.ALIGN_RIGHT
            document.add(totals)

            // Footer
            document.add(com.itextpdf.text.Paragraph(" "))
            document.add(com.itextpdf.text.Paragraph("Status: ${if (orderList.all { it.deliveryStatus == "Delivered" }) "Delivered" else "Partial"}"))
            document.add(com.itextpdf.text.Paragraph("Thank you!"))

            document.close()

            // Share the PDF
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "application/pdf"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Bill ${bill.id}")
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the bill for ${shop.name} dated $billDate")
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"))

            Toast.makeText(this, "PDF ready for sharing", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
