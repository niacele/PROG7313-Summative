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

        // Load envelopes and goals first
        envelopesRef.get().addOnSuccessListener { envelopeSnap ->
            val spendingItems = mutableListOf<SpendingItem>()

            for (env in envelopeSnap.children) {
                val envelopeName = env.child("name").getValue(String::class.java) ?: continue
                val goalAmount = goalsRef.child(env.key!!).child("targetAmount").get().result?.getValue(Double::class.java) ?: 0.0

                // Sum expenses for this envelope
                var totalSpent = 0.0
                val expenseSnap = expensesRef.child(env.key!!).get().result
                expenseSnap?.children?.forEach { exp ->
                    totalSpent += exp.child("amount").getValue(Double::class.java) ?: 0.0
                }

                spendingItems.add(SpendingItem(envelopeName, totalSpent, goalAmount))
            }

            rvTotalSpendingList.adapter = TotalSpendingAdapter(spendingItems)
        }
    }
}