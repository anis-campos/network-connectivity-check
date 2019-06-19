package fr.dasilvacampos.network_check

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import fr.dasilvacampos.network_check.networkMonitoring.Event
import fr.dasilvacampos.network_check.networkMonitoring.NetworkConnectivityListener
import kotlinx.android.synthetic.main.activity_main.*

class SecondActivity : AppCompatActivity(), NetworkConnectivityListener {


    override fun networkConnectivityChanged(event: Event) {
        when (event) {
            is Event.ConnectivityEvent -> {
                if (event.state.isConnected) {
                    showSnackBar(textView, "The network is back !")
                    wifi_off_icon.visibility = View.GONE
                } else {
                    showSnackBar(textView, "There is no more network")
                    wifi_off_icon.visibility = View.VISIBLE
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        textView.text = "This view is hooked to network changed using NetworkConnectivityListener"


        fab.setImageResource(android.R.drawable.ic_media_rew)
        fab.setOnClickListener {
            onBackPressed()
        }
    }

}
