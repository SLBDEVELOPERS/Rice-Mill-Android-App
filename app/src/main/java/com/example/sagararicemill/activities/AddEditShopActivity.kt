package com.example.sagararicemill.activities

import android.os.Bundle
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sagararicemill.R
import com.example.sagararicemill.models.Shop

class AddEditShopActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextContact: EditText
    private lateinit var buttonSave: Button

    private val db = FirebaseFirestore.getInstance()
    private var shopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_shop)

        editTextName = findViewById(R.id.editTextShopName)
        editTextAddress = findViewById(R.id.editTextShopAddress)
        editTextContact = findViewById(R.id.editTextShopContact)
        buttonSave = findViewById(R.id.buttonSaveShop)

        // Check if editing
        shopId = intent.getStringExtra("shopId")
        if (!TextUtils.isEmpty(shopId)) {
            // Fetch shop details and populate fields
            db.collection("shops").document(shopId!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val shop = document.toObject(Shop::class.java)
                        shop?.let {
                            editTextName.setText(it.name)
                            editTextAddress.setText(it.address)
                            editTextContact.setText(it.contact)
                        }
                    } else {
                        Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        buttonSave.setOnClickListener {
            saveShop()
        }
    }

    private fun saveShop() {
        val name = editTextName.text.toString().trim()
        val address = editTextAddress.text.toString().trim()
        val contact = editTextContact.text.toString().trim()

        if (TextUtils.isEmpty(name)) {
            editTextName.error = "Name is required"
            editTextName.requestFocus()
            return
        }

        if (TextUtils.isEmpty(address)) {
            editTextAddress.error = "Address is required"
            editTextAddress.requestFocus()
            return
        }

        if (TextUtils.isEmpty(contact)) {
            editTextContact.error = "Contact is required"
            editTextContact.requestFocus()
            return
        }

        if (shopId.isNullOrEmpty()) {
            // Add new shop
            val newShop = Shop(
                name = name,
                address = address,
                contact = contact
            )

            db.collection("shops")
                .add(newShop.toMap())
                .addOnSuccessListener {
                    Toast.makeText(this, "Shop added", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding shop: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Update existing shop
            val updatedShop = Shop(
                id = shopId!!,
                name = name,
                address = address,
                contact = contact
            )

            db.collection("shops").document(shopId!!)
                .set(updatedShop.toMap())
                .addOnSuccessListener {
                    Toast.makeText(this, "Shop updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating shop: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
