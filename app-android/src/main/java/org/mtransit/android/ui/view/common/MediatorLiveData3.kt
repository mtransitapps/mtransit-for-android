package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog

class MediatorLiveData3<A, B, C>(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>) :
    MediatorLiveData<Triple<A?, B?, C?>>(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = MediatorLiveData3::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    init {
        addSource(a) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for A '$it'")
            }
            value = Triple(it, b.value, c.value)
        }
        addSource(b) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for B '$it'.")
            }
            value = Triple(a.value, it, c.value)
        }
        addSource(c) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for C '$it'.")
            }
            value = Triple(a.value, b.value, it)
        }
    }

    override fun toString() = "${javaClass.simpleName}(${this.value?.toString()})"
}