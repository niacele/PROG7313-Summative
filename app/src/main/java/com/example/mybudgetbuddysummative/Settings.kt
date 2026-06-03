package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class Settings : Fragment() {

    private lateinit var btnBackButton: ImageButton
    private lateinit var btnAccountInfo: Button
    private lateinit var btnCurrency: Button
    private lateinit var btnAchievements: Button
    private lateinit var btnLogout: Button
    private lateinit var btnManageSubscription: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        btnBackButton = view.findViewById(R.id.btnBackButton)
        btnAccountInfo = view.findViewById(R.id.btnAccountInfo)
        btnCurrency = view.findViewById(R.id.btnCurrency)
        btnAchievements = view.findViewById(R.id.btnAchievements)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnManageSubscription = view.findViewById(R.id.btnManageSubscription)

        btnBackButton.setOnClickListener {
            if (activity is MainActivity) {
                (activity as MainActivity).bottomNav.selectedItemId = R.id.nav_home
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        btnAccountInfo.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AccountInfo())
                .addToBackStack(null)
                .commit()
        }

        btnCurrency.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Currency())
                .addToBackStack(null)
                .commit()
        }

        btnAchievements.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Achievements())
                .addToBackStack(null)
                .commit()
        }

        btnManageSubscription.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Subscription())
                .addToBackStack(null)
                .commit()
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            UserSession.uid = null
            UserSession.email = null
            UserSession.currency = "ZAR"

            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GetStarted())
                .commit()

            (activity as? MainActivity)?.apply {
                bottomNav.visibility = View.GONE
            }
        }

        return view
    }
}