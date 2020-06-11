package fr.dasilvacampos.network.monitoring.core

import android.net.LinkProperties
import android.net.NetworkCapabilities
import fr.dasilvacampos.network.monitoring.ConnectivityState
import fr.dasilvacampos.network.monitoring.NetworkState

sealed class NetworkEvent {
    abstract val  state: NetworkState

    class AvailabilityEvent(override val state: NetworkState, val oldNetworkAvailability : Boolean, val oldConnectivity:Boolean): NetworkEvent()
    class NetworkCapabilityEvent(override val state: NetworkState, val old: NetworkCapabilities?) : NetworkEvent()
    class LinkPropertyChangeEvent(override val state: NetworkState, val old: LinkProperties?) : NetworkEvent()
}