package com.example.mybudgetbuddysummative

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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.DateFormatSymbols
import java.util.Calendar

class Home : Fragment() {

    private lateinit var txtMonthName: TextView
    private lateinit var btnGenReport: Button
    private lateinit var btnMonthlyGoal: Button
    private lateinit var btnMonthFilter: ImageButton
    private lateinit var txtTotalExpensesAmount: TextView

    // nav bar buttons
    private lateinit var btnAccountButton: ImageButton
    private lateinit var btnAddExpenseHome: ImageButton
    private lateinit var btnEnvelope: ImageButton

    // Firebase reference
    private lateinit var dbRef: DatabaseReference

    // month expenses
    private var selectedMonthKey: String = ""
    private var selectedMonthDisplay: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // typecasting
        btnGenReport = view.findViewById(R.id.btnGenReport)
        btnMonthlyGoal = view.findViewById(R.id.btnMonthlyGoal)
        btnMonthFilter = view.findViewById(R.id.btnMonthFilter)
        txtMonthName = view.findViewById(R.id.txtMonthName)
        txtTotalExpensesAmount = view.findViewById(R.id.txtTotalExpensesAmount)
        btnAccountButton = view.findViewById(R.id.btnAccountButton)
        btnAddExpenseHome = view.findViewById(R.id.btnAddExpenseHome)
        btnEnvelope = view.findViewById(R.id.btnEnvelope)

        // initialize Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("expenses")

        // initialize the date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val formattedMonth = String.format("%02d", month + 1)

        selectedMonthKey = "$year-$formattedMonth"
        selectedMonthDisplay = "${getMonthName(month)} $year"
        txtMonthName.text = selectedMonthDisplay

        // button listeners
        btnGenReport.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Report())
                .addToBackStack(null)
                .commit()
        }

        btnMonthlyGoal.setOnClickListener {
            val fragment = MonthlyGoal()
            val bundle = Bundle()
            bundle.putString("selectedMonth", selectedMonthKey)
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }


        btnMonthFilter.setOnClickListener { showMonthPicker() }

        updateTotalSpending(selectedMonthKey)

        // nav bar buttons
        btnAccountButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Settings())
                .addToBackStack(null)
                .commit()
        }

        btnAddExpenseHome.setOnClickListener {
            Toast.makeText(requireContext(), "You clicked Add Expense", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Expense())
                .addToBackStack(null)
                .commit()
        }

        btnEnvelope.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Envelope())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun showMonthPicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        val datePicker = DatePickerDialog(requireContext(), { _, y, m, _ ->
            val formattedMonth = String.format("%02d", m + 1)
            selectedMonthKey = "$y-$formattedMonth"
            selectedMonthDisplay = "${getMonthName(m)} $y"
            txtMonthName.text = selectedMonthDisplay

            updateTotalSpending(selectedMonthKey)
        }, year, month, 1)

        datePicker.show()
    }

    private fun getMonthName(month: Int): String {
        return DateFormatSymbols().months[month]
    }

    private fun updateTotalSpending(monthKey: String) {
        // Query Firebase for expenses in this month
        dbRef.child(monthKey).get().addOnSuccessListener { snapshot ->
            var total = 0.0
            snapshot.children.forEach { expense ->
                val amount = expense.child("amount").getValue(Double::class.java) ?: 0.0
                total += amount
            }
            txtTotalExpensesAmount.text = "R%.2f".format(total)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show()
        }
    }
}
