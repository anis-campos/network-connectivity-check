package fr.dasilvacampos.network.monitoring

/**
 * Enables synchronous and asynchronous connectivity state checking thanks to LiveData and stored states.
 * @see isConnected to get the instance connectivity state
 * @see NetworkEvents to observe connectivity changes
 */
interface ConnectivityState{
    /**
     * Stored connectivity state of the device
     * True if the device has a available network
     */
    val isConnected: Boolean
        get() = networkStats.any{
            it.isAvailable
        }

    /**
     * The stats of the networks being used by the device
     */
    val networkStats: Iterable<NetworkState>
}