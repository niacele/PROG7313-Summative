package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TotalSpending: Fragment() {

    private lateinit var rvTotalSpendingList: RecyclerView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_total_spending, container, false)
        rvTotalSpendingList = view.findViewById(R.id.rvTotalSpendingList)
        rvTotalSpendingList.layoutManager = LinearLayoutManager(requireContext())

        loadSpendingData()

        return view
    }

    private fun loadSpendingData() {
        val userId = auth.currentUser?.uid ?: return
        val envelopesRef = db.getReference("envelopes").child(userId)
        val goalsRef = db.getReference("monthlyGoals").child(userId)
        val expensesRef = db.getReference("expenses").child(userId)

        envelopesRef.get().addOnSuccessListener { envelopeSnap ->
            val spendingItems = mutableListOf<SpendingItem>()

            // Load all goals once
            goalsRef.get().addOnSuccessListener { goalsSnap ->
                // Load all expenses once
                expensesRef.get().addOnSuccessListener { expensesSnap ->

                    for (env in envelopeSnap.children) {
                        val envelopeId = env.key ?: continue
                        val envelopeName = env.child("name").getValue(String::class.java) ?: continue

                        // ✅ Find goal amount for this envelope
                        var goalAmount = 0.0
                        val envGoals = goalsSnap.child(envelopeId)
                        for (goalSnap in envGoals.children) {
                            val target = goalSnap.child("targetAmount").getValue(Double::class.java)
                            if (target != null) {
                                goalAmount = target // you could pick latest or sum if multiple
                            }
                        }

                        // ✅ Sum expenses for this envelope
                        var totalSpent = 0.0
                        val envExpenses = expensesSnap.child(envelopeId)
                        for (exp in envExpenses.children) {
                            totalSpent += exp.child("amount").getValue(Double::class.java) ?: 0.0
                        }

                        spendingItems.add(SpendingItem(envelopeName, totalSpent, goalAmount))
                    }

                    rvTotalSpendingList.adapter = TotalSpendingAdapter(spendingItems)
                }
            }
        }
    }
}