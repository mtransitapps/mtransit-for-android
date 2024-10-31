package org.mtransit.android.task;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.ServiceUpdateProviderContract;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.ServiceUpdateProviderProperties;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.util.KeysManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ServiceUpdateLoader implements MTLog.Loggable {

	private static final String LOG_TAG = ServiceUpdateLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final Context appContext;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	@NonNull
	private final KeysManager keysManager;

	@Inject
	public ServiceUpdateLoader(
			@NonNull @ApplicationContext Context appContext,
			@NonNull DataSourcesRepository dataSourcesRepository,
			@NonNull KeysManager keysManager
	) {
		this.appContext = appContext;
		this.dataSourcesRepository = dataSourcesRepository;
		this.keysManager = keysManager;
	}

	private ThreadPoolExecutor fetchServiceUpdateExecutor;

	private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES > 1 ? RuntimeUtils.NUMBER_OF_CORES / 2 : 1;
	private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES > 1 ? RuntimeUtils.NUMBER_OF_CORES / 2 : 1;

	@NonNull
	private ThreadPoolExecutor getFetchServiceUpdateExecutor() {
		if (this.fetchServiceUpdateExecutor == null) {
			this.fetchServiceUpdateExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
					new LIFOBlockingDeque<>());
		}
		return fetchServiceUpdateExecutor;
	}

	private boolean isBusy() {
		return this.fetchServiceUpdateExecutor != null && this.fetchServiceUpdateExecutor.getActiveCount() > 0;
	}

	public void clearAllTasks() {
		if (this.fetchServiceUpdateExecutor != null) {
			this.fetchServiceUpdateExecutor.shutdown();
			this.fetchServiceUpdateExecutor = null;
		}
	}

	public boolean findServiceUpdate(@NonNull POIManager poim,
									 @NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter,
									 @Nullable ServiceUpdateLoaderListener listener,
									 boolean skipIfBusy) {
		if (skipIfBusy && isBusy()) {
			return false;
		}
		Set<ServiceUpdateProviderProperties> providers = this.dataSourcesRepository.getServiceUpdateProviders(poim.poi.getAuthority());
		if (!providers.isEmpty()) {
			for (ServiceUpdateProviderProperties provider : providers) {
				if (provider == null) {
					continue;
				}
				new ServiceUpdateFetcherCallable(this.appContext,
						listener,
						provider,
						poim,
						serviceUpdateFilter.appendProvidedKeys(this.keysManager.getKeysMap(provider.getAuthority()))
				).executeOnExecutor(getFetchServiceUpdateExecutor());
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation") // FIXME
	private static class ServiceUpdateFetcherCallable extends MTCancellableAsyncTask<Void, Void, ArrayList<ServiceUpdate>> {

		private static final String LOG_TAG = ServiceUpdateLoader.LOG_TAG + '>' + ServiceUpdateFetcherCallable.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final WeakReference<Context> contextWR;
		@NonNull
		private final ServiceUpdateProviderProperties serviceUpdateProvider;
		@NonNull
		private final WeakReference<POIManager> poiWR;
		@NonNull
		private final WeakReference<ServiceUpdateLoader.ServiceUpdateLoaderListener> listenerWR;
		@NonNull
		private final ServiceUpdateProviderContract.Filter serviceUpdateFilter;

		ServiceUpdateFetcherCallable(@Nullable Context context,
									 @Nullable ServiceUpdateLoader.ServiceUpdateLoaderListener listener,
									 @NonNull ServiceUpdateProviderProperties serviceUpdateProvider,
									 @Nullable POIManager poim,
									 @NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
			this.contextWR = new WeakReference<>(context);
			this.listenerWR = new WeakReference<>(listener);
			this.serviceUpdateProvider = serviceUpdateProvider;
			this.poiWR = new WeakReference<>(poim);
			this.serviceUpdateFilter = serviceUpdateFilter;
		}

		@Override
		protected ArrayList<ServiceUpdate> doInBackgroundNotCancelledMT(Void... params) {
			try {
				return call();
			} catch (Exception e) {
				MTLog.w(this, e, "Error while running task!");
				return null;
			}
		}

		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable ArrayList<ServiceUpdate> result) {
			if (result == null) {
				return;
			}
			final POIManager poim = this.poiWR.get();
			if (poim == null) {
				return;
			}
			poim.setServiceUpdates(result);
			final ServiceUpdateLoader.ServiceUpdateLoaderListener listener = this.listenerWR.get();
			if (listener == null) {
				return;
			}
			listener.onServiceUpdatesLoaded(poim.poi.getUUID(), poim.getServiceUpdatesOrNull());
		}

		@Nullable
		ArrayList<ServiceUpdate> call() {
			final Context context = this.contextWR.get();
			if (context == null) {
				return null;
			}
			final POIManager poim = this.poiWR.get();
			if (poim == null) {
				return null;
			}
			return DataSourceManager.findServiceUpdates(context, this.serviceUpdateProvider.getAuthority(), this.serviceUpdateFilter);
		}
	}

	@SuppressLint("UnknownNullness") // FIXME
	public static class LIFOBlockingDeque<E> extends LinkedBlockingDeque<E> implements MTLog.Loggable {

		private static final String TAG = LIFOBlockingDeque.class.getSimpleName();

		@NonNull
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

	public interface ServiceUpdateLoaderListener {
		void onServiceUpdatesLoaded(@NonNull String targetUUID, @Nullable List<ServiceUpdate> serviceUpdates);
	}
}
