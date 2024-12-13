package com.example.sagararicemill.activities

import android.os.Bundle
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sagararicemill.R
import com.example.sagararicemill.models.RiceBag

class AddEditRiceBagActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextSize: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextStock: EditText
    private lateinit var buttonSave: Button

    private val db = FirebaseFirestore.getInstance()
    private var riceBagId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_rice_bag)

        editTextName = findViewById(R.id.editTextName)
        editTextSize = findViewById(R.id.editTextSize)
        editTextPrice = findViewById(R.id.editTextPrice)
        editTextStock = findViewById(R.id.editTextStock)
        buttonSave = findViewById(R.id.buttonSave)

        // Check if editing
        riceBagId = intent.getStringExtra("riceBagId")
        if (!TextUtils.isEmpty(riceBagId)) {
            // Fetch rice bag details and populate fields
            db.collection("rice_bags").document(riceBagId!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val riceBag = document.toObject(RiceBag::class.java)
                        riceBag?.let {
                            editTextName.setText(it.name)
                            editTextSize.setText(it.size)
                            editTextPrice.setText(it.price.toString())
                            editTextStock.setText(it.stock.toString())
                        }
                    } else {
                        Toast.makeText(this, "Rice Bag not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        buttonSave.setOnClickListener {
            saveRiceBag()
        }
    }

    private fun saveRiceBag() {
        val name = editTextName.text.toString().trim()
        val size = editTextSize.text.toString().trim()
        val priceStr = editTextPrice.text.toString().trim()
        val stockStr = editTextStock.text.toString().trim()

        if (TextUtils.isEmpty(name)) {
            editTextName.error = "Name is required"
            editTextName.requestFocus()
            return
        }

        if (TextUtils.isEmpty(size)) {
            editTextSize.error = "Size is required"
            editTextSize.requestFocus()
            return
        }

        if (TextUtils.isEmpty(priceStr)) {
            editTextPrice.error = "Price is required"
            editTextPrice.requestFocus()
            return
        }

        if (TextUtils.isEmpty(stockStr)) {
            editTextStock.error = "Stock is required"
            editTextStock.requestFocus()
            return
        }

        val price = priceStr.toDoubleOrNull()
        val stock = stockStr.toIntOrNull()

        if (price == null) {
            editTextPrice.error = "Invalid price"
            editTextPrice.requestFocus()
            return
        }

        if (stock == null) {
            editTextStock.error = "Invalid stock quantity"
            editTextStock.requestFocus()
            return
        }

        val riceBag = RiceBag(
            id = riceBagId ?: "",
            name = name,
            size = size,
            price = price,
            stock = stock
        )

        if (riceBagId.isNullOrEmpty()) {
            // Add new rice bag
            db.collection("rice_bags")
                .add(riceBag.toMap())
                .addOnSuccessListener {
                    Toast.makeText(this, "Rice Bag added", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding rice bag: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Update existing rice bag
            db.collection("rice_bags").document(riceBagId!!)
                .set(riceBag.toMap())
                .addOnSuccessListener {
                    Toast.makeText(this, "Rice Bag updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating rice bag: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

