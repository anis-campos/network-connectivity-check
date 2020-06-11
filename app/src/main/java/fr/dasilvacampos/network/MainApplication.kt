package fr.dasilvacampos.network

import android.app.Application
import fr.dasilvacampos.network.monitoring.ConnectivityStateHolder.registerConnectivityBroadcaster

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerConnectivityBroadcaster()
    }
}