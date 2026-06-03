package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    private val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"
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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSpendingData()
    }

    private fun loadSpendingData() {
        val userId = auth.currentUser?.uid ?: "test_development_user"
        val envelopesRef = db.getReference("envelopes").child(userId)
        val expensesRef = db.getReference("expenses").child(userId)

        // REAL-TIME CONVERTER REPAIR: Upgraded to active value stream listeners to refresh dashboard counters automatically
        envelopesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(envelopeSnap: DataSnapshot) {

                expensesRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(expensesSnap: DataSnapshot) {
                        val spendingItems = mutableListOf<SpendingItem>()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val now = Calendar.getInstance()

                        for (env in envelopeSnap.children) {
                            val envelopeId = env.key ?: continue
                            val envelopeName = env.child("name").getValue(String::class.java) ?: continue

                            // Pulls direct target bounds from your custom envelopes creation node fields securely
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

                        // Protects adapter binding sequence state limits from thread drops
                        if (isAdded && activity != null) {
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
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TotalSpending", "Expenses data pipeline canceled: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TotalSpending", "Envelopes data pipeline canceled: ${error.message}")
            }
        })
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
