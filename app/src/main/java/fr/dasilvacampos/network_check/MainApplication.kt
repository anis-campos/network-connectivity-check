package fr.dasilvacampos.network_check

import android.app.Application
import fr.dasilvacampos.network_check.networkMonitoring.NetworkStateHolder.Companion.registerConnectivityBroadcaster

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        this.registerConnectivityBroadcaster()
    }
}