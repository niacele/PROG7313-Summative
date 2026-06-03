package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TotalSpending : Fragment() {

    private lateinit var rvTotalSpendingList: RecyclerView
    private lateinit var btnBackButton: ImageButton
    private lateinit var btnSpendingFilter: ImageButton
    private lateinit var btnAddNewExpense: MaterialButton
    private lateinit var txtSpendingSubheading: TextView

    private val auth = FirebaseAuth.getInstance()
    private val databaseUrl = "https://firebaseio.com"
    private val db = FirebaseDatabase.getInstance(databaseUrl)

    private var filterType: String = "month"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_total_spending, container, false)

        rvTotalSpendingList = view.findViewById(R.id.rvTotalSpendingList)
        btnBackButton = view.findViewById(R.id.btnBackButton)
        btnSpendingFilter = view.findViewById(R.id.btnSpendingFilter)
        btnAddNewExpense = view.findViewById(R.id.btnAddNewExpense)
        txtSpendingSubheading = view.findViewById(R.id.txtSpendingSubheading)

        rvTotalSpendingList.layoutManager = LinearLayoutManager(requireContext())
        rvTotalSpendingList.isNestedScrollingEnabled = false

        txtSpendingSubheading.text = filterType.replaceFirstChar { it.uppercase() }

        btnBackButton.setOnClickListener { parentFragmentManager.popBackStack() }

        btnAddNewExpense.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Expense())
                .addToBackStack(null)
                .commit()
        }

        btnSpendingFilter.setOnClickListener {
            val options = arrayOf("Day", "Week", "Month", "Year")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Spending Filter Range")
                .setItems(options) { _, which ->
                    filterType = options[which].lowercase()
                    txtSpendingSubheading.text = options[which]
                    loadSpendingData()
                }
                .show()
        }
        //TEMPORARILY COMMENTED OUT!!!
        //loadSpendingData()

        return view
    }

    //ADDED THIS FUNCTION TEMPORARILY!!!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSpendingData()
    }

    private fun loadSpendingData() {
        val userId = auth.currentUser?.uid ?: "test_development_user"
        val envelopesRef = db.getReference("envelopes").child(userId)
        val expensesRef = db.getReference("expenses").child(userId)

        envelopesRef.get().addOnSuccessListener { envelopeSnap ->
            val spendingItems = mutableListOf<SpendingItem>()

            expensesRef.get().addOnSuccessListener { expensesSnap ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val now = Calendar.getInstance()

                for (env in envelopeSnap.children) {
                    val envelopeId = env.key ?: continue
                    val envelopeName = env.child("name").getValue(String::class.java) ?: continue

                    // Reads default target amounts configuration values from your envelopes profile node securely
                    val goalAmount = env.child("totalAmount").getValue(Double::class.java) ?: 0.0

                    var totalSpent = 0.0
                    for (exp in expensesSnap.children) {
                        val savedEnvelopeName = exp.child("envelope").getValue(String::class.java)

                        if (savedEnvelopeName == envelopeName) {
                            val dateStr = exp.child("date").getValue(String::class.java) ?: ""
                            if (dateStr.isNotEmpty()) {
                                try {
                                    val expenseDate = sdf.parse(dateStr)
                                    if (expenseDate != null && isInRange(expenseDate, filterType, now)) {
                                        totalSpent += exp.child("amount").getValue(Double::class.java) ?: 0.0
                                    }
                                } catch (e: Exception) {
                                    Log.e("TotalSpending", "Skipping parsing error date string: $dateStr", e)
                                }
                            }
                        }
                    }

                    spendingItems.add(SpendingItem(envelopeId, envelopeName, totalSpent, goalAmount))
                }

                rvTotalSpendingList.adapter = TotalSpendingAdapter(spendingItems) { selectedItem ->
                    val fragment = EnvelopeTotal().apply {
                        arguments = Bundle().apply {
                            putString("envelopeId", selectedItem.envelopeId)
                            putString("envelopeName", selectedItem.envelopeName)
                        }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("TotalSpending", "Firebase connection failed: ${e.message}")
        }
    }

    private fun isInRange(expenseDate: Date, range: String, calendar: Calendar): Boolean {
        val expCal = Calendar.getInstance().apply { time = expenseDate }
        val anchorYear = calendar.get(Calendar.YEAR)

        return when (range) {
            "day" -> expCal.get(Calendar.YEAR) == anchorYear &&
                    expCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
            "week" -> {
                val weekStart = Calendar.getInstance().apply {
                    time = calendar.time
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
                !expenseDate.before(weekStart.time) && expenseDate.before(weekEnd.time)
            }
            "month" -> expCal.get(Calendar.YEAR) == anchorYear &&
                    expCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
            "year" -> expCal.get(Calendar.YEAR) == anchorYear
            else -> false
        }
    }
}