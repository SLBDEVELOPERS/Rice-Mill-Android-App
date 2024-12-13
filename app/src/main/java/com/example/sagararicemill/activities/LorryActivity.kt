package com.example.sagararicemill.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.RiceBagAdapter
import com.example.sagararicemill.models.RiceBag
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.Exception

class LorryActivity : AppCompatActivity() {

    private val TAG = "LorryActivity"

    private lateinit var spinnerRiceBags: Spinner
    private lateinit var editTextQuantity: EditText
    private lateinit var buttonLoad: Button
    private lateinit var buttonRestock: Button
    private lateinit var listViewLoadedBags: ListView
    private lateinit var textViewAvailableStock: TextView

    private val db = FirebaseFirestore.getInstance()
    private val loadedLorryRef = db.collection("loaded_lorries")
    private val riceBagsRef = db.collection("rice_bags")

    private val riceBagList = mutableListOf<RiceBag>()
    private val loadedRiceBags = mutableListOf<RiceBag>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var loadedAdapter: RiceBagAdapter

    private var selectedRiceBag: RiceBag? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lorry)

        // Initialize UI components
        spinnerRiceBags = findViewById(R.id.spinnerRiceBags)
        editTextQuantity = findViewById(R.id.editTextLoadQuantity)
        buttonLoad = findViewById(R.id.buttonLoad)
        buttonRestock = findViewById(R.id.buttonRestock)
        listViewLoadedBags = findViewById(R.id.listViewLoadedBags)
        textViewAvailableStock = findViewById(R.id.textViewAvailableStock)

        // Initialize Adapters
        loadedAdapter = RiceBagAdapter(this, loadedRiceBags)
        listViewLoadedBags.adapter = loadedAdapter

        // Fetch Rice Bags from Firestore
        fetchRiceBags()

        setupLoadedLorriesListener()

        // Spinner selection listener
        spinnerRiceBags.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRiceBag = null
                textViewAvailableStock.text = "Available Stock: N/A"
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in riceBagList.indices) {
                    selectedRiceBag = riceBagList[position]
                    textViewAvailableStock.text = "Available Stock: ${selectedRiceBag!!.stock}"
                }
            }
        }

        // Load Button Listener
        buttonLoad.setOnClickListener {
            loadRiceBag()
        }

        // Restock Button Listener
        buttonRestock.setOnClickListener {
            restockLeftoverRiceBags()
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
                spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayNames)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRiceBags.adapter = spinnerAdapter
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
            loadedAdapter.notifyDataSetChanged()
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            editTextQuantity.text.clear()
            // Update the displayed stock
            selectedRiceBag!!.stock -= quantity
            textViewAvailableStock.text = "Available Stock: ${selectedRiceBag!!.stock}"
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
        }.addOnSuccessListener {
            Toast.makeText(this, "Leftover rice bags restocked successfully!", Toast.LENGTH_SHORT).show()
            loadedRiceBags.clear()
            loadedAdapter.notifyDataSetChanged()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error restocking leftover rice bags: ", exception)
            Toast.makeText(this, "Error restocking rice bags: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLoadedLorriesListener() {
        db.collection("loaded_lorries")
            .addSnapshotListener { snapshots, error ->
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
                }
                loadedAdapter.notifyDataSetChanged()
            }
    }

}
