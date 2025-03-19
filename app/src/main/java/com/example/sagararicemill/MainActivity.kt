package com.example.sagararicemill

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import androidx.fragment.app.Fragment
import com.example.sagararicemill.fragment.DashboardFragment
import com.example.sagararicemill.fragment.HomeFragment
import com.example.sagararicemill.fragment.OutstandingBillsFragment

import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    // Initialize Fragments
    private val dashboardFragment = DashboardFragment()
    private val homeFragment = HomeFragment()
    private val outstandingBillsFragment = OutstandingBillsFragment()
    // private val settingsFragment = SettingsFragment() // Optional

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set Dashboard as default
        loadFragment(dashboardFragment)

        // Handle Bottom Navigation Item Selection
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(dashboardFragment)
                    true
                }
                R.id.navigation_home -> {
                    loadFragment(homeFragment)
                    true
                }
//                R.id.navigation_issue_rice -> {
//                    loadFragment(issueRiceFragment)
//                    true
//                }
                R.id.navigation_outstanding_bills -> {
                    loadFragment(outstandingBillsFragment)
                    true
                }
                // R.id.navigation_settings -> {
                //     loadFragment(settingsFragment)
                //     true
                // }
                else -> false
            }
        }
    }

    /**
     * Loads the selected fragment into the fragment container.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, fragment)
            .commit()
    }
}
