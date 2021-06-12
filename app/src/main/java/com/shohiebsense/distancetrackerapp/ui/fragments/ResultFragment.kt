package com.shohiebsense.distancetrackerapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shohiebsense.distancetrackerapp.R
import com.shohiebsense.distancetrackerapp.databinding.FragmentResultBinding

class ResultFragment : BottomSheetDialogFragment() {

    private val args : ResultFragmentArgs by navArgs()
    private var _resultBinding : FragmentResultBinding? = null
    private val resultBinding get() = _resultBinding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       _resultBinding = FragmentResultBinding.inflate(inflater, container, false)
        resultBinding.distanceValueTextView.text = getString(R.string.text_result_distance_value, args.result.distance)
        resultBinding.timeValueTextView.text = args.result.time
        resultBinding.shareButton.setOnClickListener {
            shareResult()
        }
        return resultBinding.root
    }

    private fun shareResult() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Aku pergi sejauh ${args.result.distance}km dalam ${args.result.time}!")
        }
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _resultBinding = null
    }
}