package com.example.mybudgetbuddysummative

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EnvelopeTotal : Fragment() {

    private lateinit var txtEnvelopeTitle: TextView
    private lateinit var rvExpensesList: RecyclerView
    private lateinit var rvAllocationsList: RecyclerView
    private lateinit var btnEnvelopeSettings: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    private var envelopeId: String? = null
    private var filterType: String = "month"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_envelope_total, container, false)

        txtEnvelopeTitle = view.findViewById(R.id.txtEnvelopeTitle)
        rvExpensesList = view.findViewById(R.id.rvExpensesList)
        rvAllocationsList = view.findViewById(R.id.rvAllocationsList)
        btnEnvelopeSettings = view.findViewById(R.id.btnEnvelopeSettings)

        rvExpensesList.layoutManager = LinearLayoutManager(requireContext())
        rvAllocationsList.layoutManager = LinearLayoutManager(requireContext())

        // envelopeId and name passed via arguments
        envelopeId = arguments?.getString("envelopeId")
        val envelopeName = arguments?.getString("envelopeName") ?: "Envelope"
        txtEnvelopeTitle.text = envelopeName

        if (envelopeId == null) {
            txtEnvelopeTitle.text = "No envelope selected"
        } else {
            loadExpenses()
            loadAllocations()
        }

        btnEnvelopeSettings.setOnClickListener {
            // Cycle filter type: day → week → month → year
            filterType = when (filterType) {
                "day" -> "week"
                "week" -> "month"
                "month" -> "year"
                else -> "day"
            }
            loadExpenses()
            loadAllocations()
        }
        return view
    }

    private fun loadExpenses() {
        val userId = auth.currentUser?.uid ?: return
        val envId = envelopeId ?: return
        val ref = db.getReference("expenses").child(userId).child(envId)

        ref.get().addOnSuccessListener { snapshot ->
            val expenses = mutableListOf<ExpenseItem>()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // ✅ match Income.kt
            val now = Calendar.getInstance()

            for (expenseSnap in snapshot.children) {
                val dateStr = expenseSnap.child("date").getValue(String::class.java) ?: continue
                val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                val desc = expenseSnap.child("description").getValue(String::class.java) ?: ""

                val date = sdf.parse(dateStr) ?: continue
                if (matchesFilter(date, now)) {
                    expenses.add(ExpenseItem(dateStr, desc, amount))
                }
            }

            rvExpensesList.adapter = ExpenseAdapter(expenses)
        }
    }

    private fun loadAllocations() {
        val userId = auth.currentUser?.uid ?: return
        val envId = envelopeId ?: return
        val ref = db.getReference("allocations").child(userId).child(envId) // ✅ ensure allocations saved under envelopeId

        ref.get().addOnSuccessListener { snapshot ->
            val allocations = mutableListOf<AllocationItem>()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // ✅ match Income.kt
            val now = Calendar.getInstance()

            for (allocSnap in snapshot.children) {
                val dateStr = allocSnap.child("date").getValue(String::class.java) ?: continue
                val amount = allocSnap.child("amount").getValue(Double::class.java) ?: 0.0
                val source = allocSnap.child("source").getValue(String::class.java) ?: ""

                val date = sdf.parse(dateStr) ?: continue
                if (matchesFilter(date, now)) {
                    allocations.add(AllocationItem(dateStr, source, amount))
                }
            }

            rvAllocationsList.adapter = AllocationAdapter(allocations)
        }
    }

    private fun matchesFilter(date: Date, now: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { time = date }
        return when (filterType) {
            "day" -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

            "week" -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)

            "month" -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)

            "year" -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            else -> true
        }
    }
}