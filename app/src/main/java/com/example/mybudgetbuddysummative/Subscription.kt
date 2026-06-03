package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Subscription : Fragment() {

    private lateinit var btnStartFree: Button
    private lateinit var btnSeePlans: Button

    private val auth = FirebaseAuth.getInstance()
    // Explicitly updated to match your MainActivity centralized Realtime Database instance URL
    private val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"
    private lateinit var userRef: com.google.firebase.database.DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subscription, container, false)

        btnStartFree = view.findViewById(R.id.btnStartFree)
        btnSeePlans = view.findViewById(R.id.btnSeePlans)

        btnStartFree.setOnClickListener { startFreePlan() }
        btnSeePlans.setOnClickListener { goToPlansPage() }

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

    private fun startFreePlan() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

        val updateMap = mapOf(
            "subscription" to "free",
            "subscribed" to false
        )

        userRef.child(userId).updateChildren(updateMap).addOnSuccessListener {
            navigateAfterSubscription()
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

    private fun goToPlansPage() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SubscriptionTwo())
            .addToBackStack(null)
            .commit()
    }
}
