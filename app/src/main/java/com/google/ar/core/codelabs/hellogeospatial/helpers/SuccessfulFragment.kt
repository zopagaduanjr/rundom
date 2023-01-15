package com.google.ar.core.codelabs.hellogeospatial.helpers

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class SuccessfulFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val elapsedTime = this.arguments!!.getString("time")
            val elapsedDist = this.arguments!!.get("distance")
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Congratulations!")
            builder.setMessage("You have successfully finished your Rundom track!\n\nDistance: $elapsedDist meters\n\nDuration: $elapsedTime\n")
                .setPositiveButton("Thanks!"
                ) { _, _ ->
                }
                .setNegativeButton("Share"
                ) { _, _ ->
                    val share = Intent(Intent.ACTION_SEND)
                    share.type = "text/plain"
                    share.putExtra(Intent.EXTRA_TEXT, "check out my rundom track!")
                    startActivity(Intent.createChooser(share, "share"))
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}