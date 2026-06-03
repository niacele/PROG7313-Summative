package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.RoundedCornersTransformation
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
    private val databaseUrl = "https://firebaseio.com"
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

        if (activity is MainActivity) {
            val mainAct = activity as MainActivity
            envelopesRef = mainAct.envelopesRef
            expensesRef = mainAct.envelopesRef.root.child("expenses")
        } else {
            val databaseInstance = FirebaseDatabase.getInstance(databaseUrl)
            envelopesRef = databaseInstance.getReference("envelopes")
            expensesRef = databaseInstance.getReference("expenses")
        }

        loadEnvelopes()
    }

    private fun loadEnvelopes() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

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
                }
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

                    val photoUriStr = expenseSnap.child("photoUri").getValue(String::class.java)

                    if (dateStr.isNotEmpty()) {
                        try {
                            val expDate = dbSdf.parse(dateStr)

                            if (expDate != null && !expDate.before(startDate) && !expDate.after(endDate)) {
                                total += amount

                                val rowContainer = LinearLayout(requireContext()).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    setPadding(12, 16, 12, 16)
                                    gravity = android.view.Gravity.CENTER_VERTICAL
                                }

                                val txtInfo = TextView(requireContext()).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        0,
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        1f
                                    )
                                    textSize = 14f
                                    setTextColor(Color.BLACK)

                                    val formattedDisplayDate = uiSdf.format(expDate)
                                    val formattedMoney = CurrencyHelper.formatAmount(amount, UserSession.currency)
                                    text = "$formattedDisplayDate - $envelopeName: $desc ($formattedMoney)"
                                }
                                rowContainer.addView(txtInfo)

                                if (!photoUriStr.isNullOrEmpty()) {
                                    val imgReceiptPreview = ImageView(requireContext()).apply {
                                        layoutParams = LinearLayout.LayoutParams(54.toPx(), 54.toPx()).apply {
                                            setMargins(8, 0, 0, 0)
                                        }
                                        scaleType = ImageView.ScaleType.CENTER_CROP
                                        contentDescription = "Receipt Attachment Preview"

                                        load(photoUriStr) {
                                            crossfade(true)
                                            placeholder(android.R.drawable.progress_horizontal)
                                            error(android.R.drawable.ic_menu_gallery)
                                            transformations(RoundedCornersTransformation(12f))
                                        }

                                        setOnClickListener {
                                            val viewDialog = ImageView(requireContext()).apply { load(photoUriStr) }
                                            AlertDialog.Builder(requireContext())
                                                .setView(viewDialog)
                                                .setPositiveButton("Close", null)
                                                .show()
                                        }
                                    }
                                    rowContainer.addView(imgReceiptPreview)
                                }

                                resultsContainer.addView(rowContainer)
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

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}

