package fr.dasilvacampos.network

import android.app.Application
import fr.dasilvacampos.network.monitoring.ConnectivityStateHolder.initNetworkMonitorConfig

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initNetworkMonitorConfig()
    }

}