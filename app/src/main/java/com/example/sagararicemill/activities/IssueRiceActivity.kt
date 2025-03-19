package com.example.sagararicemill.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.CartAdapter
import com.example.sagararicemill.models.*
import com.example.sagararicemill.utils.PrinterHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class IssueRiceActivity : AppCompatActivity() {

    private val TAG = "IssueRiceActivity"

    private lateinit var spinnerShops: Spinner
    private lateinit var spinnerLoadedRiceBags: Spinner
    private lateinit var editTextLoadQuantity: EditText
    private lateinit var buttonAddToCart: Button
    private lateinit var recyclerViewCart: RecyclerView
    private lateinit var buttonIssueAll: Button

    private lateinit var radioGroupPaymentMethod: RadioGroup
    private lateinit var radioCash: RadioButton
    private lateinit var radioCheque: RadioButton
    private lateinit var radioCredit: RadioButton
    private lateinit var layoutChequeDetails: LinearLayout
    private lateinit var layoutCreditDetails: LinearLayout
    private lateinit var editTextChequeNumber: EditText
    private lateinit var editTextBankName: EditText
    private lateinit var editTextCreditTerm: EditText
    private lateinit var editTextAdvancePaid: EditText

    private lateinit var textViewShopDue: TextView
    private lateinit var textViewAvailableStock: TextView
    private lateinit var textViewCartTotal: TextView

    private val db = FirebaseFirestore.getInstance()

    private val shopList = mutableListOf<Shop>()
    private val loadedRiceBagsList = mutableListOf<RiceBag>()
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter

    private lateinit var printerHelper: PrinterHelper

    // Responsive Metrics class
    data class ResponsiveMetrics(
        val maxWidth: Int,
        val colWidths: FloatArray,
        val fontSize: Float,
        val lineHeight: Float
    ) {
        companion object {
            fun calculate(context: Context): ResponsiveMetrics {
                val displayMetrics = context.resources.displayMetrics
                val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
                val baseWidth = minOf(screenWidthDp.toInt() / 10, 60)

                val fontSize = when {
                    screenWidthDp < 320 -> 10f
                    screenWidthDp > 600 -> 14f
                    else -> 12f
                }

                return ResponsiveMetrics(
                    maxWidth = baseWidth,
                    colWidths = floatArrayOf(baseWidth * 0.35f, baseWidth * 0.20f, baseWidth * 0.15f, baseWidth * 0.15f, baseWidth * 0.15f),
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.5f
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_issue_rice)

        // Initialize Views
        spinnerShops = findViewById(R.id.spinnerShops)
        spinnerLoadedRiceBags = findViewById(R.id.spinnerLoadedRiceBags)
        editTextLoadQuantity = findViewById(R.id.editTextLoadQuantity)
        buttonAddToCart = findViewById(R.id.buttonAddToCart)
        recyclerViewCart = findViewById(R.id.recyclerViewCart)
        buttonIssueAll = findViewById(R.id.buttonIssueAll)
        radioGroupPaymentMethod = findViewById(R.id.radioGroupPaymentMethod)
        radioCash = findViewById(R.id.radioCash)
        radioCheque = findViewById(R.id.radioCheque)
        radioCredit = findViewById(R.id.radioCredit)
        layoutChequeDetails = findViewById(R.id.layoutChequeDetails)
        layoutCreditDetails = findViewById(R.id.layoutCreditDetails)
        editTextChequeNumber = findViewById(R.id.editTextChequeNumber)
        editTextBankName = findViewById(R.id.editTextBankName)
        editTextCreditTerm = findViewById(R.id.editTextCreditTerm)
        editTextAdvancePaid = findViewById(R.id.editTextAdvancePaid)
        textViewShopDue = findViewById(R.id.textViewShopDue)
        textViewAvailableStock = findViewById(R.id.textViewAvailableStock)
        textViewCartTotal = findViewById(R.id.textViewCartTotal)

        printerHelper = PrinterHelper(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up RecyclerView
        cartAdapter = CartAdapter(
            this,
            cartItems,
            onQuantityChange = { position, newQuantity ->
                cartItems[position].quantity = newQuantity
                cartAdapter.notifyItemChanged(position)
                updateCartTotal()
            },
            onRemoveItem = { position ->
                cartItems.removeAt(position)
                cartAdapter.notifyItemRemoved(position)
                updateCartTotal()
            }
        )
        recyclerViewCart.layoutManager = LinearLayoutManager(this)
        recyclerViewCart.adapter = cartAdapter

        // Payment Method Changes
        radioGroupPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioCash -> {
                    layoutChequeDetails.visibility = LinearLayout.GONE
                    layoutCreditDetails.visibility = LinearLayout.GONE
                }
                R.id.radioCheque -> {
                    layoutChequeDetails.visibility = LinearLayout.VISIBLE
                    layoutCreditDetails.visibility = LinearLayout.GONE
                }
                R.id.radioCredit -> {
                    layoutChequeDetails.visibility = LinearLayout.GONE
                    layoutCreditDetails.visibility = LinearLayout.VISIBLE
                }
            }
        }

        // Fetch Shops & Loaded Rice Bags
        fetchShops()
        fetchLoadedRiceBags()

        // Shop Selection Listener
        spinnerShops.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedShop = shopList[position]
                fetchShopDue(selectedShop.id)
            }
        }

        // Rice Bag Selection Listener
        spinnerLoadedRiceBags.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedString = spinnerLoadedRiceBags.selectedItem as String
                val selectedBag = loadedRiceBagsList.find { "${it.name} - ${it.size}" == selectedString }
                if (selectedBag != null) {
                    textViewAvailableStock.text = "Available Stock: ${selectedBag.stock}"
                }
            }
        }

        // Add to Cart Button Listener
        buttonAddToCart.setOnClickListener {
            addToCart()
        }

        // Issue All Button Listener
        buttonIssueAll.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showIssueConfirmationDialogWithBreakdown()
        }

        setupLoadedLorriesListener()
    }

    private fun fetchShops() {
        db.collection("shops").get()
            .addOnSuccessListener { documents ->
                shopList.clear()
                val shopNames = mutableListOf<String>()
                for (document in documents) {
                    val shop = document.toObject(Shop::class.java)
                    shop.id = document.id
                    shopList.add(shop)
                    shopNames.add(shop.name)
                }
                val shopAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, shopNames)
                shopAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerShops.adapter = shopAdapterSpinner
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching shops: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchLoadedRiceBags() {
        db.collection("loaded_lorries").get()
            .addOnSuccessListener { documents ->
                loadedRiceBagsList.clear()
                val names = mutableListOf<String>()
                for (doc in documents) {
                    val bag = doc.toObject(RiceBag::class.java)
                    bag.id = doc.id
                    loadedRiceBagsList.add(bag)
                    names.add("${bag.name} - ${bag.size}")
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerLoadedRiceBags.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching loaded rice bags: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchShopDue(shopId: String) {
        db.collection("credits")
            .whereEqualTo("shopId", shopId)
            .whereEqualTo("paid", false)
            .get()
            .addOnSuccessListener { docs ->
                var totalDue = 0.0
                for (doc in docs) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val advancePaid = doc.getDouble("advancePaid") ?: 0.0
                    totalDue += (amount - advancePaid)
                }
                textViewShopDue.text = "Outstanding Due: Rs ${"%.2f".format(totalDue)}"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching shop due: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addToCart() {
        val selectedShopName = spinnerShops.selectedItem as? String ?: return
        val selectedRiceBagString = spinnerLoadedRiceBags.selectedItem as? String ?: return
        val quantityStr = editTextLoadQuantity.text.toString().trim()

        if (TextUtils.isEmpty(quantityStr)) {
            editTextLoadQuantity.error = "Enter quantity"
            editTextLoadQuantity.requestFocus()
            return
        }

        val quantity = quantityStr.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            editTextLoadQuantity.error = "Invalid quantity"
            editTextLoadQuantity.requestFocus()
            return
        }

        val selectedRiceBag = loadedRiceBagsList.find { "${it.name} - ${it.size}" == selectedRiceBagString }
        if (selectedRiceBag == null) {
            Toast.makeText(this, "Selected rice bag not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRiceBag.stock < quantity) {
            Toast.makeText(this, "Insufficient stock available", Toast.LENGTH_SHORT).show()
            return
        }

        val existingCartItem = cartItems.find { it.riceBagId == selectedRiceBag.id }
        if (existingCartItem != null) {
            if (selectedRiceBag.stock >= existingCartItem.quantity + quantity) {
                existingCartItem.quantity += quantity
                cartAdapter.notifyItemChanged(cartItems.indexOf(existingCartItem))
                Toast.makeText(this, "Updated quantity in cart", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Insufficient stock to add more", Toast.LENGTH_SHORT).show()
            }
        } else {
            val cartItem = CartItem(
                riceBagId = selectedRiceBag.id,
                size = selectedRiceBag.size,
                price = selectedRiceBag.price,
                quantity = quantity,
                name = selectedRiceBag.name
            )
            cartItems.add(cartItem)
            cartAdapter.notifyItemInserted(cartItems.size - 1)
            Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
        }

        editTextLoadQuantity.text.clear()
        updateCartTotal()
    }

    private fun updateCartTotal() {
        var subtotal = 0.0
        for (item in cartItems) {
            subtotal += (item.price * item.quantity)
        }
        textViewCartTotal.text = "Total: Rs ${"%.2f".format(subtotal)}"
    }

    private fun showIssueConfirmationDialogWithBreakdown() {
        val subtotal = cartItems.fold(0.0) { acc, item -> acc + (item.price * item.quantity) }
        val discount = 50.0
        val taxRate = 0.05
        val taxAmount = (subtotal - discount) * taxRate
        val transportFee = 100.0
        val grandTotal = (subtotal - discount) + taxAmount + transportFee

        val sb = StringBuilder("Review your invoice:\n\n")
        for (item in cartItems) {
            val lineTotal = item.price * item.quantity
            sb.append("${item.name} - ${item.size}: ${item.quantity} x Rs ${item.price} = Rs $lineTotal\n")
        }
        sb.append("\nSubtotal: Rs ${"%.2f".format(subtotal)}")
        sb.append("\nDiscount: Rs ${"%.2f".format(discount)}")
        sb.append("\nTax (5%): Rs ${"%.2f".format(taxAmount)}")
        sb.append("\nTransport Fee: Rs ${"%.2f".format(transportFee)}")
        sb.append("\n--------------------------------\n")
        sb.append("Grand Total: Rs ${"%.2f".format(grandTotal)}\n\nProceed?")

        AlertDialog.Builder(this)
            .setTitle("Confirm Issue")
            .setMessage(sb.toString())
            .setPositiveButton("Yes") { _, _ -> issueAllItemsConfirmed(subtotal, discount, taxAmount, transportFee) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun issueAllItemsConfirmed(subtotal: Double, discount: Double, taxAmount: Double, transportFee: Double) {
        buttonIssueAll.isEnabled = false

        val selectedShopName = spinnerShops.selectedItem as String
        val selectedShop = shopList.find { it.name == selectedShopName }!!

        val selectedPaymentMethodId = radioGroupPaymentMethod.checkedRadioButtonId
        val paymentMethod = when (selectedPaymentMethodId) {
            R.id.radioCash -> "Cash"
            R.id.radioCheque -> "Cheque"
            R.id.radioCredit -> "Credit"
            else -> "Cash"
        }

        var chequeNumber: String? = null
        var bankName: String? = null
        var creditTermDays: Int? = null
        var advancePaid = 0.0

        if (paymentMethod == "Cheque") {
            chequeNumber = editTextChequeNumber.text.toString().trim()
            bankName = editTextBankName.text.toString().trim()
            if (chequeNumber.isEmpty() || bankName.isEmpty()) {
                Toast.makeText(this, "Please enter cheque details", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
                return
            }
        } else if (paymentMethod == "Credit") {
            val creditTermStr = editTextCreditTerm.text.toString().trim()
            if (creditTermStr.isEmpty()) {
                Toast.makeText(this, "Enter credit term days", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
                return
            }
            creditTermDays = creditTermStr.toIntOrNull()
            if (creditTermDays == null || creditTermDays <= 0) {
                Toast.makeText(this, "Invalid credit term days", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
                return
            }
            val advanceStr = editTextAdvancePaid.text.toString().trim()
            if (advanceStr.isNotEmpty()) {
                advancePaid = advanceStr.toDoubleOrNull() ?: 0.0
            }
        }

        val grandTotal = (subtotal - discount) + taxAmount + transportFee

        val batch = db.batch()
        val orderList = mutableListOf<Order>()
        var totalAmount = 0.0

        for (item in cartItems) {
            val riceBag = loadedRiceBagsList.find { it.id == item.riceBagId } ?: continue
            if (riceBag.stock < item.quantity) {
                Toast.makeText(this, "Insufficient stock for ${item.size}", Toast.LENGTH_SHORT).show()
                continue
            }
            val loadedLorryDocRef = db.collection("loaded_lorries").document(riceBag.id)
            batch.update(loadedLorryDocRef, "stock", riceBag.stock - item.quantity)

            val orderRef = db.collection("orders").document()
            val order = Order(
                id = orderRef.id,
                shopId = selectedShop.id,
                riceBagId = riceBag.id,
                riceName = riceBag.name,
                size = riceBag.size,
                price = item.price,
                quantity = item.quantity,
                totalPrice = item.price * item.quantity,
                deliveryStatus = "Delivered",
                orderDate = Timestamp(Date())
            )
            orderList.add(order)
            batch.set(orderRef, order.toMap())

            totalAmount += order.totalPrice
        }

        batch.commit()
            .addOnSuccessListener {
                createBill(orderList, grandTotal, selectedShop, paymentMethod, chequeNumber, bankName, creditTermDays, advancePaid)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error issuing rice bags: ${e.message}", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
            }
    }

    private fun createBill(orderList: List<Order>, grandTotal: Double, shop: Shop, paymentMethod: String,
                           chequeNumber: String? = null, bankName: String? = null, creditTermDays: Int? = null, advancePaid: Double = 0.0) {
        val billRef = db.collection("bills").document()
        val bill = Bill(
            id = billRef.id,
            orderIds = orderList.map { it.id },
            amount = grandTotal,
            billDate = Timestamp(Date()),
            paymentMethod = paymentMethod
        )

        val billMap = bill.toMap()
        billRef.set(billMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Bill generated", Toast.LENGTH_SHORT).show()
                if (paymentMethod == "Credit" && creditTermDays != null) {
                    recordCredit(shop.id, bill.id, grandTotal, creditTermDays, advancePaid)
                }
                fetchAndHandleBill(billRef.id, shop, orderList)
                cartItems.clear()
                cartAdapter.notifyDataSetChanged()
                updateCartTotal()
                buttonIssueAll.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating bill: ${e.message}", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
            }
    }

    private fun recordCredit(shopId: String, billId: String, amount: Double, creditTermDays: Int, advancePaid: Double) {
        val dueDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, creditTermDays)
        }.time

        val creditRef = db.collection("credits").document()
        val creditData = mapOf(
            "billId" to billId,
            "shopId" to shopId,
            "amount" to amount,
            "dueDate" to Timestamp(dueDate),
            "advancePaid" to advancePaid,
            "paid" to false
        )
        creditRef.set(creditData)
            .addOnSuccessListener {
                Log.d(TAG, "Credit recorded successfully.")
            }
            .addOnFailureListener {
                Log.e(TAG, "Error recording credit: ${it.message}")
            }
    }

    private fun fetchAndHandleBill(billId: String, shop: Shop, orderList: List<Order>) {
        db.collection("bills").document(billId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val bill = document.toObject(Bill::class.java)
                    bill?.let {
                        showPrintOrSendDialog(shop, orderList, it)
                    }
                } else {
                    Toast.makeText(this, "Bill not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching bill: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPrintOrSendDialog(shop: Shop, orderList: List<Order>, bill: Bill) {
        val options = arrayOf("Print Bill", "Send via WhatsApp", "Export as PDF")
        AlertDialog.Builder(this)
            .setTitle("Choose Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> printerHelper.printBill(shop, orderList, bill)
                    1 -> CoroutineScope(Dispatchers.Main).launch {
                        val summary = generateBillSummary(shop, orderList, bill)
                        sendBillViaWhatsApp(shop, orderList, bill, summary)
                    }
                    2 -> CoroutineScope(Dispatchers.Main).launch {
                        exportBillToPdf(shop, orderList, bill)
                    }
                }
            }
            .show()
    }

    private fun sendBillViaWhatsApp(shop: Shop, orderList: List<Order>, bill: Bill, summary: String) {
        if (isWhatsAppInstalled()) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, summary)
                type = "text/plain"
                setPackage("com.whatsapp")
            }
            try {
                startActivity(sendIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error sending via WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateBillSummary(shop: Shop, orderList: List<Order>, bill: Bill): String {
        val metrics = ResponsiveMetrics.calculate(this)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val billDate = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

        val sb = StringBuilder()
        val headerText = "SAGARA RICE MILL - INVOICE"
        val headerPadding = maxOf((metrics.maxWidth - headerText.length - 4) / 2, 0)
        sb.append("╔${"═".repeat(headerPadding)}$headerText${"═".repeat(headerPadding + if (metrics.maxWidth % 2 == 0) 0 else 1)}╗\n")
        sb.append("║${" ".repeat(metrics.maxWidth - 2)}║\n")
        sb.append("╚${"═".repeat(metrics.maxWidth - 2)}╝\n\n")

        val billLine = "Bill: ${bill.id.take(metrics.maxWidth - 20)}  Date: $billDate"
        sb.append(billLine.take(metrics.maxWidth) + " ".repeat(maxOf(0, metrics.maxWidth - billLine.length)) + "\n")
        sb.append("─".repeat(metrics.maxWidth) + "\n")

        sb.append("Shop: ${shop.name.take(metrics.maxWidth - 6)}\n")
        sb.append("Addr: ${shop.address.take(metrics.maxWidth - 6)}\n\n")

        val headers = arrayOf("Desc", "Size", "Qty", "Rate", "Amt")
        sb.append("┌${"─".repeat(metrics.colWidths[0].toInt())}┬${"─".repeat(metrics.colWidths[1].toInt())}┬${"─".repeat(metrics.colWidths[2].toInt())}┬${"─".repeat(metrics.colWidths[3].toInt())}┬${"─".repeat(metrics.colWidths[4].toInt())}┐\n")
        sb.append("│${headers[0].padEnd(metrics.colWidths[0].toInt())}│${headers[1].padEnd(metrics.colWidths[1].toInt())}│${headers[2].padEnd(metrics.colWidths[2].toInt())}│${headers[3].padEnd(metrics.colWidths[3].toInt())}│${headers[4].padEnd(metrics.colWidths[4].toInt())}│\n")
        sb.append("├${"─".repeat(metrics.colWidths[0].toInt())}┼${"─".repeat(metrics.colWidths[1].toInt())}┼${"─".repeat(metrics.colWidths[2].toInt())}┼${"─".repeat(metrics.colWidths[3].toInt())}┼${"─".repeat(metrics.colWidths[4].toInt())}┤\n")

        for (order in orderList) {
            val riceBag = loadedRiceBagsList.find { it.id == order.riceBagId }
            val riceBagName = riceBag?.name ?: "Unknown"
            val desc = riceBagName.take(metrics.colWidths[0].toInt() - 1).padEnd(metrics.colWidths[0].toInt())
            val size = order.size.take(metrics.colWidths[1].toInt() - 1).padEnd(metrics.colWidths[1].toInt())
            val qty = order.quantity.toString().padEnd(metrics.colWidths[2].toInt())
            val rate = String.format("%.2f", order.price).take(metrics.colWidths[3].toInt() - 1).padEnd(metrics.colWidths[3].toInt())
            val amount = String.format("%.2f", order.totalPrice).take(metrics.colWidths[4].toInt() - 1).padEnd(metrics.colWidths[4].toInt())
            sb.append("│$desc│$size│$qty│$rate│$amount│\n")
        }
        sb.append("└${"─".repeat(metrics.colWidths[0].toInt())}┴${"─".repeat(metrics.colWidths[1].toInt())}┴${"─".repeat(metrics.colWidths[2].toInt())}┴${"─".repeat(metrics.colWidths[3].toInt())}┴${"─".repeat(metrics.colWidths[4].toInt())}┘\n\n")

        val subtotal = orderList.sumOf { it.totalPrice }
        val discount = 50.0
        val taxRate = 0.05
        val taxAmount = (subtotal - discount) * taxRate
        val transportFee = 100.0

        val labelWidth = (metrics.maxWidth * 0.7).toInt()
        val valueWidth = metrics.maxWidth - labelWidth
        sb.append("Pay: ${bill.paymentMethod.take(metrics.maxWidth - 5)}\n")
        sb.append("${"Sub:".padEnd(labelWidth)}${"Rs %.2f".format(subtotal).padEnd(valueWidth)}\n")
        sb.append("${"Disc:".padEnd(labelWidth)}${"Rs %.2f".format(discount).padEnd(valueWidth)}\n")
        sb.append("${"Tax:".padEnd(labelWidth)}${"Rs %.2f".format(taxAmount).padEnd(valueWidth)}\n")
        sb.append("${"Trans:".padEnd(labelWidth)}${"Rs %.2f".format(transportFee).padEnd(valueWidth)}\n")
        sb.append("─".repeat(metrics.maxWidth) + "\n")
        sb.append("${"TOTAL:".padEnd(labelWidth)}${"Rs %.2f".format(bill.amount).padEnd(valueWidth)}\n")
        sb.append("─".repeat(metrics.maxWidth) + "\n\n")

        sb.append("Status: ${if (orderList.all { it.deliveryStatus == "Delivered" }) "Delivered" else "Partial"}\n")
        sb.append("Thank you!\n")

        return sb.toString()
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
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            PdfWriter.getInstance(document, FileOutputStream(file))

            document.open()

            // Add title
            val title = com.itextpdf.text.Paragraph("SAGARA RICE MILL - INVOICE")
            title.alignment = com.itextpdf.text.Element.ALIGN_CENTER
            title.font = com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16f, com.itextpdf.text.Font.BOLD)
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
                val riceBag = loadedRiceBagsList.find { it.id == order.riceBagId }
                table.addCell(riceBag?.name ?: "Unknown")
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

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun setupLoadedLorriesListener() {
        db.collection("loaded_lorries")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed: ", error)
                    Toast.makeText(this, "Error loading lorries.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                loadedRiceBagsList.clear()
                for (document in snapshots!!) {
                    val bag = document.toObject(RiceBag::class.java)
                    bag.id = document.id
                    loadedRiceBagsList.add(bag)
                }
                val names = loadedRiceBagsList.map { "${it.name} - ${it.size}" }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerLoadedRiceBags.adapter = adapter

                Log.d(TAG, "Loaded lorries updated in real-time.")
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed() // Navigate back when arrow is clicked
        return true
    }
}