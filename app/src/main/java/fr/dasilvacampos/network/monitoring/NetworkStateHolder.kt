package fr.dasilvacampos.network.monitoring

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.*
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer


object NetworkStateHolder : NetworkState {

    private lateinit var holder: NetworkStateImp


    /**
     * This starts the broadcast of network events to NetworkState and all Activity implementing NetworkConnectivityListener
     * @see NetworkState
     * @see NetworkConnectivityListener
     */
    fun Application.registerConnectivityBroadcaster() {

        holder = NetworkStateImp()

        //register tje Activity Broadcaster
        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksImp(holder))


        //get connectivity manager
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //register to network events
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            NetworkCallbackImp(holder)
        )

    }

    override val isConnected: Boolean
        get() = holder.isConnected
    override val network: Network?
        get() = holder.network
    override val networkCapabilities: NetworkCapabilities?
        get() = holder.networkCapabilities
    override val linkProperties: LinkProperties?
        get() = holder.linkProperties


    /**
     * This is a static implementation of NetworkState, it holds the network states and is editable but it's only usable from this file.
     */
    private class NetworkStateImp : NetworkState {

        override var isConnected: Boolean = false
            set(value) {
                field = value
                NetworkEvents.notify(Event.ConnectivityEvent(this))
            }

        override var network: Network? = null

        override var linkProperties: LinkProperties? = null
            set(value) {
                val old = field
                field = value
                NetworkEvents.notify(Event.LinkPropertyChangeEvent(this, old))
            }

        override var networkCapabilities: NetworkCapabilities? = null
            set(value) {
                val old = field
                field = value
                NetworkEvents.notify(Event.NetworkCapabilityEvent(this, old))

            }
    }


    /**
     * Implementation of ConnectivityManager.NetworkCallback,
     * it stores every change of connectivity into NetworkState
     * @see NetworkState
     */
    private class NetworkCallbackImp(private val holder: NetworkStateImp) :
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
    private class ActivityLifecycleCallbacksImp(private val networkState: NetworkState) :
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
            if (p0 !is LifecycleOwner) return

            handleFragments(p0)

            if (p0 !is NetworkConnectivityListener || !p0.shouldBeCalled) return

            p0.onListenerCreated()

        }

        private fun handleFragments(activity: Activity) = safeRun {
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(object :
                    FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                        if (f !is NetworkConnectivityListener || !f.shouldBeCalled) return

                        f.onListenerCreated()


                    }

                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        if (f !is NetworkConnectivityListener) return
                        f.onListenerResume()
                    }

                }, true)
            }
        }

        private fun NetworkConnectivityListener.onListenerResume() {
            if (!shouldBeCalled || !checkOnResume) return

            val previousState = previousState
            val isConnected = networkState.isConnected

            this.previousState = isConnected

            val connectionLost = (previousState == null || previousState == true) && !isConnected
            val connectionBack = previousState == false && isConnected

            if (connectionLost || connectionBack) {
                networkConnectivityChanged(Event.ConnectivityEvent(networkState))
            }

        }

        private fun NetworkConnectivityListener.onListenerCreated() {

            NetworkEvents.observe(this as LifecycleOwner, Observer {
                if (previousState != null)
                    networkConnectivityChanged(it)
            })

        }

        override fun onActivityResumed(p0: Activity) = safeRun {
            if (p0 !is LifecycleOwner) return
            if (p0 !is NetworkConnectivityListener) return

            p0.onListenerResume()
        }


        /**
         * This property serves as a flag to detect if this activity lost network
         */
        private var NetworkConnectivityListener.previousState: Boolean?
            get() {
                return when {
                    this is Fragment -> this.arguments?.previousState
                    this is Activity -> this.intent.extras?.previousState
                    else -> null
                }
            }
            set(value) {
                when {
                    this is Fragment -> this.arguments?.previousState = value
                    this is Activity -> this.intent.extras?.previousState = value
                }
            }


        private var Bundle.previousState: Boolean?
            get() = when (getInt(ID_KEY, -1)) {
                -1 -> null
                0 -> false
                else -> true
            }
            set(value) {
                putInt(ID_KEY, if (value == true) 1 else 0)
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
}

