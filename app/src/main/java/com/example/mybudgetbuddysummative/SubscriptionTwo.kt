package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    private val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"
    private lateinit var userRef: com.google.firebase.database.DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subscription_two, container, false)

        cardMonthlyPlan = view.findViewById(R.id.cardMonthlyPlan)
        cardAnnualPlan = view.findViewById(R.id.cardAnnualPlan)
        cardFreePlan = view.findViewById(R.id.cardFreePlan)
        btnContinueUpgrade = view.findViewById(R.id.btnContinueUpgrade)

        cardMonthlyPlan.setOnClickListener { selectPlan("monthly") }
        cardAnnualPlan.setOnClickListener { selectPlan("annual") }
        cardFreePlan.setOnClickListener { selectPlan("free") }

        btnContinueUpgrade.setOnClickListener { saveSelection() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is MainActivity) {
            userRef = (activity as MainActivity).envelopesRef.root.child("users")
        } else {
            userRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")
        }
    }

    private fun selectPlan(plan: String) {
        selectedPlan = plan

        cardMonthlyPlan.strokeWidth = if (plan == "monthly") 4 else 0
        cardAnnualPlan.strokeWidth = if (plan == "annual") 4 else 0
        cardFreePlan.strokeWidth = if (plan == "free") 4 else 0
    }

    private fun saveSelection() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

        val planValue = when (selectedPlan) {
            "monthly" -> "premium_monthly"
            "annual" -> "premium_annual"
            "free" -> "free"
            else -> null
        }

        if (planValue == null) {
            Toast.makeText(requireContext(), "Please select a plan first", Toast.LENGTH_SHORT).show()
            return
        }

        val isSubscribedBoolean = (planValue != "free")

        val updateMap = mapOf(
            "subscription" to planValue,
            "subscribed" to isSubscribedBoolean
        )

        // Pushing updateChildren completely overwrites previous plan records in the Firebase data tree
        userRef.child(userId).updateChildren(updateMap).addOnSuccessListener {
            Toast.makeText(requireContext(), "Subscription Updated!", Toast.LENGTH_SHORT).show()
            navigateAfterSubscription()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Update failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateAfterSubscription() {
        val mainAct = activity as? MainActivity
        if (mainAct != null) {
            if (mainAct.bottomNav.visibility == View.VISIBLE) {
                mainAct.bottomNav.selectedItemId = R.id.nav_home
            } else {
                mainAct.enableBottomNav()
            }
        } else {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Home())
                .commit()
        }
    }
}