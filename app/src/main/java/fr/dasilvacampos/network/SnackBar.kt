package fr.dasilvacampos.network

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun showSnackBar(view: View, text: String) {
    val snackbar = Snackbar.make(
        view,
        text,
        Snackbar.LENGTH_LONG
    )
    snackbar.setAction("Close") {
        snackbar.dismiss()
    }
    snackbar.show()
}