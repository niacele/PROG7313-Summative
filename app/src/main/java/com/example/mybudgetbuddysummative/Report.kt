package com.example.mybudgetbuddysummative

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Report : Fragment() {

    private lateinit var edtFilterEnvelopeDropdown: AutoCompleteTextView
    private lateinit var edtStartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var btnViewReport: Button
    private lateinit var resultsContainer: LinearLayout
    private lateinit var txtTotal: TextView
    private lateinit var btnBackButton: ImageButton

    private val auth = FirebaseAuth.getInstance()

    // Core database path pointer layers
    private lateinit var envelopesRef: com.google.firebase.database.DatabaseReference
    private lateinit var expensesRef: com.google.firebase.database.DatabaseReference

    private val uiSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dbSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val envelopeMap = mutableMapOf<String, String>()
    private val envelopeNames = mutableListOf<String>()
    private lateinit var dropdownAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        edtFilterEnvelopeDropdown = view.findViewById(R.id.edtFilterEnvelopeDropdown)
        edtStartDate = view.findViewById(R.id.edtStartDate)
        edtEndDate = view.findViewById(R.id.edtEndDate)
        btnViewReport = view.findViewById(R.id.btnViewReport)
        resultsContainer = view.findViewById(R.id.resultsContainer)
        txtTotal = view.findViewById(R.id.txtTotal)
        btnBackButton = view.findViewById(R.id.btnBackButton)

        // Initialize drop adapter with empty list framework row templates
        dropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, envelopeNames)
        edtFilterEnvelopeDropdown.setAdapter(dropdownAdapter)

        edtStartDate.setOnClickListener { showDatePicker(edtStartDate) }
        edtEndDate.setOnClickListener { showDatePicker(edtEndDate) }

        btnViewReport.setOnClickListener { viewReport() }
        btnBackButton.setOnClickListener { parentFragmentManager.popBackStack() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CORRECTED INITIALIZATION HOOK: Inherit the verified central path directly from MainActivity threads
        if (activity is MainActivity) {
            val mainAct = activity as MainActivity
            envelopesRef = mainAct.envelopesRef
            expensesRef = mainAct.envelopesRef.root.child("expenses")
            Log.d("ReportDebug", "Successfully connected to MainActivity data threads.")
        } else {
            // Backup direct instance pointer path definitions
            val databaseUrl = "https://firebaseio.com"
            val databaseInstance = FirebaseDatabase.getInstance(databaseUrl)
            envelopesRef = databaseInstance.getReference("envelopes")
            expensesRef = databaseInstance.getReference("expenses")
        }

        // Trigger safe network fetch streaming sequence
        loadEnvelopes()
    }

    private fun loadEnvelopes() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

        // CORRECTION: Uses the inherited reference configured at root to listen for updates live
        envelopesRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                envelopeNames.clear()
                envelopeMap.clear()

                if (snapshot.exists()) {
                    for (envSnap in snapshot.children) {
                        val name = envSnap.child("name").getValue(String::class.java)
                        val id = envSnap.key
                        if (name != null && id != null) {
                            envelopeNames.add(name)
                            envelopeMap[name] = id
                        }
                    }
                    Log.d("ReportDebug", "Populated ${envelopeNames.size} envelopes into layout memory arrays.")
                } else {
                    Log.w("ReportDebug", "No snapshot children items found under: envelopes/$userId")
                }

                // Notify list adapter elements to physically draw values to screen dropdown choices
                dropdownAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ReportDebug", "Envelopes data stream failed: ${error.message}")
            }
        })
    }

    private fun showDatePicker(target: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, year, month, day ->
                val formattedMonth = String.format("%02d", month + 1)
                val formattedDay = String.format("%02d", day)
                target.setText("$formattedDay/$formattedMonth/$year")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun viewReport() {
        val userId = auth.currentUser?.uid ?: "test_development_user"
        val envelopeFilterName = edtFilterEnvelopeDropdown.text.toString().trim()
        val startDateStr = edtStartDate.text.toString().trim()
        val endDateStr = edtEndDate.text.toString().trim()

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please select start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        val startDate = uiSdf.parse(startDateStr) ?: return
        val endDate = uiSdf.parse(endDateStr) ?: return

        expensesRef.child(userId).get().addOnSuccessListener { snapshot ->
            resultsContainer.removeAllViews()
            var total = 0.0

            if (snapshot.exists()) {
                for (expenseSnap in snapshot.children) {
                    val envelopeName = expenseSnap.child("envelope").getValue(String::class.java) ?: ""

                    if (envelopeFilterName.isNotEmpty() && envelopeFilterName != envelopeName) continue

                    val dateStr = expenseSnap.child("date").getValue(String::class.java) ?: ""
                    val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                    val desc = expenseSnap.child("description").getValue(String::class.java) ?: ""

                    if (dateStr.isNotEmpty()) {
                        try {
                            val expDate = dbSdf.parse(dateStr)

                            if (expDate != null && !expDate.before(startDate) && !expDate.after(endDate)) {
                                total += amount

                                val row = TextView(requireContext())
                                row.setPadding(12, 16, 12, 16)
                                row.textSize = 15f
                                row.setTextColor(android.graphics.Color.BLACK)

                                val formattedDisplayDate = uiSdf.format(expDate)
                                val formattedMoney = CurrencyHelper.formatAmount(amount, UserSession.currency)

                                row.text = "$formattedDisplayDate - $envelopeName: $desc ($formattedMoney)"
                                resultsContainer.addView(row)
                            }
                        } catch (e: Exception) {
                            Log.e("ReportDebug", "Skipping parsing error: $dateStr", e)
                        }
                    }
                }
            }

            txtTotal.text = CurrencyHelper.formatAmount(total, UserSession.currency)

            if (resultsContainer.childCount == 0) {
                Toast.makeText(requireContext(), "No matching data found.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("ReportDebug", "Firebase extraction cancelled: ${e.message}")
        }
    }
}