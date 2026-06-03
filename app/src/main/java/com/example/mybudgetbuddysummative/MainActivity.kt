package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    lateinit var bottomNav: BottomNavigationView
    lateinit var envelopesRef: DatabaseReference
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"

            val databaseInstance = FirebaseDatabase.getInstance(databaseUrl)

            databaseInstance.setPersistenceEnabled(false)

            envelopesRef = databaseInstance.getReference("envelopes")
            Log.d("MainActivity", "Firebase database initialized cleanly in MainActivity.")

        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed in MainActivity, falling back.", e)
            envelopesRef = FirebaseDatabase.getInstance().getReference("envelopes")
        }
        supportActionBar?.hide()

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
                R.id.nav_home -> {
                    showFragment(Home()); true
                }
                R.id.nav_expense -> {
                    showFragment(Expense()); true
                }
                R.id.nav_envelope -> {
                    showFragment(Envelope()); true
                }
                R.id.nav_settings -> {
                    showFragment(Settings()); true
                }
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
            .commit()
    }
}