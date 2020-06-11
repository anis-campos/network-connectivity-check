package fr.dasilvacampos.network.monitoring.core

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Implementation of ConnectivityManager.NetworkCallback,
 * it stores every change of connectivity into NetworkState
 */
internal class NetworkCallbackImp(private val stateHolder: NetworkStateImp) :
    ConnectivityManager.NetworkCallback() {

    private fun updateConnectivity(isAvailable: Boolean, network: Network) {
        stateHolder.network = network
        stateHolder.isAvailable = isAvailable
    }

    //in case of a new network ( wifi enabled ) this is called first
    override fun onAvailable(network: Network) {
        Log.i(TAG, "[$network] - new network")
        updateConnectivity(true, network)
    }

    //this is called several times in a row, as capabilities are added step by step
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        Log.i(TAG, "[$network] - network capability changed: $networkCapabilities")
        stateHolder.networkCapabilities = networkCapabilities
    }

    //this is called after
    override fun onLost(network: Network) {
        Log.i(TAG, "[$network] - network lost")
        updateConnectivity(false, network)
    }

    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
        stateHolder.linkProperties = linkProperties
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