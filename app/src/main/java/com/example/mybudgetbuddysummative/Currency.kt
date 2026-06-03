package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Currency : Fragment() {
    private lateinit var rgCurrencySelector: RadioGroup
    private lateinit var btnSaveCurrency: MaterialButton
    private lateinit var btnBackButton: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"
    private lateinit var dbRef: com.google.firebase.database.DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_currency, container, false)

        rgCurrencySelector = view.findViewById(R.id.rgCurrencySelector)
        btnSaveCurrency = view.findViewById(R.id.btnSaveCurrency)
        btnBackButton = view.findViewById(R.id.btnBackButton)

        btnSaveCurrency.setOnClickListener { saveCurrencySelection() }
        btnBackButton.setOnClickListener { parentFragmentManager.popBackStack() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is MainActivity) {
            dbRef = (activity as MainActivity).envelopesRef.root.child("users")
        } else {
            dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")
        }

        when (UserSession.currency) {
            "ZAR" -> rgCurrencySelector.check(R.id.rbCurrencyZar)
            "USD" -> rgCurrencySelector.check(R.id.rbCurrencyUsd)
            "GBP" -> rgCurrencySelector.check(R.id.rbCurrencyGbp)
            else -> rgCurrencySelector.check(R.id.rbCurrencyZar)
        }
    }

    private fun saveCurrencySelection() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

        btnSaveCurrency.isEnabled = false

        // Check subscription configuration properties first to enforce premium locks
        dbRef.child(userId).child("subscribed").get().addOnSuccessListener { snapshot ->
            btnSaveCurrency.isEnabled = true
            val isSubscribed = snapshot.getValue(Boolean::class.java) ?: false

            if (!isSubscribed) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Premium Feature Locked")
                    .setMessage("Currency conversion is a Premium feature. Upgrade now to manage your money in multiple currencies!")
                    .setPositiveButton("See Plans") { _, _ ->
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, SubscriptionTwo())
                            .addToBackStack(null)
                            .commit()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@addOnSuccessListener
            }

            val selectedCurrency = when (rgCurrencySelector.checkedRadioButtonId) {
                R.id.rbCurrencyZar -> "ZAR"
                R.id.rbCurrencyUsd -> "USD"
                R.id.rbCurrencyGbp -> "GBP"
                else -> "ZAR"
            }

            // Overwrite node path value on the database console trees cleanly
            dbRef.child(userId).child("currency").setValue(selectedCurrency)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Currency updated to $selectedCurrency", Toast.LENGTH_SHORT).show()

                    UserSession.currency = selectedCurrency

                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            btnSaveCurrency.isEnabled = true
            Log.e("CurrencyDebug", "Failed to confirm subscription metadata: ${e.message}")
        }
    }
}
