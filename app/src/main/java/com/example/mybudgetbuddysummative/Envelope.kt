package com.example.mybudgetbuddysummative

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Envelope : Fragment() {
    private lateinit var edtEnvelopeName: EditText
    private lateinit var edtAmount: EditText
    private lateinit var rgRecurring: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private lateinit var colorPaletteContainer: LinearLayout

    private var selectedColor: String = "#4A6984"
    private var isRecurring: Boolean = false

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().getReference("envelopes")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_envelope, container, false)

        edtEnvelopeName = view.findViewById(R.id.edtEnvelopeName)
        edtAmount = view.findViewById(R.id.edtAmount)
        rgRecurring = view.findViewById(R.id.rgRecurring)
        btnSave = view.findViewById(R.id.btnSave)
        btnBack = view.findViewById(R.id.btnBack)
        colorPaletteContainer = view.findViewById(R.id.colorPaletteContainer)

        // recurring flag
        rgRecurring.setOnCheckedChangeListener { _, checkedId ->
            isRecurring = checkedId == R.id.rbYes
        }

        // color palette selection
        for (i in 0 until colorPaletteContainer.childCount) {
            val child = colorPaletteContainer.getChildAt(i)
            if (child is CheckBox) {
                child.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        // uncheck others
                        for (j in 0 until colorPaletteContainer.childCount) {
                            val other = colorPaletteContainer.getChildAt(j)
                            if (other is CheckBox && other != buttonView) {
                                other.isChecked = false
                            }
                        }
                        // save selected color
                        val bg = child.background
                        selectedColor = (bg as? android.graphics.drawable.ColorDrawable)?.color?.let {
                            String.format("#%06X", 0xFFFFFF and it)
                        } ?: "#4A6984"
                    }
                }
            }
        }

        btnSave.setOnClickListener { saveEnvelope() }
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        return view
    }

    private fun saveEnvelope() {
        val name = edtEnvelopeName.text.toString().trim()
        val amountText = edtAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        if (name.isEmpty() || amount == null) {
            Toast.makeText(requireContext(), "Please enter valid name and amount", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val envelopeId = dbRef.child(userId).push().key ?: return

        val envelopeMap = mapOf(
            "id" to envelopeId,
            "userId" to userId,
            "name" to name,
            "totalAmount" to amount,
            "recurring" to isRecurring,
            "colorHex" to selectedColor
        )

        dbRef.child(userId).child(envelopeId).setValue(envelopeMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Envelope saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
