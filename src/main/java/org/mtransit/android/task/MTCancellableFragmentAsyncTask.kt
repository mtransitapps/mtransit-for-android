@file:Suppress("DEPRECATION")

package org.mtransit.android.task

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.task.MTCancellableAsyncTask
import org.mtransit.android.util.FragmentUtils
import java.lang.ref.WeakReference

@Deprecated(message = "Deprecated in Android SDK")
abstract class MTCancellableFragmentAsyncTask<Params, Progress, Result, F : Fragment?>(
    fragment: F?
) : MTCancellableAsyncTask<Params, Progress, Result>(), MTLog.Loggable {

    private val fragmentWR: WeakReference<F?> = WeakReference(fragment)

    val fragment: F?
        get() = fragmentWR.get()

    @WorkerThread
    override fun doInBackgroundNotCancelledMT(vararg params: Params?): Result? {
        if (!FragmentUtils.isFragmentReady(fragment)) {
            MTLog.d(this, "onPostExecute() > SKIP (fragment not ready)")
            return null
        }
        return fragment?.let {
            doInBackgroundNotCancelledWithFragmentMT(it, *params)
        }
    }

    @WorkerThread
    protected abstract fun doInBackgroundNotCancelledWithFragmentMT(
        fragment: F,
        vararg params: Params?
    ): Result?

    @MainThread
    override fun onProgressUpdateNotCancelledMT(vararg values: Progress?) {
        if (!FragmentUtils.isFragmentReady(fragment)) {
            MTLog.d(this, "onProgressUpdate() > SKIP (fragment not ready)")
            return
        }
        fragment?.let {
            onProgressUpdateNotCancelledFragmentReadyMT(it, *values)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @MainThread
    protected open fun onProgressUpdateNotCancelledFragmentReadyMT(
        fragment: F,
        vararg values: Progress?
    ) {
        // not mandatory
    }

    @MainThread
    override fun onPostExecuteNotCancelledMT(result: Result?) {
        if (!FragmentUtils.isFragmentReady(fragment)) {
            MTLog.d(this, "onPostExecute() > SKIP (fragment not ready)")
            return
        }
        fragment?.let {
            onPostExecuteNotCancelledFragmentReadyMT(it, result)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @MainThread
    protected open fun onPostExecuteNotCancelledFragmentReadyMT(
        fragment: F, result: Result?
    ) {
        // not mandatory
    }
}