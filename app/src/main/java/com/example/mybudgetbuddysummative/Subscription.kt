package com.example.mybudgetbuddysummative

import android.os.Bundle
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
    private val db = FirebaseDatabase.getInstance()

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

    private fun startFreePlan() {
        val userId = auth.currentUser?.uid ?: return

        // Save subscription status in Firebase
        val ref = db.getReference("users").child(userId).child("subscription")
        ref.setValue("free").addOnSuccessListener {
            // Navigate to Home/Dashboard
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Home())
                .commit()
        }
    }

    private fun goToPlansPage() {
        // Navigate to a Premium Plans fragment/page
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SubscriptionTwo())
            .addToBackStack(null)
            .commit()
    }
}
