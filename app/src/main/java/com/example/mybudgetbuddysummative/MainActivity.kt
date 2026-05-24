package com.example.mybudgetbuddysummative

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mybudgetbuddysummative.Envelope
import com.example.mybudgetbuddysummative.R
import com.example.mybudgetbuddysummative.Settings
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_navigation)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Home())
                .commit()
        }

        // Handle navigation item clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Home())
                        .commit()
                    true
                }
                R.id.nav_expense -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Expense())
                        .commit()
                    true
                }
                R.id.nav_envelope -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Envelope())
                        .commit()
                    true
                }
                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Settings())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
