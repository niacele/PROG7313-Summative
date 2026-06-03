package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Home : Fragment() {

    private lateinit var btnDashboardSettings: ImageButton
    private lateinit var btnGenerateReport: Button
    private lateinit var txtDashboardDate: TextView
    private lateinit var txtDashboardTodayLabel: TextView
    private lateinit var mainBudgetPieChart: PieChart
    private lateinit var btnViewMore: Button

    private var currentEnvelopeId: String? = null
    private var currentEnvelopeName: String? = null

    private lateinit var expensesRef: com.google.firebase.database.DatabaseReference
    private lateinit var envelopesRef: com.google.firebase.database.DatabaseReference
    private val auth = FirebaseAuth.getInstance()

    private var filterType: String = "day"
    private val selectedCalendarAnchor = Calendar.getInstance()
    private val dynamicEnvelopeColors = mutableMapOf<String, String>()
    private val fallbackEnvelopeIdMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        btnDashboardSettings = view.findViewById(R.id.btnDashboardSettings)
        btnGenerateReport = view.findViewById(R.id.btnGenerateReport)
        txtDashboardDate = view.findViewById(R.id.txtDashboardDate)
        txtDashboardTodayLabel = view.findViewById(R.id.txtDashboardTodayLabel)
        mainBudgetPieChart = view.findViewById(R.id.mainBudgetPieChart)
        btnViewMore = view.findViewById(R.id.btnViewMore)

        btnDashboardSettings.setOnClickListener {
            val options = arrayOf("Day", "Week", "Month", "Year")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Range Type")
                .setItems(options) { _, which ->
                    filterType = options[which].lowercase()
                    showDatePickerAndFilter()
                }
                .show()
        }

        btnGenerateReport.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Report())
                .addToBackStack(null)
                .commit()
        }

        btnViewMore.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TotalSpending())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is MainActivity) {
            val mainAct = activity as MainActivity
            envelopesRef = mainAct.envelopesRef
            expensesRef = mainAct.envelopesRef.root.child("expenses")
        } else {
            val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"
            val databaseInstance = com.google.firebase.database.FirebaseDatabase.getInstance(databaseUrl)
            envelopesRef = databaseInstance.getReference("envelopes")
            expensesRef = databaseInstance.getReference("expenses")
        }

        loadDynamicEnvelopeColors {
            refreshExpensesDisplay()
        }
    }

    private fun loadDynamicEnvelopeColors(onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: "test_development_user"
        envelopesRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dynamicEnvelopeColors.clear()
                for (envSnap in snapshot.children) {
                    val name = envSnap.child("name").getValue(String::class.java)
                    val hexColor = envSnap.child("colorHex").getValue(String::class.java)
                    if (name != null && hexColor != null) {
                        dynamicEnvelopeColors[name] = hexColor
                    }
                }
                onComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                onComplete()
            }
        })
    }

    private fun showDatePickerAndFilter() {
        val year = selectedCalendarAnchor.get(Calendar.YEAR)
        val month = selectedCalendarAnchor.get(Calendar.MONTH)
        val day = selectedCalendarAnchor.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, chosenYear, chosenMonth, chosenDay ->
                selectedCalendarAnchor.set(Calendar.YEAR, chosenYear)
                selectedCalendarAnchor.set(Calendar.MONTH, chosenMonth)
                selectedCalendarAnchor.set(Calendar.DAY_OF_MONTH, chosenDay)
                refreshExpensesDisplay()
            },
            year, month, day
        ).show()
    }

    private fun refreshExpensesDisplay() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        txtDashboardDate.text = displaySdf.format(selectedCalendarAnchor.time)
        txtDashboardTodayLabel.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(selectedCalendarAnchor.time) + " (${filterType.uppercase()})"

        val userId = auth.currentUser?.uid ?: "test_development_user"

        expensesRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryTotals = mutableMapOf<String, Double>()

                if (snapshot.exists()) {
                    for (expenseSnap in snapshot.children) {
                        val envelopeName = expenseSnap.child("envelope").getValue(String::class.java) ?: "Other"
                        val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                        val dateStr = expenseSnap.child("date").getValue(String::class.java) ?: ""

                        currentEnvelopeId = expenseSnap.child("id").getValue(String::class.java)
                        currentEnvelopeName = envelopeName

                        if (!dateStr.isNullOrEmpty()) {
                            try {
                                val expenseDate = sdf.parse(dateStr)
                                if (expenseDate != null && isInRange(expenseDate, filterType, selectedCalendarAnchor)) {
                                    categoryTotals[envelopeName] = (categoryTotals[envelopeName] ?: 0.0) + amount
                                }
                            } catch (e: Exception) {
                                Log.e("HomeDebug", "Failed parsing invalid date format: $dateStr", e)
                            }
                        }
                    }
                }
                updatePieChart(categoryTotals)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeDebug", "Realtime listener dropped: ${error.message}")
            }
        })
    }

    private fun isInRange(expenseDate: java.util.Date, range: String, anchorCalendar: Calendar): Boolean {
        val expCal = Calendar.getInstance().apply { time = expenseDate }
        val anchorYear = anchorCalendar.get(Calendar.YEAR)

        return when (range) {
            "day" -> {
                expCal.get(Calendar.YEAR) == anchorYear &&
                        expCal.get(Calendar.DAY_OF_YEAR) == anchorCalendar.get(Calendar.DAY_OF_YEAR)
            }
            "week" -> {
                val weekStart = Calendar.getInstance().apply {
                    time = anchorCalendar.time
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
            "month" -> {
                expCal.get(Calendar.YEAR) == anchorYear &&
                        expCal.get(Calendar.MONTH) == anchorCalendar.get(Calendar.MONTH)
            }
            "year" -> {
                expCal.get(Calendar.YEAR) == anchorYear
            }
            else -> false
        }
    }

    private fun updatePieChart(envelopeTotal: Map<String, Double>) {
        if (envelopeTotal.isEmpty()) {
            mainBudgetPieChart.clear()
            mainBudgetPieChart.setNoDataText("No expenses logged matching this custom range.")
            mainBudgetPieChart.invalidate()
            return
        }

        val entries = envelopeTotal.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "")

        val colors = mutableListOf<Int>()
        for (entry in entries) {
            val envelope = entry.label
            val hexString = dynamicEnvelopeColors[envelope] ?: "#A3B8B8"
            colors.add(Color.parseColor(hexString))
        }

        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        mainBudgetPieChart.data = data

        mainBudgetPieChart.centerText = "${filterType.uppercase()} Breakdown"
        mainBudgetPieChart.setCenterTextSize(14f)
        mainBudgetPieChart.setDrawCenterText(true)
        mainBudgetPieChart.description.isEnabled = false
        mainBudgetPieChart.legend.isEnabled = true
        mainBudgetPieChart.animateY(800)
        mainBudgetPieChart.invalidate()
    }
}
