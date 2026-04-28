package org.mtransit.android.datasource

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData

class ContentProviderLiveData<T>(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val getContent: () -> T
) : LiveData<T>() {

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            postValue(getContent())
        }
    }

    override fun onActive() {
        super.onActive()
        postValue(getContent())
        contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun onInactive() {
        super.onInactive()
        contentResolver.unregisterContentObserver(observer)
    }
}
