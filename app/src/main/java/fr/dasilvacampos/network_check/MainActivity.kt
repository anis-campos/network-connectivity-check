package fr.dasilvacampos.network_check

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import fr.dasilvacampos.network_check.networkMonitoring.NetworkStateHolder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        textView.text = "This view is hooked to network changed using NetworkStateHolder and LiveData"

        NetworkStateHolder.instance.connectivityEvents.observe(this, Observer {
            if (it.isConnected)
                showSnackBar(textView, "The network is back !")
            else
                showSnackBar(textView, "The network is back !")

        })

        fab.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

    }


}

fun showSnackBar(view: View, text: String) {
    val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
    snackbar.setAction("Close") {
        snackbar.dismiss()
    }
    snackbar.show()
}