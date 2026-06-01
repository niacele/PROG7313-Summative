package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mybudgetbuddysummative.Envelope
import com.example.mybudgetbuddysummative.R
import com.example.mybudgetbuddysummative.Settings
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_navigation)

        bottomNav = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GetStarted())
                .commit()

            bottomNav.visibility = View.GONE
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(Home())
                    true
                }
                R.id.nav_expense -> {
                    showFragment(Expense())
                    true
                }
                R.id.nav_envelope -> {
                    showFragment(Envelope())
                    true
                }
                R.id.nav_settings -> {
                    showFragment(Settings())
                    true
                }
                else -> false
            }
        }

        // Listen for fragment changes to hide/show nav bar automatically
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is GetStarted || currentFragment is Login || currentFragment is Register) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
