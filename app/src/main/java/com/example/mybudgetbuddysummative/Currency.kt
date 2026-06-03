package com.example.mybudgetbuddysummative

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Currency : Fragment() {
    private lateinit var rgCurrencySelector: RadioGroup
    private lateinit var btnSaveCurrency: MaterialButton

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_currency, container, false)

        rgCurrencySelector = view.findViewById(R.id.rgCurrencySelector)
        btnSaveCurrency = view.findViewById(R.id.btnSaveCurrency)

        btnSaveCurrency.setOnClickListener { saveCurrencySelection() }

        return view
    }

    private fun saveCurrencySelection() {
        val userId = auth.currentUser?.uid ?: return

        // Check subscription status first
        dbRef.child(userId).child("subscribed").get().addOnSuccessListener { snapshot ->
            val isSubscribed = snapshot.getValue(Boolean::class.java) ?: false

            if (!isSubscribed) {
                Toast.makeText(requireContext(), "Premium required to change currency", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // Determine selected currency
            val selectedCurrency = when (rgCurrencySelector.checkedRadioButtonId) {
                R.id.rbCurrencyZar -> "ZAR"
                R.id.rbCurrencyUsd -> "USD"
                R.id.rbCurrencyGbp -> "GBP"
                else -> "ZAR"
            }

            // Save to Firebase under user profile
            dbRef.child(userId).child("currency").setValue(selectedCurrency)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Currency updated to $selectedCurrency", Toast.LENGTH_SHORT).show()
                    // Optionally update UserSession so adapters reflect immediately
                    UserSession.currency = selectedCurrency
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
