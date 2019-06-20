package fr.dasilvacampos.network

import android.app.Application
import fr.dasilvacampos.network.monitoring.NetworkStateHolder.registerConnectivityBroadcaster

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerConnectivityBroadcaster()
    }
}