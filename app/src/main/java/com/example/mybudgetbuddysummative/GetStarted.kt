package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class GetStarted : Fragment() {

    private lateinit var btnGetStarted: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_get_started, container, false)

        btnGetStarted = view.findViewById(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            // Navigate to Login
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Login())
                .addToBackStack(null) // ✅ allows user to go back to GetStarted
                .commit()
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = GetStarted()
    }
}