package com.example.sagararicemill.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.CartAdapter
import com.example.sagararicemill.models.Bill
import com.example.sagararicemill.models.CartItem
import com.example.sagararicemill.models.Order
import com.example.sagararicemill.models.PaymentDetails
import com.example.sagararicemill.models.RiceBag
import com.example.sagararicemill.models.Shop
import com.example.sagararicemill.utils.PrinterHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class IssueRiceFragment : Fragment() {

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

    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()

    private val db = FirebaseFirestore.getInstance()
    private val shopList = mutableListOf<Shop>() // Assuming you have a Shop data class
    private val loadedRiceBagsList = mutableListOf<RiceBag>()

    private lateinit var printerHelper: PrinterHelper

    private val TAG = "IssueRiceFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_issue_rice, container, false)

        // Initialize Views
        spinnerShops = view.findViewById(R.id.spinnerShops)
        spinnerLoadedRiceBags = view.findViewById(R.id.spinnerLoadedRiceBags)
        editTextLoadQuantity = view.findViewById(R.id.editTextLoadQuantity)
        buttonAddToCart = view.findViewById(R.id.buttonAddToCart)
        recyclerViewCart = view.findViewById(R.id.recyclerViewCart)
        buttonIssueAll = view.findViewById(R.id.buttonIssueAll)
        radioGroupPaymentMethod = view.findViewById(R.id.radioGroupPaymentMethod)
        radioCash = view.findViewById(R.id.radioCash)
        radioCheque = view.findViewById(R.id.radioCheque)
        radioCredit = view.findViewById(R.id.radioCredit)

        // Set up RecyclerView
        cartAdapter = CartAdapter(
            requireContext(),
            cartItems,
            onQuantityChange = { position, newQuantity ->
                cartItems[position].quantity = newQuantity
                cartAdapter.notifyItemChanged(position)
            }
        ) { position ->
            cartItems.removeAt(position)
            cartAdapter.notifyItemRemoved(position)
        }
        recyclerViewCart.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewCart.adapter = cartAdapter

        // Fetch Shops and Loaded Rice Bags
        fetchShops()
        fetchLoadedRiceBags()

        // Set Payment Method RadioGroup Listener
        radioGroupPaymentMethod.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioCash -> {
                    // Hide Cheque and Credit Details
                    view.findViewById<LinearLayout>(R.id.layoutChequeDetails).visibility = View.GONE
                    view.findViewById<LinearLayout>(R.id.layoutCreditDetails).visibility = View.GONE
                }
                R.id.radioCheque -> {
                    // Show Cheque Details and hide Credit Details
                    view.findViewById<LinearLayout>(R.id.layoutChequeDetails).visibility = View.VISIBLE
                    view.findViewById<LinearLayout>(R.id.layoutCreditDetails).visibility = View.GONE
                }
                R.id.radioCredit -> {
                    // Show Credit Details and hide Cheque Details
                    view.findViewById<LinearLayout>(R.id.layoutChequeDetails).visibility = View.GONE
                    view.findViewById<LinearLayout>(R.id.layoutCreditDetails).visibility = View.VISIBLE
                }
            }
        }

        // Add to Cart Button Listener
        buttonAddToCart.setOnClickListener {
            addToCart()
        }

        // Issue All Button Listener
        buttonIssueAll.setOnClickListener {
            issueAllItems()
        }

        return view
    }

    /**
     * Fetches shops from Firestore and populates the spinner.
     */
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
                // Set Spinner Adapter
                val shopAdapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, shopNames)
                shopAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerShops.adapter = shopAdapterSpinner
                Log.d(TAG, "Shops fetched successfully.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching shops: ", exception)
                Toast.makeText(requireContext(), "Error fetching shops: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fetches loaded rice bags from Firestore and populates the spinner.
     */
    private fun fetchLoadedRiceBags() {
        db.collection("loaded_lorries").get()
            .addOnSuccessListener { documents ->
                loadedRiceBagsList.clear()
                val riceBagSizes = mutableListOf<String>()
                for (document in documents) {
                    val bag = document.toObject(RiceBag::class.java)
                    bag.id = document.id
                    loadedRiceBagsList.add(bag)
                    riceBagSizes.add(bag.size)
                }
                // Set Spinner Adapter
                val riceBagAdapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, riceBagSizes)
                riceBagAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerLoadedRiceBags.adapter = riceBagAdapterSpinner
                Log.d(TAG, "Loaded lorries fetched successfully.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching loaded rice bags: ", exception)
                Toast.makeText(requireContext(), "Error fetching loaded rice bags: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Adds a selected rice bag with specified quantity to the cart.
     */
    private fun addToCart() {
        val selectedShopName = spinnerShops.selectedItem as? String
        val selectedRiceBagSize = spinnerLoadedRiceBags.selectedItem as? String
        val quantityStr = editTextLoadQuantity.text.toString().trim()

        if (selectedShopName.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a shop.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRiceBagSize.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a rice bag size.", Toast.LENGTH_SHORT).show()
            return
        }

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

        // Find the selected RiceBag
        val selectedRiceBag = loadedRiceBagsList.find { it.size == selectedRiceBagSize }
        if (selectedRiceBag == null) {
            Toast.makeText(requireContext(), "Selected rice bag not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRiceBag.stock < quantity) {
            Toast.makeText(requireContext(), "Insufficient stock available", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the rice bag is already in the cart
        val existingCartItem = cartItems.find { it.riceBagId == selectedRiceBag.id }
        if (existingCartItem != null) {
            if (selectedRiceBag.stock >= existingCartItem.quantity + quantity) {
                existingCartItem.quantity += quantity
                cartAdapter.notifyItemChanged(cartItems.indexOf(existingCartItem))
                Toast.makeText(requireContext(), "Updated quantity in cart", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Insufficient stock to add more", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Add new item to cart
            val cartItem = CartItem(
                riceBagId = selectedRiceBag.id,
                size = selectedRiceBag.size,
                price = selectedRiceBag.price,
                quantity = quantity
            )
            cartItems.add(cartItem)
            cartAdapter.notifyItemInserted(cartItems.size - 1)
            Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
        }

        // Clear the quantity input
        editTextLoadQuantity.text.clear()
    }

    /**
     * Handles issuing all items in the cart with the selected payment method.
     */
    private fun issueAllItems() {
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable the Issue All button to prevent multiple clicks
        buttonIssueAll.isEnabled = false

        // Fetch the selected shop
        val selectedShopName = spinnerShops.selectedItem as? String
        if (selectedShopName.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a shop.", Toast.LENGTH_SHORT).show()
            buttonIssueAll.isEnabled = true
            return
        }

        val selectedShop = shopList.find { it.name == selectedShopName }
        if (selectedShop == null) {
            Toast.makeText(requireContext(), "Selected shop not found", Toast.LENGTH_SHORT).show()
            buttonIssueAll.isEnabled = true
            return
        }

        // Get the selected payment method
        val selectedPaymentMethodId = radioGroupPaymentMethod.checkedRadioButtonId
        val selectedPaymentMethod = when (selectedPaymentMethodId) {
            R.id.radioCash -> "Cash"
            R.id.radioCheque -> "Cheque"
            R.id.radioCredit -> "Credit"
            else -> {
                Toast.makeText(requireContext(), "Please select a payment method.", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
                return
            }
        }

        // Capture additional payment details based on payment method
        val paymentDetails = when (selectedPaymentMethod) {
            "Cheque" -> {
                val chequeNumber = view?.findViewById<EditText>(R.id.editTextChequeNumber)?.text.toString().trim()
                val bankName = view?.findViewById<EditText>(R.id.editTextBankName)?.text.toString().trim()

                if (chequeNumber.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter cheque number.", Toast.LENGTH_SHORT).show()
                    buttonIssueAll.isEnabled = true
                    return
                }

                if (bankName.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter bank name.", Toast.LENGTH_SHORT).show()
                    buttonIssueAll.isEnabled = true
                    return
                }

                PaymentDetails(
                    chequeNumber = chequeNumber,
                    bankName = bankName
                )
            }
            "Credit" -> {
                val creditTermStr = view?.findViewById<EditText>(R.id.editTextCreditTerm)?.text.toString().trim()
                val creditTermDays = creditTermStr.toIntOrNull()

                if (creditTermDays == null || creditTermDays <= 0) {
                    Toast.makeText(requireContext(), "Enter valid credit term days.", Toast.LENGTH_SHORT).show()
                    buttonIssueAll.isEnabled = true
                    return
                }

                // Calculate due date based on credit term
                val dueDate = Calendar.getInstance()
                dueDate.add(Calendar.DAY_OF_YEAR, creditTermDays)

                PaymentDetails(
                    creditTermDays = creditTermDays,
                    // dueDate will be set in createBill()
                )
            }
            else -> null // Cash does not require additional details
        }

        // Initialize total amount
        var totalAmount = 0.0

        // Create a batch to perform atomic operations
        val batch = db.batch()

        // Lists to hold orders
        val orderList = mutableListOf<Order>()

        for (cartItem in cartItems) {
            // Find the rice bag
            val riceBag = loadedRiceBagsList.find { it.id == cartItem.riceBagId }
            if (riceBag == null) {
                Toast.makeText(requireContext(), "Rice bag ${cartItem.size} not found", Toast.LENGTH_SHORT).show()
                continue
            }

            // Check stock again before processing
            if (riceBag.stock < cartItem.quantity) {
                Toast.makeText(requireContext(), "Insufficient stock for ${cartItem.size}", Toast.LENGTH_SHORT).show()
                continue
            }

            // Update stock in loaded_lorry collection
            val loadedLorryDocRef = db.collection("loaded_lorries").document(riceBag.id)
            batch.update(loadedLorryDocRef, "stock", riceBag.stock - cartItem.quantity)

            // Create a new order
            val orderRef = db.collection("orders").document()
            val order = Order(
                id = orderRef.id,
                shopId = selectedShop.id,
                riceBagId = riceBag.id,
                size = riceBag.size,
                price = riceBag.price,
                quantity = cartItem.quantity,
                totalPrice = cartItem.price * cartItem.quantity,
                deliveryStatus = "Delivered",
                orderDate = com.google.firebase.Timestamp(Date())
            )
            orderList.add(order)
            batch.set(orderRef, order.toMap())

            // Accumulate total amount
            totalAmount += order.totalPrice
        }

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                // After successful batch operation, create a bill
                createBill(orderList, totalAmount, selectedShop, selectedPaymentMethod, paymentDetails)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error issuing rice bags: ${e.message}", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
            }
    }

    /**
     * Creates a bill in Firestore and handles post-creation actions.
     */
    private fun createBill(orderList: List<Order>, totalAmount: Double, shop: Shop, paymentMethod: String, paymentDetails: PaymentDetails?) {
        // Calculate due date for Credit payments
        var dueDateTimestamp: com.google.firebase.Timestamp? = null
        if (paymentMethod == "Credit" && paymentDetails?.creditTermDays != null) {
            val dueDate = Calendar.getInstance()
            dueDate.add(Calendar.DAY_OF_YEAR, paymentDetails.creditTermDays!!)
            dueDateTimestamp = com.google.firebase.Timestamp(dueDate.time)
        }

        // Create a bill document
        val billRef = db.collection("bills").document()
        val bill = Bill(
            id = billRef.id,
            orderIds = orderList.map { it.id },
            amount = totalAmount,
            billDate = Timestamp(Date()),
            paymentMethod = paymentMethod,
            paymentStatus = if (paymentMethod == "Cash") "Paid" else "Unpaid",
            dueDate = dueDateTimestamp,
            paymentDetails = paymentDetails
        )

        // Setting up the bill data
        val billMap = bill.toMap()

        billRef.set(billMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Rice bags issued and bill generated", Toast.LENGTH_SHORT).show()
                // Optionally, navigate to the Dashboard or show the bill
                 fetchAndHandleBill(billRef.id, shop, orderList)
                // Clear the cart
                cartItems.clear()
                cartAdapter.notifyDataSetChanged()
                // Re-enable the Issue All button
                buttonIssueAll.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error creating bill: ${e.message}", Toast.LENGTH_SHORT).show()
                buttonIssueAll.isEnabled = true
            }
    }

    /**
     * Fetches the bill and provides options to print or send via WhatsApp.
     */
    private fun fetchAndHandleBill(billId: String, shop: Shop, orderList: List<Order>) {
        db.collection("bills").document(billId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val bill = document.toObject(Bill::class.java)
                    bill?.let {
                        // Present options to the user
                        showPrintOrSendDialog(shop, orderList, it)
                    }
                } else {
                    Toast.makeText(context, "Bill not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error fetching bill: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Shows a dialog to let the user choose between printing and sending via WhatsApp.
     */
    private fun showPrintOrSendDialog(shop: Shop, orderList: List<Order>, bill: Bill) {
        val options = arrayOf("Print Bill", "Send via WhatsApp")
        val builder = context?.let { AlertDialog.Builder(it) }
        builder!!.setTitle(getString(R.string.dialog_title_choose_option))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Print the bill
                        printerHelper.printBillMultiOrder(shop, orderList, bill)
                    }
                    1 -> {
                        // Send via WhatsApp
                        sendBillViaWhatsApp(shop, orderList, bill)
                    }
                }
            }
        if (builder != null) {
            builder.create().show()
        }
    }

    /**
     * Generates a formatted bill summary and sends it via WhatsApp.
     */
    private fun sendBillViaWhatsApp(shop: Shop, orderList: List<Order>, bill: Bill) {
        // Generate the bill summary
        val billSummary = generateBillSummary(shop, orderList, bill)

        // Check if WhatsApp is installed
        if (isWhatsAppInstalled()) {
            // Create an intent to send the bill via WhatsApp
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, billSummary)
                type = "text/plain"
                setPackage("com.whatsapp")
            }

            try {
                startActivity(sendIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error sending bill via WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, getString(R.string.toast_whatsapp_not_installed), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Checks if WhatsApp is installed on the device.
     */
    private fun isWhatsAppInstalled(): Boolean {
        return try {
            //packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Generates a formatted bill summary.
     */
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
        sb.append("*🚚 Delivery Status:* ${orderList.all { it.deliveryStatus == "Delivered" }}\n\n")
        sb.append("Thank you for your business! 😊")

        return sb.toString()
    }
}
