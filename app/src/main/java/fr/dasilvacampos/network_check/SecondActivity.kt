package fr.dasilvacampos.network_check

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import fr.dasilvacampos.network_check.networkMonitoring.NetworkConnectivityListener
import kotlinx.android.synthetic.main.activity_main.*

class SecondActivity : AppCompatActivity(), NetworkConnectivityListener {

    override var checkOnResume: Boolean = false

    override fun networkAvailable() {
        showSnackBar(textView, "The network is back !")
        wifi_off_icon.visibility = View.GONE
        checkOnResume = false
    }

    override fun networkLost() {
        showSnackBar(textView, "There is no more network")
        wifi_off_icon.visibility = View.VISIBLE
        checkOnResume = true
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
