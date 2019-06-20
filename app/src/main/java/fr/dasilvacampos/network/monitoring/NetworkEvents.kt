package fr.dasilvacampos.network.monitoring

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * This liveData enabling network connectivity monitoring
 */
object NetworkEvents : LiveData<Event>() {

    internal val mutator = object : MutableLiveData<Event>() {
        override fun postValue(value: Event?) {
            this@NetworkEvents.postValue(value)
        }

        override fun setValue(value: Event?) {
            this@NetworkEvents.value = value
        }
    }
}