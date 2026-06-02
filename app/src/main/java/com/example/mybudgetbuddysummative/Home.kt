package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Home : Fragment() {

    private lateinit var btnDashboardSettings: ImageButton
    private lateinit var btnGenerateReport: Button
    private lateinit var txtDashboardDate: TextView
    private lateinit var txtDashboardTodayLabel: TextView
    private lateinit var mainBudgetPieChart: PieChart

    private val dbRef = FirebaseDatabase.getInstance().getReference("expenses")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

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

        // Default: show today's expenses
        showExpensesForRange("day")

        btnDashboardSettings.setOnClickListener {
            val options = arrayOf("Day", "Week", "Month", "Year")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Range")
                .setItems(options) { _, which ->
                    val selected = options[which].lowercase()
                    showExpensesForRange(selected)
                }
                .show()
        }

        btnGenerateReport.setOnClickListener {
            // Navigate to ReportFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Report())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun showExpensesForRange(range: String) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(calendar.time)

        txtDashboardDate.text = today
        txtDashboardTodayLabel.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)

        if (userId == null) return
        dbRef.child(userId).get().addOnSuccessListener { snapshot ->
            val categoryTotals = mutableMapOf<String, Double>()

            for (expenseSnap in snapshot.children) {
                val category = expenseSnap.child("category").getValue(String::class.java) ?: "Other"
                val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                val date = expenseSnap.child("date").getValue(String::class.java) ?: ""

                if (isInRange(date, range, calendar)) {
                    categoryTotals[category] = (categoryTotals[category] ?: 0.0) + amount
                }
            }

            updatePieChart(categoryTotals)
        }
    }

    private fun isInRange(date: String, range: String, calendar: Calendar): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expenseDate = sdf.parse(date) ?: return false

        return when (range) {
            "day" -> sdf.format(expenseDate) == sdf.format(calendar.time)
            "week" -> {
                val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                val expCal = Calendar.getInstance().apply { time = expenseDate }
                expCal.get(Calendar.WEEK_OF_YEAR) == weekOfYear
            }
            "month" -> {
                val month = calendar.get(Calendar.MONTH)
                val year = calendar.get(Calendar.YEAR)
                val expCal = Calendar.getInstance().apply { time = expenseDate }
                expCal.get(Calendar.MONTH) == month && expCal.get(Calendar.YEAR) == year
            }
            "year" -> {
                val year = calendar.get(Calendar.YEAR)
                val expCal = Calendar.getInstance().apply { time = expenseDate }
                expCal.get(Calendar.YEAR) == year
            }
            else -> false
        }
    }

    private fun updatePieChart(envelopeTotal: Map<String, Double>) {
        val entries = envelopeTotal.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "Expenses by Category")

        val colors = mutableListOf<Int>()
        for (entry in entries) {
            val envelope = entry.label
            //val colorRes = envelopeColour[envelope] ?: R.color.white // fallback
            //colors.add(resources.getColor(colorRes, null))
        }

        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = resources.getColor(R.color.black, null)

        val data = PieData(dataSet)
        mainBudgetPieChart.data = data
        mainBudgetPieChart.invalidate()
    }

}
