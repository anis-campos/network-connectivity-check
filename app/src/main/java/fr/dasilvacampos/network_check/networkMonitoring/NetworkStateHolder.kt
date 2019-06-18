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
 * @see isConnected to get the instance connectivity state
 * @see connectivityEvents to observe connectivity changes
 */
interface NetworkStateHolder {

    companion object {

        lateinit var instance: NetworkStateHolder
            private set


        /**
         * This starts the broadcast of network events to NetworkStateHolder and all Activity implementing NetworkConnectivityListener
         * @see NetworkStateHolder
         * @see NetworkConnectivityListener
         */
        fun Application.registerConnectivityBroadcaster() {

            val editor = NetworkStateHolderImp()

            //register tje Activity Broadcaster
            registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksImp(editor))

            //get connectivity manager
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            //register to network events
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().build(),
                NetworkCallbackImp(editor)
            )

            instance = editor
        }
    }

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

    /**
     * This liveData enabling network link monitoring
     */
    val linkEvents: LiveData<NetworkStateHolder>
}

/**
 * This is a static implementation of NetworkStateHolder, it holds the network states and is editable but it's only usable from this file.
 * @see NetworkStateHolder.Companion
 */
private class NetworkStateHolderImp : NetworkStateHolder {


    override var isConnected: Boolean = false
        set(value) {
            field = value
            connectivityEvents.postValue(this)
        }

    override var network: Network? = null

    override var linkProperties: LinkProperties? = null
        set(value) {
            field = value
            linkEvents.postValue(this)
        }

    override var networkCapabilities: NetworkCapabilities? = null
        set(value) {
            field = value
            capabilityEvents.postValue(this)
        }

    override val connectivityEvents: MutableLiveData<NetworkStateHolder> =
        MutableLiveData()

    override val capabilityEvents: MutableLiveData<NetworkStateHolder> =
        MutableLiveData()

    override val linkEvents: MutableLiveData<NetworkStateHolder> =
        MutableLiveData()

}

/**
 * Implementation of ConnectivityManager.NetworkCallback,
 * it stores every change of connectivity into NetworkStateHolder
 * @see NetworkStateHolder
 */
private class NetworkCallbackImp(private val holder: NetworkStateHolderImp) :
    ConnectivityManager.NetworkCallback() {

    private fun updateConnectivity(isAvailable: Boolean, network: Network) {
        holder.network = network
        holder.isConnected = isAvailable
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
        holder.linkProperties = linkProperties
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

    companion object {
        private const val TAG = "NetworkCallbackImp"
    }
}

/**
 * This is the implementation Application.ActivityLifecycleCallbacksImp,
 * it calls the methods of NetworkConnectivityListener in the activity implementing it,
 * thus enabling
 * @see Application.ActivityLifecycleCallbacks
 */
private class ActivityLifecycleCallbacksImp(private val networkStateHolder: NetworkStateHolder) :
    Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "ActivityCallbacks"
        private const val ID_KEY = "ActivityCallbacks_ID"
    }


    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStarted(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivityDestroyed(p0: Activity) {}

    override fun onActivityCreated(p0: Activity, p1: Bundle?) = safeRun {
        runCatching {
            if (p0 !is LifecycleOwner) return
            if (p0 !is NetworkConnectivityListener || !p0.shouldBeCalled) return

            networkStateHolder.connectivityEvents.observe(p0, Observer {
                if (p0.previousState != null)
                    if (it.isConnected) p0.networkAvailable() else p0.networkLost()
            })
        }
    }

    override fun onActivityResumed(p0: Activity) = safeRun {
        if (p0 !is LifecycleOwner) return
        if (p0 !is NetworkConnectivityListener || !p0.shouldBeCalled || !p0.checkOnResume) return

        val previousState = p0.previousState
        val isConnected = networkStateHolder.isConnected

        p0.previousState = isConnected

        when {
            (previousState == null || previousState == true) && !isConnected -> p0.networkLost()
            previousState == false && isConnected -> p0.networkAvailable()
        }
    }


    /**
     * This property serves as a flag to detect if this activity lost network
     */
    private var Activity.previousState: Boolean?
        get() = when (intent.getIntExtra(ID_KEY, -1)) {
            -1 -> null
            0 -> false
            else -> true
        }
        set(value) {
            intent.putExtra(ID_KEY, if (value == true) 1 else 0)
        }


    /**
     * just like runCatching but without result
     * @see runCatching
     */
    private inline fun <T> T.safeRun(block: T.() -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            //ignore but log it
            Log.e(TAG, e.toString())
        }
    }


}