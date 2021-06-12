package com.shohiebsense.distancetrackerapp.ui.adapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter

class MapsBindingAdapter {


    companion object {
        @BindingAdapter("observeTracking")
        @JvmStatic
        fun observeTracking(view: View, isStarted: Boolean){
            if(isStarted && view is Button){
                view.visibility = View.VISIBLE
            }
            else if(isStarted && view is TextView) {
                view.visibility = View.INVISIBLE
            }
        }
    }
}