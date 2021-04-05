package org.mtransit.android.task;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTAsyncTaskLoaderX<D> extends AsyncTaskLoader<D> implements MTLog.Loggable {

	public MTAsyncTaskLoaderX(@NonNull Context context) {
		super(context);
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	@Override
	public void dump(@Nullable String prefix, @Nullable FileDescriptor fd, @NonNull PrintWriter writer, @Nullable String[] args) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "dump(%s,%s,%s,%s)", prefix, fd, writer, args);
		}
		super.dump(prefix, fd, writer, args);
	}

	@Nullable
	@Override
	public D loadInBackground() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "loadInBackground()");
		}
		return loadInBackgroundMT();
	}

	/**
	 * @see AsyncTaskLoader#loadInBackground()
	 */
	@Nullable
	public abstract D loadInBackgroundMT();

	@Override
	public void onCanceled(@Nullable D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onCanceled(%s)", data);
		}
		super.onCanceled(data);
	}

	@Override
	protected void onReset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onReset()");
		}
		super.onReset();
	}

	@Override
	public void setUpdateThrottle(long delayMS) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "setUpdateThrottle(%s)", delayMS);
		}
		super.setUpdateThrottle(delayMS);
	}

	@Override
	public void deliverResult(@Nullable D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "deliverResult(%s)", data);
		}
		super.deliverResult(data);
	}

	@Override
	protected void onStartLoading() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onStartLoading()");
		}
		super.onStartLoading();
	}

	@Override
	protected void onStopLoading() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onStopLoading()");
		}
		super.onStopLoading();
	}

	// inherited from Loader

	@Override
	public void abandon() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "abandon()");
		}
		super.abandon();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public boolean cancelLoad() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "cancelLoad()");
		}
		return super.cancelLoad();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void commitContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "commitContentChanged()");
		}
		super.commitContentChanged();
	}

	@NonNull
	@Override
	public String dataToString(@Nullable D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "dataToString(%s)", data);
		}
		return super.dataToString(data);
	}

	@Override
	public void forceLoad() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "forceLoad()");
		}
		super.forceLoad();
	}

	@Override
	public boolean isAbandoned() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isAbandoned()");
		}
		return super.isAbandoned();
	}

	@Override
	public boolean isReset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isReset()");
		}
		return super.isReset();
	}

	@Override
	public boolean isStarted() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isStarted()");
		}
		return super.isStarted();
	}

	@Override
	public void onContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onContentChanged()");
		}
		super.onContentChanged();
	}

	@Override
	public void registerListener(int id, @NonNull OnLoadCompleteListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "registerListener(%s,%s)", id, listener);
		}
		super.registerListener(id, listener);
	}

	@Override
	public void reset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "reset()");
		}
		super.reset();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void rollbackContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "rollbackContentChanged()");
		}
		super.rollbackContentChanged();
	}

	@Override
	public void stopLoading() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "stopLoading()");
		}
		super.stopLoading();
	}

	@Override
	public boolean takeContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "takeContentChanged()");
		}
		return super.takeContentChanged();
	}

	@Override
	public void unregisterListener(@NonNull OnLoadCompleteListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "unregisterListener(%s)", listener);
		}
		super.unregisterListener(listener);
	}
}
