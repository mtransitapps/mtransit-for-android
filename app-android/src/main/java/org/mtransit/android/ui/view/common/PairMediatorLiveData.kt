package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog

class PairMediatorLiveData<A, B>(a: LiveData<A>, b: LiveData<B>) :
    MediatorLiveData<Pair<A?, B?>>(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = PairMediatorLiveData::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    init {
        addSource(a) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for A '$it'")
            }
            value = it to b.value
        }
        addSource(b) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for B '$it'")
            }
            value = a.value to it
        }
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(${this.value?.toString()})"
    }
}

fun <A, B> LiveData<A>.with(other: LiveData<B>) = PairMediatorLiveData(this, other)
