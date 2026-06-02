package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SubscriptionTwo : Fragment() {

    private lateinit var cardMonthlyPlan: MaterialCardView
    private lateinit var cardAnnualPlan: MaterialCardView
    private lateinit var cardFreePlan: MaterialCardView
    private lateinit var btnContinueUpgrade: MaterialButton

    private var selectedPlan: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subscription_two, container, false)

        cardMonthlyPlan = view.findViewById(R.id.cardMonthlyPlan)
        cardAnnualPlan = view.findViewById(R.id.cardAnnualPlan)
        cardFreePlan = view.findViewById(R.id.cardFreePlan)
        btnContinueUpgrade = view.findViewById(R.id.btnContinueUpgrade)

        // Handle selection
        cardMonthlyPlan.setOnClickListener { selectPlan("monthly") }
        cardAnnualPlan.setOnClickListener { selectPlan("annual") }
        cardFreePlan.setOnClickListener { selectPlan("free") }

        btnContinueUpgrade.setOnClickListener { saveSelection() }

        return view
    }

    private fun selectPlan(plan: String) {
        selectedPlan = plan

        // Visual feedback: highlight selected card
        cardMonthlyPlan.strokeWidth = if (plan == "monthly") 4 else 0
        cardAnnualPlan.strokeWidth = if (plan == "annual") 4 else 0
        cardFreePlan.strokeWidth = if (plan == "free") 4 else 0
    }

    private fun saveSelection() {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.getReference("users").child(userId).child("subscription")

        // Free mode is not a subscription
        val value = when (selectedPlan) {
            "monthly" -> "premium_monthly"
            "annual" -> "premium_annual"
            "free" -> "free"
            else -> null
        }

        if (value != null) {
            ref.setValue(value).addOnSuccessListener {
                // Navigate to Home/Dashboard
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, Home())
                    .commit()
            }
        }
    }
}
