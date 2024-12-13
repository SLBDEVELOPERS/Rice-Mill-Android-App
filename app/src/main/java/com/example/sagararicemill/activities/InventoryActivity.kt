package com.example.sagararicemill.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.RiceBagAdapter
import com.example.sagararicemill.models.RiceBag

class InventoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var addButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val riceBagsRef = db.collection("rice_bags")

    private lateinit var adapter: RiceBagAdapter
    private val riceBagList = mutableListOf<RiceBag>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        listView = findViewById(R.id.listViewInventory)
        addButton = findViewById(R.id.buttonAddRiceBag)

        adapter = RiceBagAdapter(this, riceBagList)
        listView.adapter = adapter


        riceBagsRef
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                riceBagList.clear()
                for (document in snapshots!!) {
                    val riceBag = document.toObject(RiceBag::class.java)
                    riceBag.id = document.id
                    riceBagList.add(riceBag)
                }
                adapter.notifyDataSetChanged()
            }



        // Fetch Rice Bags from Firestore
//        riceBagsRef.get()
//            .addOnSuccessListener { documents ->
//                riceBagList.clear()
//                for (document in documents) {
//                    val riceBag = document.toObject(RiceBag::class.java)
//                    riceBag.id = document.id
//                    riceBagList.add(riceBag)
//                }
//                adapter.notifyDataSetChanged()
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error getting documents: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }

        // Add Button Listener
        addButton.setOnClickListener {
            val intent = Intent(this, AddEditRiceBagActivity::class.java)
            startActivity(intent)
        }

        // ListView Item Click Listener for Editing
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedBag = riceBagList[position]
            val intent = Intent(this, AddEditRiceBagActivity::class.java)
            intent.putExtra("riceBagId", selectedBag.id)
            startActivity(intent)
        }

        // Long Click Listener for Deleting
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedBag = riceBagList[position]
            AlertDialog.Builder(this)
                .setTitle("Delete Rice Bag")
                .setMessage("Are you sure you want to delete ${selectedBag.size}?")
                .setPositiveButton("Yes") { _, _ ->
                    riceBagsRef.document(selectedBag.id).delete()
                        .addOnSuccessListener {
                            riceBagList.removeAt(position)
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("No", null)
                .show()
            true
        }
    }
}
