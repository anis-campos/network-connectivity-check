package fr.dasilvacampos.network_check.networkMonitoring

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.*
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
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

    /**
     * Network Capabilities
     */
    val networkCapabilities: NetworkCapabilities?

    /**
     * Link Properties
     */
    val linkProperties: LinkProperties?

    /**
     * Check if the network is Wifi ( shortcut )
     */
    val isWifi: Boolean
        get() = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

    /**
     * Check if the network is Mobile ( shortcut )
     */
    val isMobile: Boolean
        get() = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false

    /**
     * This liveData enabling network connectivity monitoring
     */
    val connectivityEvents: LiveData<NetworkStateHolder>

    /**
     * This liveData enabling network capability monitoring
     */
    val capabilityEvents: LiveData<NetworkStateHolder>


}

/**
 * As a static object, CurrentNetworkState gives a simple way to access the current state of the network across the app.
 * */
object CurrentNetworkState : NetworkStateHolder {

    private val instance: NetworkStateHolder = StaticNetworkStateHolder

    override val isConnected: Boolean
        get() = instance.isConnected
    override val network: Network?
        get() = instance.network
    override val networkCapabilities: NetworkCapabilities?
        get() = instance.networkCapabilities
    override val linkProperties: LinkProperties?
        get() = instance.linkProperties
    override val connectivityEvents: LiveData<NetworkStateHolder>
        get() = instance.connectivityEvents
    override val capabilityEvents: LiveData<NetworkStateHolder>
        get() = instance.capabilityEvents
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


/**
 * This starts the broadcast of network events to NetworkStateHolder and all Activity implementing NetworkConnectivityListener
 * @see NetworkStateHolder
 * @see NetworkConnectivityListener
 */
fun Application.registerConnectivityBroadcaster() {

    //register tje Activity Broadcaster
    registerActivityLifecycleCallbacks(ActivityBroadcaster)

    //get connectivity manager
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    //register to network events
    connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), NetworkEventsReceiver)
}

/**
 * This is a static implementation of NetworkStateHolder, it holds the network states and is editable but it's only usable from this file.
 * In a project with DI, this would not be static and be resolved by the container, thus removing the use of CurrentNetworkState
 * @see CurrentNetworkState
 */
private object StaticNetworkStateHolder : NetworkStateHolder {


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
 * This is the implementation Application.ActivityLifecycleCallbacks,
 * it calls the methods of NetworkConnectivityListener in the activity implementing it
 * @see Application.ActivityLifecycleCallbacks
 */
private object ActivityBroadcaster : Application.ActivityLifecycleCallbacks {

    private const val TAG = "ActivityBroadcaster"

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStarted(p0: Activity) {}

    override fun onActivityDestroyed(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivityCreated(p0: Activity, p1: Bundle?) = safeRun {
        runCatching {
            if (p0 !is LifecycleOwner) return
            if (p0 !is NetworkConnectivityListener || !p0.shouldBeCalled) return
            CurrentNetworkState.connectivityEvents.observe(p0, Observer {
                if (it.isConnected) p0.networkAvailable() else p0.networkLost()
            })
        }
    }

    override fun onActivityResumed(p0: Activity) = safeRun {
        if (p0 !is LifecycleOwner) return
        if (p0 !is NetworkConnectivityListener || !p0.checkOnResume) return
        if (CurrentNetworkState.isConnected)
            p0.networkAvailable()
        else
            p0.networkLost()
    }

    inline fun <T> T.safeRun(block: T.() -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            //ignore but log it
            Log.e(TAG, e.toString())
        }
    }

}

/**
 * Implementation of ConnectivityManager.NetworkCallback,
 * it stores the every change of connectivity into NetworkStateHolder
 * @see NetworkStateHolder
 */
private object NetworkEventsReceiver : ConnectivityManager.NetworkCallback() {

    private const val TAG = "NetworkEventsReceiver"

    private val holder = StaticNetworkStateHolder

    private fun updateConnectivity(isAvailable: Boolean, network: Network) {
        holder.isConnected = isAvailable
        holder.network = network
    }

    //in case of a new network ( wifi enabled ) this is called first
    override fun onAvailable(network: Network) {
        Log.i(TAG, "[$network] - new network")
        updateConnectivity(true, network)
    }

    //this is called several times in a row, as capabilities are added step by step
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        Log.i(TAG, "[$network] - network capability changed: $networkCapabilities")
        holder.networkCapabilities = networkCapabilities
    }

    //this is called after
    override fun onLost(network: Network) {
        Log.i(TAG, "[$network] - network lost")
        updateConnectivity(false, network)
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

