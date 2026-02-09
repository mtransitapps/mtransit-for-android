package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.commons.model.Quintuple

class MediatorLiveData5<A, B, C, D, E>(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>, d: LiveData<D>, e: LiveData<E>) :
    MediatorLiveData<Quintuple<A?, B?, C?, D?, E?>>(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = MediatorLiveData5::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    init {
        addSource(a) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for A '$it'")
            }
            value = Quintuple(it, b.value, c.value, d.value, e.value)
        }
        addSource(b) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for B '$it'")
            }
            value = Quintuple(a.value, it, c.value, d.value, e.value)
        }
        addSource(c) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for C '$it'")
            }
            value = Quintuple(a.value, b.value, it, d.value, e.value)
        }
        addSource(d) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for D '$it'")
            }
            value = Quintuple(a.value, b.value, c.value, it, e.value)
        }
        addSource(e) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for E '$it'")
            }
            value = Quintuple(a.value, b.value, c.value, d.value, it)
        }
    }

    override fun toString() = "${javaClass.simpleName}(${this.value?.toString()})"
}