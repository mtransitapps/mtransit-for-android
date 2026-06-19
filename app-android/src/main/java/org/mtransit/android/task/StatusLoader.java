package org.mtransit.android.task;

import android.annotation.SuppressLint;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.provider.status.StatusProviderContract;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.StatusProviderProperties;
import org.mtransit.android.datasource.DataSourceRequestManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.util.KeysManager;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StatusLoader implements MTLog.Loggable {

	private static final String LOG_TAG = StatusLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	@NonNull
	private final DataSourceRequestManager dataSourceRequestManager;

	@NonNull
	private final KeysManager keysManager;

	@Inject
	public StatusLoader(
			@NonNull DataSourcesRepository dataSourcesRepository,
			@NonNull DataSourceRequestManager dataSourceRequestManager,
			@NonNull KeysManager keysManager
	) {
		this.dataSourcesRepository = dataSourcesRepository;
		this.dataSourceRequestManager = dataSourceRequestManager;
		this.keysManager = keysManager;
	}

	@NonNull
	private final HashMap<String, ThreadPoolExecutor> fetchStatusExecutors = new HashMap<>();

	private static final int MAX_CORE = 2;
	private static final int CORE_POOL_SIZE = Math.min(RuntimeUtils.NUMBER_OF_CORES, MAX_CORE) + 1;
	private static final int MAX_POOL_SIZE = Math.min(RuntimeUtils.NUMBER_OF_CORES, MAX_CORE) * 2 + 1;

	@NonNull
	private ThreadPoolExecutor getFetchStatusExecutor(@NonNull String statusProviderAuthority) {
		ThreadPoolExecutor threadPoolExecutor = this.fetchStatusExecutors.get(statusProviderAuthority);
		if (threadPoolExecutor == null) {
			threadPoolExecutor = new ThreadPoolExecutor(
					CORE_POOL_SIZE,
					MAX_POOL_SIZE,
					0L,
					TimeUnit.MILLISECONDS,
					new LIFOBlockingDeque<>()
			);
			this.fetchStatusExecutors.put(statusProviderAuthority, threadPoolExecutor);
		}
		return threadPoolExecutor;
	}

	private boolean isBusy() {
		for (Map.Entry<String, ThreadPoolExecutor> fetchStatusExecutor : this.fetchStatusExecutors.entrySet()) {
			if (fetchStatusExecutor.getValue() != null && fetchStatusExecutor.getValue().getActiveCount() > 0) {
				return true;
			}
		}
		return false;
	}

	public void clearAllTasks() {
		final Iterator<Map.Entry<String, ThreadPoolExecutor>> it = this.fetchStatusExecutors.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<String, ThreadPoolExecutor> fetchStatusExecutor = it.next();
			if (fetchStatusExecutor.getValue() != null) {
				fetchStatusExecutor.getValue().shutdown();
			}
			it.remove();
		}
	}

	@AnyThread
	public boolean findStatus(
			@NonNull POIManager poim,
			@NonNull StatusProviderContract.Filter statusFilter,
			@Nullable StatusLoader.StatusLoaderListener listener,
			boolean skipIfBusy
	) {
		if (skipIfBusy && isBusy()) {
			return false; // skipped
		}
		final Collection<StatusProviderProperties> providers = this.dataSourcesRepository.getStatusProviders(poim.poi.getAuthority());
		if (!providers.isEmpty()) {
			for (StatusProviderProperties provider : providers) {
				if (provider == null) continue;
				new StatusFetcherCallable(
						this.dataSourceRequestManager,
						listener,
						provider,
						poim,
						statusFilter.appendProvidedKeys(this.keysManager.getKeysMap(provider.getAuthority()))
				).executeOnExecutor(getFetchStatusExecutor(provider.getAuthority()));
			}
		}
		return true; // not skipped
	}

	@SuppressLint("DeprecatedCall")
	@SuppressWarnings("deprecation")
	private static class StatusFetcherCallable extends MTCancellableAsyncTask<Void, POIStatus, POIStatus> {

		private static final String LGO_TAG = StatusLoader.class.getSimpleName() + '>' + StatusFetcherCallable.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LGO_TAG;
		}

		@NonNull
		private final DataSourceRequestManager dataSourceRequestManager;
		@NonNull
		private final StatusProviderProperties statusProvider;
		@NonNull
		private final WeakReference<POIManager> poiWR;
		@NonNull
		private final WeakReference<StatusLoader.StatusLoaderListener> listenerWR;
		@NonNull
		private final StatusProviderContract.Filter statusFilter;

		StatusFetcherCallable(
				@NonNull DataSourceRequestManager dataSourceRequestManager,
				@Nullable StatusLoader.StatusLoaderListener listener,
				@NonNull StatusProviderProperties statusProvider,
				@Nullable POIManager poim,
				@NonNull StatusProviderContract.Filter statusFilter
		) {
			this.dataSourceRequestManager = dataSourceRequestManager;
			this.listenerWR = new WeakReference<>(listener);
			this.statusProvider = statusProvider;
			this.poiWR = new WeakReference<>(poim);
			this.statusFilter = statusFilter;
		}

		@Override
		public boolean isCancelledMT() {
			if (super.isCancelledMT()) return true;
			if (this.poiWR.get() == null && this.listenerWR.get() == null) return true;
			return false;
		}

		@Override
		protected POIStatus doInBackgroundNotCancelledMT(Void... params) {
			try {
				final POIManager poim = this.poiWR.get();
				if (poim == null) return null;
				// 1 - cache only
				this.statusFilter.setCacheOnly(true);
				//noinspection DiscouragedApi
				final POIStatus cacheOnlyStatus = dataSourceRequestManager.findStatusSync(this.statusProvider.getAuthority(), this.statusFilter, this::isCancelledMT);
				if (isCancelledMT()) return cacheOnlyStatus;
				publishProgress(cacheOnlyStatus);
				// 2 - not cache only
				this.statusFilter.setCacheOnly(false);
				//noinspection DiscouragedApi
				return dataSourceRequestManager.findStatusSync(this.statusProvider.getAuthority(), this.statusFilter, this::isCancelledMT);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while running task!");
				return null;
			}
		}

		@MainThread
		@Override
		protected void onProgressUpdateNotCancelledMT(@Nullable POIStatus... results) {
			if (results == null) return;
			for (POIStatus result : results) {
				if (result == null) continue;
				onStatusLoaded(result);
			}
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable POIStatus result) {
			onStatusLoaded(result);
		}

		private void onStatusLoaded(@Nullable POIStatus result) {
			if (result == null) return;
			final POIManager poim = this.poiWR.get();
			if (poim == null) return;
			final boolean statusChanged = poim.setStatus(result); // filter no data or not useful or older than current status
			if (statusChanged) {
				final StatusLoaderListener listener = this.listenerWR.get();
				if (listener == null) return;
				listener.onStatusLoaded(result);
			}
		}
	}

	public static class LIFOBlockingDeque<E> extends LinkedBlockingDeque<E> implements MTLog.Loggable {

		private static final String LOG_TAG = LIFOBlockingDeque.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final long serialVersionUID = -2614488162524238781L;

		@Override
		public boolean offer(@NonNull E e) {
			return super.offerFirst(e);
		}

		@Override
		public boolean offer(@NonNull E e, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
			return super.offerFirst(e, timeout, unit);
		}

		@Override
		public boolean add(@NonNull E e) {
			return super.offerFirst(e);
		}

		@Override
		public void put(@NonNull E e) throws InterruptedException {
			super.putFirst(e);
		}
	}

	public interface StatusLoaderListener {
		void onStatusLoaded(@NonNull POIStatus status);
	}
}
