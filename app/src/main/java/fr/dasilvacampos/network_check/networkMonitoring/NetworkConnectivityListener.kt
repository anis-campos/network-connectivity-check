package fr.dasilvacampos.network_check.networkMonitoring

/**
 * When implemented by an Activity, it add hooks to network events thanks to ActivityLifecycleCallbacks
 * @see android.app.Application.ActivityLifecycleCallbacks
 */
interface NetworkConnectivityListener {

    /**
     * Put this at false to disable hooks ( enabled by default )
     */
    val shouldBeCalled: Boolean
        get() = true

    /**
     * Put this at false to disable hooks on resume ( enabled by default )
     */
    val checkOnResume: Boolean
        get() = true

    /**
     * Called if connectivity is lost. If checkOnResume is true, this will be called on resume
     * if the connectivity was lost during the activity idle time
     */
    fun networkLost() {}

    /**
     * Called if connectivity is back. If checkOnResume is true, this will be called on resume
     * if the connectivity was back during the activity idle time
     */
    fun networkAvailable() {}
}