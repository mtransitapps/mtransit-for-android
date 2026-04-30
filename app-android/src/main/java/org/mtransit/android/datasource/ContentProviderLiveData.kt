package org.mtransit.android.datasource

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ContentProviderLiveData<T>(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val getContent: suspend () -> T
) : LiveData<T>() {

    private var scope: CoroutineScope? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    private fun refresh() {
        scope?.launch {
            postValue(getContent())
        }
    }

    override fun onActive() {
        super.onActive()
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        refresh()
        contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun onInactive() {
        super.onInactive()
        contentResolver.unregisterContentObserver(observer)
        scope?.cancel()
        scope = null
    }
}
