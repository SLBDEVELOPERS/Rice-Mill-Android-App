package com.example.sagararicemill.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.sagararicemill.R

class HomeActivity : AppCompatActivity() {

    private lateinit var inventoryButton: Button
    private lateinit var lorryButton: Button
    private lateinit var shopButton: Button
    private lateinit var issueRiceButton: Button
    private lateinit var billButton: Button
    private lateinit var reportsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Buttons
//        inventoryButton = findViewById(R.id.buttonInventory)
//        lorryButton = findViewById(R.id.buttonLorry)
//        shopButton = findViewById(R.id.buttonShops)
//        issueRiceButton = findViewById(R.id.buttonIssueRice)
//        billButton = findViewById(R.id.buttonBills)
//        reportsButton = findViewById(R.id.buttonReports)
//
//        // Set OnClickListeners
//        inventoryButton.setOnClickListener {
//            val intent = Intent(this, InventoryActivity::class.java)
//            startActivity(intent)
//        }
//
//        lorryButton.setOnClickListener {
//            val intent = Intent(this, LorryActivity::class.java)
//            startActivity(intent)
//        }
//
//        shopButton.setOnClickListener {
//            val intent = Intent(this, ShopActivity::class.java)
//            startActivity(intent)
//        }
//
//        issueRiceButton.setOnClickListener {
//            val intent = Intent(this, IssueRiceActivity::class.java)
//            startActivity(intent)
//        }
//
//        billButton.setOnClickListener {
//            val intent = Intent(this, BillActivity::class.java)
//            startActivity(intent)
//        }
//
//        reportsButton.setOnClickListener {
//            val intent = Intent(this, ReportsActivity::class.java)
//            startActivity(intent)
//        }
    }
}
