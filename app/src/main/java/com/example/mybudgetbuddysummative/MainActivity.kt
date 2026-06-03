package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide() // prevent double bar

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.itemIconTintList = null

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GetStarted())
                .commit()
            bottomNav.visibility = View.GONE
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { showFragment(Home()); true }
                R.id.nav_expense -> { showFragment(Expense()); true }
                R.id.nav_envelope -> { showFragment(Envelope()); true }
                R.id.nav_settings -> { showFragment(Settings()); true }
                else -> false
            }
        }
    }

    fun enableBottomNav() {
        bottomNav.visibility = View.VISIBLE
        bottomNav.selectedItemId = R.id.nav_home
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, Home())
            .commit()
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit() // no back stack for nav bar clicks
    }
}