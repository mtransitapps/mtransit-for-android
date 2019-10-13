package org.mtransit.android.task;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.provider.StatusProviderContract;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.StatusProviderProperties;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

	@NonNull
	private HashMap<String, ThreadPoolExecutor> fetchStatusExecutors = new HashMap<>();

	private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES > 1 ? RuntimeUtils.NUMBER_OF_CORES / 2 : 1;

	private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES > 1 ? RuntimeUtils.NUMBER_OF_CORES / 2 : 1;

	@NonNull
	public ThreadPoolExecutor getFetchStatusExecutor(String statusProviderAuthority) {
		if (!this.fetchStatusExecutors.containsKey(statusProviderAuthority)) {
			this.fetchStatusExecutors.put(statusProviderAuthority, //
					new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LIFOBlockingDeque<>()));
		}
		return this.fetchStatusExecutors.get(statusProviderAuthority);
	}

	public boolean isBusy() {
		for (Map.Entry<String, ThreadPoolExecutor> fetchStatusExecutor : this.fetchStatusExecutors.entrySet()) {
			if (fetchStatusExecutor.getValue() != null && fetchStatusExecutor.getValue().getActiveCount() > 0) {
				return true;
			}
		}
		return false;
	}

	public void clearAllTasks() {
		Iterator<Map.Entry<String, ThreadPoolExecutor>> it = this.fetchStatusExecutors.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ThreadPoolExecutor> fetchStatusExecutor = it.next();
			if (fetchStatusExecutor.getValue() != null) {
				fetchStatusExecutor.getValue().shutdown();
			}
			it.remove();
		}
	}

	public boolean findStatus(@Nullable Context context, @NonNull POIManager poim, @NonNull StatusProviderContract.Filter statusFilter,
			@Nullable StatusLoader.StatusLoaderListener listener, boolean skipIfBusy) {
		if (skipIfBusy && isBusy()) {
			return false;
		}
		HashSet<StatusProviderProperties> providers = DataSourceProvider.get(context).getTargetAuthorityStatusProviders(poim.poi.getAuthority());
		if (providers != null && providers.size() > 0) {
			for (StatusProviderProperties provider : providers) {
				if (provider == null) {
					continue;
				}
				StatusFetcherCallable task = new StatusFetcherCallable(context, listener, provider, poim, statusFilter); // , null, timestamp);
				task.executeOnExecutor(getFetchStatusExecutor(provider.getAuthority()));
			}
		}
		return true;
	}

	private static class StatusFetcherCallable extends MTAsyncTask<Void, Void, POIStatus> {

		private static final String TAG = StatusLoader.class.getSimpleName() + '>' + StatusFetcherCallable.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		@NonNull
		private final WeakReference<Context> contextWR;
		@NonNull
		private final StatusProviderProperties statusProvider;
		@NonNull
		private final WeakReference<POIManager> poiWR;
		@NonNull
		private final WeakReference<StatusLoader.StatusLoaderListener> listenerWR;
		@NonNull
		private final StatusProviderContract.Filter statusFilter;

		StatusFetcherCallable(@Nullable Context context, @Nullable StatusLoader.StatusLoaderListener listener, @NonNull StatusProviderProperties statusProvider,
				@Nullable POIManager poim, @NonNull StatusProviderContract.Filter statusFilter) {
			this.contextWR = new WeakReference<>(context);
			this.listenerWR = new WeakReference<>(listener);
			this.statusProvider = statusProvider;
			this.poiWR = new WeakReference<>(poim);
			this.statusFilter = statusFilter;
		}

		@Override
		protected POIStatus doInBackgroundMT(Void... params) {
			try {
				return call();
			} catch (Exception e) {
				MTLog.w(this, e, "Error while running task!");
				return null;
			}
		}

		@Override
		protected void onPostExecute(@Nullable POIStatus result) {
			if (result == null) {
				return;
			}
			POIManager poim = this.poiWR.get();
			if (poim == null) {
				return;
			}
			boolean statusChanged = poim.setStatus(result);
			if (statusChanged) {
				StatusLoader.StatusLoaderListener listener = this.listenerWR.get();
				if (listener == null) {
					return;
				}
				listener.onStatusLoaded(result);
			}
		}

		public POIStatus call() {
			Context context = this.contextWR.get();
			if (context == null) {
				return null;
			}
			POIManager poim = this.poiWR.get();
			if (poim == null) {
				return null;
			}
			return DataSourceManager.findStatus(context, this.statusProvider.getAuthority(), this.statusFilter);
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

	public interface StatusLoaderListener {
		void onStatusLoaded(@NonNull POIStatus status);
	}
}
