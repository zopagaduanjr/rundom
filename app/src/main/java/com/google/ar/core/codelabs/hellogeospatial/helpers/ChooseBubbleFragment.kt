package com.google.ar.core.codelabs.hellogeospatial.helpers

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.google.ar.core.codelabs.hellogeospatial.HelloGeoActivity
import com.google.ar.core.codelabs.hellogeospatial.R

class ChooseBubbleFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Choose Bubble Size")
            val editTextId = View.generateViewId()
            val editText = EditText(it)
            editText.id = editTextId
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setText("500")
            val textInputLayout = TextInputLayout(it)
            textInputLayout.hint = "Meters"
            textInputLayout.addView(editText)
            textInputLayout.setPadding(20,20,20,20)
            builder.setPositiveButton("Start"){_,_->
                val startButton: Button = it.findViewById(R.id.start_button)
                val bagTextView: TextView = it.findViewById(R.id.bag_textview)
                val z = editText.text.toString()
                (activity as HelloGeoActivity).view.activity.renderer.startRundom(z.toInt())
                startButton.visibility = View.INVISIBLE
                bagTextView.visibility = View.VISIBLE
            }
            builder.setView(textInputLayout)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}