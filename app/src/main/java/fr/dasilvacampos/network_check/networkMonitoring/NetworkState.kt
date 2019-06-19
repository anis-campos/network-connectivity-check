package fr.dasilvacampos.network_check.networkMonitoring

import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities

/**
 * Enables synchronous and asynchronous connectivity state checking thanks to LiveData and stored states.
 * @see isConnected to get the instance connectivity state
 * @see connectivityEvents to observe connectivity changes
 */
interface NetworkState {

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


}