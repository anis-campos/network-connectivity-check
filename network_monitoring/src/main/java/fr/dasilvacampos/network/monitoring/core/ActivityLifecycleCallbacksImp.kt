package fr.dasilvacampos.network.monitoring.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import fr.dasilvacampos.network.monitoring.NetworkConnectivityListener
import fr.dasilvacampos.network.monitoring.NetworkState

/**
 * This is the implementation Application.ActivityLifecycleCallbacksImp,
 * it calls the methods of NetworkConnectivityListener in the activity implementing it,
 * thus enabling
 * @see Application.ActivityLifecycleCallbacks
 */
internal class ActivityLifecycleCallbacksImp :
    Application.ActivityLifecycleCallbacks {


    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivityCreated(activity: Activity, p1: Bundle?) = safeRun(TAG) {
        if (activity !is LifecycleOwner) return

        if (activity is FragmentActivity)
            addLifecycleCallbackToFragments(activity)

        if (activity !is NetworkConnectivityListener || !activity.shouldBeCalled) return

        activity.onListenerCreated()

    }

    override fun onActivityResumed(activity: Activity) = safeRun {
        if (activity !is LifecycleOwner) return
        if (activity !is NetworkConnectivityListener) return

        activity.onListenerResume()
    }


    private fun addLifecycleCallbackToFragments(activity: FragmentActivity) = safeRun(
        TAG
    ) {

        val callback = object : FragmentManager.FragmentLifecycleCallbacks() {

            override fun onFragmentCreated(
                fm: FragmentManager,
                fragment: Fragment,
                savedInstanceState: Bundle?
            ) {
                if (fragment !is NetworkConnectivityListener || !fragment.shouldBeCalled) return
                fragment.onListenerCreated()
            }

            override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
                if (fragment is NetworkConnectivityListener)
                    fragment.onListenerResume()
            }
        }

        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(callback, true)
    }

    companion object {
        const val TAG = "ActivityCallbacks"

    }
}