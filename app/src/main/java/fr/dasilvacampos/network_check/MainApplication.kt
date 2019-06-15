package fr.dasilvacampos.network_check

import android.app.Application
import fr.dasilvacampos.network_check.networkMonitoring.registerConnectivityBroadcaster

class MainApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        registerConnectivityBroadcaster()
    }
}