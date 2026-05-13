package org.mtransit.android.ui.view.common

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content

    override fun toString(): String {
        return "Event($content, handled=$hasBeenHandled)"
    }
}

fun <T : Any> T.toEvent() = Event(this)

fun <T : Any> LiveData<Event<T>>.observeEvent(owner: LifecycleOwner, observer: (T) -> Unit) {
    observe(owner, EventObserver(observer))
}

class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>?> {
    override fun onChanged(value: Event<T>?) {
        value?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}
