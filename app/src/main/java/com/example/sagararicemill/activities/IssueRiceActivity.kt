package com.example.sagararicemill.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.CartAdapter
import com.example.sagararicemill.models.*
import com.example.sagararicemill.utils.PrinterHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            when(checkedId) {
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
        val discount = 50.0 // Example discount
        val taxRate = 0.05 // 5% tax
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

        // Payment method details
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

        // Calculate final grand total
        val grandTotal = (subtotal - discount) + taxAmount + transportFee

        // Issue orders (similar to original code)
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
                // Create Bill with final grandTotal
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
        val options = arrayOf("Print Bill", "Send via WhatsApp")
        AlertDialog.Builder(this)
            .setTitle("Choose Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> printerHelper.printBillMultiOrder(shop, orderList, bill)
                    1 -> sendBillViaWhatsApp(shop, orderList, bill)
                }
            }
            .show()
    }

    private fun sendBillViaWhatsApp(shop: Shop, orderList: List<Order>, bill: Bill) {
        val billSummary = generateBillSummary(shop, orderList, bill)
        if (isWhatsAppInstalled()) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, billSummary)
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
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val billDate = bill.billDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

        val sb = StringBuilder()
        sb.append("🛒 *Sagara Rice Mill Bill* 🛒\n\n")
        sb.append("*📅 Date:* $billDate\n")
        sb.append("*🏪 Shop:* ${shop.name}\n")
        sb.append("*📍 Location:* ${shop.address}\n\n")
        sb.append("*🔖 Bill ID:* ${bill.id}\n")
        sb.append("*💰 Payment Method:* ${bill.paymentMethod}\n\n")
        sb.append("*📦 Items Issued:*\n")
        for (order in orderList) {
            sb.append("- *${order.size}*: ${order.quantity} x Rs ${order.price} = Rs ${order.totalPrice}\n")
        }
        sb.append("\n*💵 Total Amount:* Rs ${"%.2f".format(bill.amount)}\n")
        sb.append("*🚚 Delivery Status:* ${if (orderList.all { it.deliveryStatus == "Delivered" }) "All Delivered" else "Partial"}\n\n")
        sb.append("Thank you for your business! 😊")

        return sb.toString()
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
}
