package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

    private val dbRef = FirebaseDatabase.getInstance().getReference("expenses")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var filterType: String = "day"

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

        showExpensesForRange("day")

        btnDashboardSettings.setOnClickListener {
            val options = arrayOf("Day", "Week", "Month", "Year")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Range")
                .setItems(options) { _, which ->
                    val selected = options[which].lowercase()
                    filterType = selected
                    showExpensesForRange(selected)
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
                .replace(R.id.fragment_container, EnvelopeTotal())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun showExpensesForRange(range: String) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(calendar.time)

        txtDashboardDate.text = today
        txtDashboardTodayLabel.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)

        if (userId == null) return

        dbRef.child(userId).get().addOnSuccessListener { snapshot ->
            val categoryTotals = mutableMapOf<String, Double>()

            for (envelopeSnap in snapshot.children) {
                for (expenseSnap in envelopeSnap.children) {
                    val category = expenseSnap.child("category").getValue(String::class.java) ?: "Other"
                    val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                    val date = expenseSnap.child("date").getValue(String::class.java) ?: ""

                    val expenseDate = sdf.parse(date) ?: continue
                    if (isInRange(expenseDate, range, calendar)) {
                        categoryTotals[category] = (categoryTotals[category] ?: 0.0) + amount
                    }
                }
            }

            updatePieChart(categoryTotals)
        }
    }

    private fun isInRange(expenseDate: java.util.Date, range: String, calendar: Calendar): Boolean {
        val expCal = Calendar.getInstance().apply { time = expenseDate }
        return when (range) {
            "day" -> expCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    expCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
            "week" -> expCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    expCal.get(Calendar.WEEK_OF_YEAR) == calendar.get(Calendar.WEEK_OF_YEAR)
            "month" -> expCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    expCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
            "year" -> expCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            else -> false
        }
    }

    private fun updatePieChart(envelopeTotal: Map<String, Double>) {
        val entries = envelopeTotal.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "Expenses by Category")

        val colors = mutableListOf<Int>()
        for (entry in entries) {
            val envelope = entry.label
            val colorInt = envelopeColour[envelope] ?: Color.parseColor("#FFFFFF")
            colors.add(colorInt)
        }

        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        mainBudgetPieChart.data = data
        mainBudgetPieChart.invalidate()
    }

    private val envelopeColour: Map<String, Int> = mapOf(
        "Groceries" to Color.parseColor("#C57B7B"),
        "Transport" to Color.parseColor("#404040"),
        "Entertainment" to Color.parseColor("#FFAAAA"),
        "Bills" to Color.parseColor("#93A9A7"),
        "Other" to Color.parseColor("#FFFFFF")
    )
}
