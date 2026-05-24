package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class GetStarted : Fragment() {

    private lateinit var btnGetStarted: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // You can handle arguments here if needed
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_get_started, container, false)

        // Wire up button
        btnGetStarted = view.findViewById(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            // Navigate to LoginFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Login())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = GetStarted()
    }
}
