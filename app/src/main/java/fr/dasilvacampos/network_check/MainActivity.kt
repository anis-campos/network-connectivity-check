package fr.dasilvacampos.network_check

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import fr.dasilvacampos.network_check.networkMonitoring.Event
import fr.dasilvacampos.network_check.networkMonitoring.NetworkEvents
import fr.dasilvacampos.network_check.networkMonitoring.NetworkState
import fr.dasilvacampos.network_check.networkMonitoring.NetworkStateHolder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    var lostConnection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        textView.text = "This view is hooked to network changed using NetworkState and LiveData"

        NetworkEvents.observe(this, Observer {
            if (it is Event.ConnectivityEvent)
                handleConnectivityChange(it.state)
        })

        fab.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

    }

    private fun handleConnectivityChange(networkState: NetworkState) {
        if (networkState.isConnected) {
            if (lostConnection) {
                showSnackBar(textView, "The network is back !")
                wifi_off_icon.visibility = View.GONE
            }
            lostConnection = false
        } else {
            showSnackBar(textView, "No Network !")
            wifi_off_icon.visibility = View.VISIBLE
            lostConnection = true
        }

    }

    override fun onResume() {
        super.onResume()
        handleConnectivityChange(NetworkStateHolder)
    }

}


fun showSnackBar(view: View, text: String) {
    val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
    snackbar.setAction("Close") {
        snackbar.dismiss()
    }
    snackbar.show()
}