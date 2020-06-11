package fr.dasilvacampos.network.monitoring

sealed class Event {

    class ConnectivityEvent(val isConnected: Boolean) : Event()
}