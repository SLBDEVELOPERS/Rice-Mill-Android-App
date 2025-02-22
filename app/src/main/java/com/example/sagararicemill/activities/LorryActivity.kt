package com.example.sagararicemill.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.LoadedRiceBagAdapter
import com.example.sagararicemill.models.RiceBag
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.Exception

class LorryActivity : AppCompatActivity() {

    private val TAG = "LorryActivity"

    private lateinit var spinnerRiceBags: Spinner
    private lateinit var editTextQuantity: EditText
    private lateinit var buttonLoad: MaterialButton
    private lateinit var buttonRestock: MaterialButton
    private lateinit var recyclerViewLoadedBags: RecyclerView
    private lateinit var textViewAvailableStock: TextView
    private lateinit var topAppBar: com.google.android.material.appbar.MaterialToolbar

    private val db = FirebaseFirestore.getInstance()
    private val loadedLorryRef = db.collection("loaded_lorries")
    private val riceBagsRef = db.collection("rice_bags")

    private val riceBagList = mutableListOf<RiceBag>()
    private val loadedRiceBags = mutableListOf<RiceBag>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var loadedAdapter: LoadedRiceBagAdapter

    private var selectedRiceBag: RiceBag? = null

    // Threshold for low stock alerts
    private val lowStockThreshold = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lorry)

        // Initialize UI components
        topAppBar = findViewById(R.id.topAppBarLorry)
        spinnerRiceBags = findViewById(R.id.spinnerRiceBags)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        buttonLoad = findViewById(R.id.buttonLoad)
        buttonRestock = findViewById(R.id.buttonRestock)
        recyclerViewLoadedBags = findViewById(R.id.recyclerViewLoadedBags)
        textViewAvailableStock = findViewById(R.id.textViewAvailableStock)

        // Setup Toolbar
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize Adapters
        loadedAdapter = LoadedRiceBagAdapter(this, loadedRiceBags)
        recyclerViewLoadedBags.layoutManager = LinearLayoutManager(this)
        recyclerViewLoadedBags.adapter = loadedAdapter

        // Setup Spinner with AutoCompleteTextView
        setupRiceBagsSpinner()

        // Fetch Rice Bags from Firestore
        //fetchRiceBags()

        // Listen to Loaded Lorries in real-time
        setupLoadedLorriesListener()

        // Load Button Listener
        buttonLoad.setOnClickListener {
            loadRiceBag()
        }

        // Restock Button Listener
        buttonRestock.setOnClickListener {
            restockLeftoverRiceBags()
        }

        // Implement Swipe to Delete for Loaded Rice Bags
        implementSwipeToDelete()
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.lorry_top_menu, menu)
//
//        // Optionally, add menu items like settings or notifications
//
//        return true
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle toolbar menu item clicks
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Handle back navigation
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Sets up the rice bags spinner with auto-complete suggestions.
     */
    private fun setupRiceBagsSpinner() {
        // Initialize the adapter for the spinner
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRiceBags.adapter = spinnerAdapter

        // Fetch rice bags from Firestore
        fetchRiceBags()

        // Listener for rice bag selection
        spinnerRiceBags.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position != AdapterView.INVALID_POSITION) {
                    selectedRiceBag = riceBagList[position]
                    textViewAvailableStock.text = "Available Stock: ${selectedRiceBag!!.stock}"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedRiceBag = null
                textViewAvailableStock.text = "Available Stock: 0"
            }
        }
    }

    /**
     * Fetches rice bags from Firestore and populates the spinner with "Name - Size".
     */
    @SuppressLint("StringFormatInvalid")
    private fun fetchRiceBags() {
        riceBagsRef.get()
            .addOnSuccessListener { documents ->
                riceBagList.clear()
                val displayNames = mutableListOf<String>()
                for (document in documents) {
                    val bag = document.toObject(RiceBag::class.java)
                    bag.id = document.id
                    riceBagList.add(bag)
                    // Show "Name - Size" for clarity
                    displayNames.add("${bag.name} - ${bag.size}")
                }
                Log.d(TAG, "fetchRiceBags: " + displayNames.size)
//                spinnerAdapter.addAll(displayNames)
//                spinnerAdapter.notifyDataSetChanged()

                spinnerAdapter.clear()
                spinnerAdapter.addAll(displayNames)
                spinnerAdapter.notifyDataSetChanged()

                // Update AutoComplete suggestions if using AutoCompleteTextView
                val names = riceBagList.map { it.name }.distinct()
                val sizes = riceBagList.map { it.size }.distinct()

                // You can set up separate adapters or use a MultiAutoCompleteTextView if needed
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching rice bags: ", exception)
                Toast.makeText(this, "Error fetching rice bags: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Handles loading of rice bags onto the lorry, checking name, size, and stock availability.
     */
    @SuppressLint("StringFormatInvalid")
    private fun loadRiceBag() {
        val quantityStr = editTextQuantity.text.toString().trim()

        // Validate if a rice bag is selected
        if (selectedRiceBag == null) {
            Toast.makeText(this, "Please select a rice bag first.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate Quantity Input
        if (TextUtils.isEmpty(quantityStr)) {
            editTextQuantity.error = "Enter quantity"
            editTextQuantity.requestFocus()
            return
        }

        val quantity = quantityStr.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            editTextQuantity.error = "Invalid quantity"
            editTextQuantity.requestFocus()
            return
        }

        // Check Stock Availability
        if (selectedRiceBag!!.stock < quantity) {
            Toast.makeText(this, "Insufficient stock available", Toast.LENGTH_SHORT).show()
            return
        }

        // Perform Firestore Transaction
        db.runTransaction { transaction ->
            val bagRef = riceBagsRef.document(selectedRiceBag!!.id)
            val freshBag = transaction.get(bagRef).toObject(RiceBag::class.java)
                ?: throw Exception("Rice bag not found in Firestore.")

            freshBag.id = bagRef.id

            Log.d(TAG, "loadRiceBag: freshBag $freshBag")

            if (freshBag.stock < quantity) {
                throw Exception("Insufficient stock for ${freshBag.name} - ${freshBag.size}. Available: ${freshBag.stock}")
            }

            val loadedBagRef = loadedLorryRef.document(freshBag.id)
            val existingLoadedLorryBag = transaction.get(loadedBagRef).toObject(RiceBag::class.java)

            // Decrement the stock in rice_bags
            val newStock = freshBag.stock - quantity
            transaction.update(bagRef, "stock", newStock)

            // Update loadedRiceBags locally
            val existingLoadedBag = loadedRiceBags.find { it.id == freshBag.id }
            if (existingLoadedBag != null) {
                existingLoadedBag.stock += quantity
            } else {
                loadedRiceBags.add(RiceBag(freshBag.id, freshBag.name, freshBag.size, freshBag.price, quantity))
            }

            if (existingLoadedLorryBag != null) {
                // Increment the stock in loaded_lorries
                val updatedLorryStock = existingLoadedLorryBag.stock + quantity
                transaction.update(loadedBagRef, "stock", updatedLorryStock)
            } else {
                // Create a new entry in loaded_lorries
                val newLorryBag = RiceBag(freshBag.id, freshBag.name, freshBag.size, freshBag.price, quantity)
                transaction.set(loadedBagRef, newLorryBag.toMap())
            }

            "Successfully loaded $quantity of ${freshBag.name} - ${freshBag.size} onto the lorry."
        }.addOnSuccessListener { message ->
            loadedAdapter.updateList(loadedRiceBags)
            Snackbar.make(findViewById(R.id.coordinatorLayoutLorry), message, Snackbar.LENGTH_SHORT).show()
            editTextQuantity.text.clear()
            // Update the displayed stock
            selectedRiceBag!!.stock -= quantity
            textViewAvailableStock.text = "Available Stock: ${selectedRiceBag!!.stock}"

            // Check for low stock after loading
            checkLowStock(selectedRiceBag!!)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error loading rice bags: ", exception)
            Toast.makeText(this, "Error loading rice bags: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Restock Leftover Rice Bags back into the inventory at the end of the day.
     */
    private fun restockLeftoverRiceBags() {
        if (loadedRiceBags.isEmpty()) {
            Toast.makeText(this, "No leftover bags to restock.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Restock Leftover Bags")
            .setMessage("Are you sure you want to restock all leftover rice bags back to inventory?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                performRestock()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performRestock() {
        db.runTransaction { transaction ->
            for (loadedBag in loadedRiceBags) {
                val inventoryRef = riceBagsRef.document(loadedBag.id)
                val existingBag = transaction.get(inventoryRef).toObject(RiceBag::class.java)

                // If rice bag already exists, increment stock
                if (existingBag != null) {
                    val updatedStock = existingBag.stock + loadedBag.stock
                    transaction.update(inventoryRef, "stock", updatedStock)
                } else {
                    // If doesn't exist, create a new one
                    transaction.set(inventoryRef, loadedBag.toMap())
                }

                // Remove from loaded_lorries
                val loadedBagRef = loadedLorryRef.document(loadedBag.id)
                transaction.delete(loadedBagRef)
            }

            "Restocked leftover rice bags to inventory."
        }.addOnSuccessListener { message ->
            Snackbar.make(findViewById(R.id.coordinatorLayoutLorry), message, Snackbar.LENGTH_SHORT).show()
            loadedRiceBags.clear()
            loadedAdapter.updateList(loadedRiceBags)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error restocking leftover rice bags: ", exception)
            Toast.makeText(this, "Error restocking rice bags: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sets up real-time listener for loaded lorries.
     */
    private fun setupLoadedLorriesListener() {
        loadedLorryRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed: ", error)
                Toast.makeText(this, "Error loading lorries: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            loadedRiceBags.clear()
            for (document in snapshots!!) {
                val bag = document.toObject(RiceBag::class.java)
                bag.id = document.id
                loadedRiceBags.add(bag)
                Log.d(TAG, "Loaded Rice Bag: ${bag.name} - ${bag.size} - ${bag.stock}")
            }

            loadedAdapter.updateList(loadedRiceBags)

            // Optionally, check for low stock after loading
            loadedRiceBags.forEach { checkLowStock(it) }
        }
    }

    /**
     * Implements swipe-to-delete functionality for loaded rice bags.
     */
    private fun implementSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Not supporting move
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val riceBag = loadedRiceBags[position]

                AlertDialog.Builder(this@LorryActivity)
                    .setTitle("Delete Loaded Rice Bag")
                    .setMessage("Are you sure you want to remove '${riceBag.name} - ${riceBag.size}' from loaded bags?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        dialog.dismiss()
                        removeLoadedRiceBag(riceBag, position)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        loadedAdapter.notifyItemChanged(position) // Restore the swiped item
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewLoadedBags)
    }

    /**
     * Removes a loaded rice bag from the lorry and updates the inventory accordingly.
     */
    private fun removeLoadedRiceBag(riceBag: RiceBag, position: Int) {
        db.runTransaction { transaction ->
            val inventoryRef = riceBagsRef.document(riceBag.id)
            val loadedBagRef = loadedLorryRef.document(riceBag.id)

            // Fetch current inventory
            val existingBag = transaction.get(inventoryRef).toObject(RiceBag::class.java)

            if (existingBag != null) {
                // Increment the stock back to inventory
                val updatedStock = existingBag.stock + riceBag.stock
                transaction.update(inventoryRef, "stock", updatedStock)
            } else {
                // If rice bag doesn't exist in inventory, create it
                transaction.set(inventoryRef, riceBag.toMap())
            }

            // Remove from loaded_lorries
            transaction.delete(loadedBagRef)

            "Removed ${riceBag.name} - ${riceBag.size} from loaded bags."
        }.addOnSuccessListener { message ->
            loadedRiceBags.removeAt(position)
            loadedAdapter.updateList(loadedRiceBags)
            Snackbar.make(findViewById(R.id.coordinatorLayoutLorry), message, Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    // Optionally, implement undo functionality
                    // This would require re-adding the rice bag
                }
                .show()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error removing loaded rice bag: ", exception)
            Toast.makeText(this, "Error removing rice bag: ${exception.message}", Toast.LENGTH_SHORT).show()
            loadedAdapter.notifyItemChanged(position) // Restore the swiped item
        }
    }

    /**
     * Checks for low stock and alerts the user if necessary.
     */
    private fun checkLowStock(riceBag: RiceBag) {
        if (riceBag.stock <= lowStockThreshold) {
            Snackbar.make(findViewById(R.id.coordinatorLayoutLorry), "Low stock for ${riceBag.name} - ${riceBag.size}", Snackbar.LENGTH_LONG)
                .setAction("View Inventory") {
                    // Optionally, navigate to InventoryActivity
                    val intent = Intent(this, InventoryActivity::class.java)
                    startActivity(intent)
                }
                .show()
        }
    }

}
