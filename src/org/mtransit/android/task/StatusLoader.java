package org.mtransit.android.task;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.provider.StatusFilter;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.StatusProviderProperties;

import android.content.Context;
import android.net.Uri;

public class StatusLoader implements MTLog.Loggable {

	private static final String TAG = StatusLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static StatusLoader instance;

	public static StatusLoader get() {
		if (instance == null) {
			instance = new StatusLoader();
		}
		return instance;
	}

	private StatusLoader() {
	}

	private ThreadPoolExecutor fetchStatusExecutor;

	private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	public ThreadPoolExecutor getFetchStatusExecutor() {
		if (this.fetchStatusExecutor == null) {
			this.fetchStatusExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
					new LIFOBlockingDeque<Runnable>());
		}
		return fetchStatusExecutor;
	}

	public boolean isBusy() {
		final boolean busy = this.fetchStatusExecutor != null && this.fetchStatusExecutor.getActiveCount() > 0;
		return busy;
	}

	public boolean findStatus(Context context, POI poi, StatusFilter statusFilter, StatusLoader.StatusLoaderListener listener,
			boolean skipIfBusy) {
		if (skipIfBusy && isBusy()) {
			return false;
		}
		Collection<StatusProviderProperties> providers = DataSourceProvider.get().getTargetAuthorityStatusProviders(context, poi.getAuthority());
		if (providers != null) {
			for (final StatusProviderProperties provider : providers) {
				final StatusFetcherCallable task = new StatusFetcherCallable(context, listener, provider, poi, statusFilter);
				task.executeOnExecutor(getFetchStatusExecutor());
				break;
			}
		}
		return true;
	}

	private static class StatusFetcherCallable extends android.os.AsyncTask<Void, Void, POIStatus> implements MTLog.Loggable {

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<Context> contextWR;
		private StatusProviderProperties statusProvider;
		private WeakReference<POI> poiWR;
		private StatusLoader.StatusLoaderListener listener;
		private StatusFilter statusFilter;

		public StatusFetcherCallable(Context context, StatusLoader.StatusLoaderListener listener, StatusProviderProperties statusProvider, POI poi,
				StatusFilter statusFilter) {
			this.contextWR = new WeakReference<Context>(context);
			this.listener = listener;
			this.statusProvider = statusProvider;
			this.poiWR = new WeakReference<POI>(poi);
			this.statusFilter = statusFilter;
		}

		@Override
		protected POIStatus doInBackground(Void... params) {
			try {
				return call();
			} catch (Exception e) {
				MTLog.w(this, e, "Error while running task!");
				return null;
			}
		}

		@Override
		protected void onPostExecute(POIStatus result) {
			if (result != null) {
				POI poi = this.poiWR == null ? null : this.poiWR.get();
				if (poi != null) {
					poi.setStatus(result);
					if (listener != null) {
						listener.onStatusLoaded();
					} else {
						MTLog.d(this, "onPostExecute() > listener null!");
					}
				} else {
					MTLog.d(this, "onPostExecute() > rts null!");
				}
			} else {
				MTLog.d(this, "onPostExecute() > result null!");
			}
		}

		public POIStatus call() throws Exception {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				MTLog.w(this, "Context mandatory!");
				return null;
			}
			POI poi = this.poiWR == null ? null : this.poiWR.get();
			if (poi == null) {
				MTLog.w(this, "POI mandatory!");
				return null;
			}
			if (statusFilter == null) {
				MTLog.w(this, "Status filter mandatory!");
				return null;
			}
			final Uri uri = DataSourceProvider.get().getUri(this.statusProvider.getAuthority());
			final POIStatus status = DataSourceProvider.findStatus(context, uri, poi, statusFilter);
			return status;
		}

	}

	public static class LIFOBlockingDeque<E> extends LinkedBlockingDeque<E> implements MTLog.Loggable {

		private static final String TAG = LIFOBlockingDeque.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final long serialVersionUID = -470545646554946137L;

		@Override
		public boolean offer(E e) {
			return super.offerFirst(e);
		}

		@Override
		public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
			return super.offerFirst(e, timeout, unit);
		}

		@Override
		public boolean add(E e) {
			return super.offerFirst(e);
		}

		@Override
		public void put(E e) throws InterruptedException {
			super.putFirst(e);
		}
	}

	public static interface StatusLoaderListener {
		public void onStatusLoaded();
	}

}
