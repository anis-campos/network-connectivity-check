package fr.dasilvacampos.network_check.networkMonitoring

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.net.*
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 * Enables synchronous and asynchronous connectivity state checking thanks to LiveData and stored states.
 * @see isConnected to get the current connectivity state
 * @see connectivityEvents to observe connectivity changes
 */
interface NetworkStateHolder {

    /**
     * Stored connectivity state of the network
     * <p>
     * True if the device as access the the network
     */
    val isConnected: Boolean

    /**
     * The network being used by the device
     */
    val network: Network?


    val networkCapabilities: NetworkCapabilities?

    val linkProperties: LinkProperties?

    /**
     *
     */
    val isWifi: Boolean?
        get() = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

    val isMobile: Boolean?
        get() = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

    /**
     * This liveData enabling network connectivity monitoring
     */
    val connectivityEvents: MutableLiveData<NetworkStateHolder>

    /**
     * This liveData enabling network capability monitoring
     */
    val capabilityEvents: MutableLiveData<NetworkStateHolder>


    companion object {

        /**
         * Singleton
         */
        val instance: NetworkStateHolder by lazy { NetworkStateHolderImplementation() }
    }
}

private class NetworkStateHolderImplementation : NetworkStateHolder {


    override var isConnected: Boolean = false
        set(value) {
            field = value
            connectivityEvents.postValue(this)
        }

    override var network: Network? = null

    override var linkProperties: LinkProperties? = null

    override var networkCapabilities: NetworkCapabilities? = null
        set(value) {
            field = value
            capabilityEvents.postValue(this)
        }

    override val connectivityEvents: MutableLiveData<NetworkStateHolder> = MutableLiveData()

    override val capabilityEvents: MutableLiveData<NetworkStateHolder> = MutableLiveData()

}



/**
 * This starts the listener and the broadcaster for connectivity state
 * @see NetworkStateHolder
 */
fun Application.registerConnectivityBroadcaster() {


    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(p0: Activity) {}

        override fun onActivityStarted(p0: Activity) {}

        override fun onActivityDestroyed(p0: Activity) {}

        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

        override fun onActivityStopped(p0: Activity) {}

        override fun onActivityCreated(p0: Activity, p1: Bundle?) {
            if (p0 !is LifecycleOwner) return
            if (p0 !is NetworkConnectivityListener || !p0.shouldBeCalled) return
            NetworkStateHolder.instance.connectivityEvents.observe(p0, Observer {
                if (it.isConnected) p0.networkAvailable() else p0.networkLost()
            })
        }

        override fun onActivityResumed(p0: Activity) {
            if (p0 !is LifecycleOwner) return
            if (p0 !is NetworkConnectivityListener || !p0.checkOnResume) return
            if (NetworkStateHolder.instance.isConnected)
                p0.networkAvailable()
            else
                p0.networkLost()
        }

    })

    val holder = NetworkStateHolder.instance as NetworkStateHolderImplementation


    val broadcaster = object : ConnectivityManager.NetworkCallback() {


        private val TAG = "broadcaster"

        private fun connectivityIntent(isAvailable: Boolean, network: Network) {
            holder.isConnected = isAvailable
            holder.network = network
        }


        private fun capabilityIntent(networkCapabilities: NetworkCapabilities) {
            holder.networkCapabilities = networkCapabilities
        }

        //in case of a new network ( wifi enabled ) this is called first
        override fun onAvailable(network: Network) {
            Log.i(TAG, "[$network] - new network")
            connectivityIntent(true, network)
        }

        //this is called several times in a row, as capabilities are added step by step
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log.i(TAG, "[$network] - network capability changed: $networkCapabilities")
            capabilityIntent(networkCapabilities)
        }

        //this is called after
        override fun onLost(network: Network) {
            Log.i(TAG, "[$network] - network lost")
            connectivityIntent(false, network)
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            Log.i(TAG, "[$network] - link changed: ${linkProperties.interfaceName}")
        }

        override fun onUnavailable() {
            Log.i(TAG, "Unavailable")
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            Log.i(TAG, "[$network] - Losing with $maxMsToLive")
        }

        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
            Log.i(TAG, "[$network] - Blocked status changed: $blocked")
        }
    }



    //get connectivity manager
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


    //register to network events
    connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), broadcaster)
}


/**
 * When placed upon a Activity, this is works as automatic hook to network events
 */
interface NetworkConnectivityListener {

    /**
     * Put this at false to disable hooks
     */
    val shouldBeCalled: Boolean
        get() = true

    /**
     * put this a false to not call hooks on resume
     */
    val checkOnResume: Boolean
        get() = true

    /**
     * Called if connectivity is lost and on resume if there is not connection and checkOnResume is true
     */
    fun networkLost() {}

    /**
     * Called if connectivity is and on resume if there is connection and checkOnResume is false
     */
    fun networkAvailable() {}
}
