package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var btnEnvelopeSettings: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val databaseUrl = "https://firebaseio.com"
    private val db = FirebaseDatabase.getInstance(databaseUrl)

    private var envelopeId: String? = null
    private var envelopeName: String? = null
    private var filterType: String = "month"

    private val availableEnvelopesList = ArrayList<String>()
    private val envelopeIdMap = HashMap<String, String>()

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

        envelopeId = arguments?.getString("envelopeId")
        envelopeName = arguments?.getString("envelopeName")

        if (!envelopeName.isNullOrEmpty()) {
            txtEnvelopeTitle.text = envelopeName
        } else {
            txtEnvelopeTitle.text = "All Envelopes"
            envelopeName = null
        }

        fetchUserEnvelopesList()

        loadExpenses()
        loadAllocations()

        btnEnvelopeSettings.setOnClickListener {
            if (availableEnvelopesList.isEmpty()) {
                Toast.makeText(requireContext(), "No alternative categories discovered.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogOptions = availableEnvelopesList.toMutableList()
            dialogOptions.add(0, "Show All Envelopes")

            AlertDialog.Builder(requireContext())
                .setTitle("Filter by Category")
                .setItems(dialogOptions.toTypedArray()) { _, which ->
                    if (which == 0) {
                        envelopeName = null
                        envelopeId = null
                        txtEnvelopeTitle.text = "All Envelopes"
                    } else {
                        val selectedCategory = dialogOptions[which]
                        envelopeName = selectedCategory
                        envelopeId = envelopeIdMap[selectedCategory]
                        txtEnvelopeTitle.text = selectedCategory
                    }

                    Toast.makeText(context, "Displaying: ${txtEnvelopeTitle.text}", Toast.LENGTH_SHORT).show()

                    loadExpenses()
                    loadAllocations()
                }
                .show()
        }
        return view
    }

    private fun fetchUserEnvelopesList() {
        val userId = auth.currentUser?.uid ?: "test_development_user"
        db.getReference("envelopes").child(userId).get().addOnSuccessListener { snapshot ->
            availableEnvelopesList.clear()
            envelopeIdMap.clear()
            for (envSnap in snapshot.children) {
                val name = envSnap.child("name").getValue(String::class.java)
                val id = envSnap.key
                if (name != null && id != null) {
                    availableEnvelopesList.add(name)
                    envelopeIdMap[name] = id
                }
            }
        }
    }

    private fun loadExpenses() {
        val userId = auth.currentUser?.uid ?: "test_development_user"
        val ref = db.getReference("expenses").child(userId)

        ref.get().addOnSuccessListener { snapshot ->
            val expenses = mutableListOf<ExpenseItem>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val now = Calendar.getInstance()

            for (expenseSnap in snapshot.children) {
                val expenseEnvelope = expenseSnap.child("envelope").getValue(String::class.java)

                if (envelopeName == null || expenseEnvelope == envelopeName) {

                    val dateStr = expenseSnap.child("date").getValue(String::class.java) ?: continue
                    val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                    val desc = expenseSnap.child("description").getValue(String::class.java) ?: ""

                    try {
                        val date = sdf.parse(dateStr)
                        if (date != null && matchesFilter(date, now)) {
                            expenses.add(ExpenseItem(dateStr, desc, amount))
                        }
                    } catch (e: Exception) {
                        Log.e("EnvelopeTotalDebug", "Malformed date parse skip entry: $dateStr", e)
                    }
                }
            }
            rvExpensesList.adapter = ExpenseAdapter(expenses)
        }
    }

    private fun loadAllocations() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

        val ref = if (envelopeId != null) {
            db.getReference("allocations").child(userId).child(envelopeId!!)
        } else {
            db.getReference("allocations").child(userId)
        }

        ref.get().addOnSuccessListener { snapshot ->
            val allocations = mutableListOf<AllocationItem>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val now = Calendar.getInstance()

            if (envelopeId != null) {
                for (allocSnap in snapshot.children) {
                    val dateStr = allocSnap.child("date").getValue(String::class.java) ?: continue
                    val amount = allocSnap.child("amount").getValue(Double::class.java) ?: 0.0
                    val source = allocSnap.child("source").getValue(String::class.java) ?: ""

                    try {
                        val date = sdf.parse(dateStr)
                        if (date != null && matchesFilter(date, now)) {
                            allocations.add(AllocationItem(dateStr, source, amount))
                        }
                    } catch (e: Exception) {
                        Log.e("EnvelopeTotalDebug", "Malformed allocation date skip entry: $dateStr", e)
                    }
                }
            } else {
                for (envelopeFolderSnap in snapshot.children) {
                    for (allocSnap in envelopeFolderSnap.children) {
                        val dateStr = allocSnap.child("date").getValue(String::class.java) ?: continue
                        val amount = allocSnap.child("amount").getValue(Double::class.java) ?: 0.0
                        val source = allocSnap.child("source").getValue(String::class.java) ?: ""

                        try {
                            val date = sdf.parse(dateStr)
                            if (date != null && matchesFilter(date, now)) {
                                allocations.add(AllocationItem(dateStr, source, amount))
                            }
                        } catch (e: Exception) {
                            Log.e("EnvelopeTotalDebug", "Malformed allocation date skip entry: $dateStr", e)
                        }
                    }
                }
            }
            rvAllocationsList.adapter = AllocationAdapter(allocations)
        }
    }

    private fun matchesFilter(date: Date, now: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { time = date }
        val currentYear = now.get(Calendar.YEAR)

        return when (filterType) {
            "day" -> cal.get(Calendar.YEAR) == currentYear &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

            "week" -> {
                val weekStart = Calendar.getInstance().apply {
                    time = now.time
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val weekEnd = Calendar.getInstance().apply {
                    time = weekStart.time
                    add(Calendar.DAY_OF_YEAR, 7)
                }
                !date.before(weekStart.time) && date.before(weekEnd.time)
            }

            "month" -> cal.get(Calendar.YEAR) == currentYear &&
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)

            "year" -> cal.get(Calendar.YEAR) == currentYear
            else -> true
        }
    }
}