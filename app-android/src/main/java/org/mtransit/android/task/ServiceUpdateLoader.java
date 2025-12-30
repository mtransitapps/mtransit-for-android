package org.mtransit.android.task;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.RouteDirectionManager;
import org.mtransit.android.data.RouteManager;
import org.mtransit.android.data.ServiceUpdateProviderProperties;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.util.KeysManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
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
		return getActiveCount() > 0;
	}

	private int getActiveCount() {
		return this.fetchServiceUpdateExecutor == null ? 0 : this.fetchServiceUpdateExecutor.getActiveCount();
	}

	private long getTaskCount() {
		return this.fetchServiceUpdateExecutor == null ? 0 : this.fetchServiceUpdateExecutor.getTaskCount();
	}

	public void clearAllTasks() {
		if (this.fetchServiceUpdateExecutor != null) {
			this.fetchServiceUpdateExecutor.shutdown();
			this.fetchServiceUpdateExecutor = null;
		}
	}

	public boolean findServiceUpdate(@NonNull POIManager poim,
									 @NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter,
									 @Nullable Collection<ServiceUpdateLoaderListener> listeners,
									 boolean skipIfBusy) {
		// SUPPORTED BY ALL SERVICE UPDATE PROVIDERS
		return findServiceUpdate(
				poim.poi.getAuthority(),
				poim.poi.getUUID(),
				poim,
				serviceUpdateFilter,
				listeners,
				skipIfBusy
		);
	}

	private static final Collection<String> ROUTE_DIRECTION_NOT_SUPPORTED;

	static {
		Set<String> collection = new HashSet<>();
		collection.add("org.mtransit.android.ca_montreal_stm_subway" + (BuildConfig.DEBUG ? ".debug" : "")); // + ".stminfo"
		ROUTE_DIRECTION_NOT_SUPPORTED = collection;
	}

	public boolean findServiceUpdate(@NonNull RouteDirectionManager routeDirectionM,
									 @NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter,
									 @Nullable Collection<ServiceUpdateLoaderListener> listeners,
									 boolean skipIfBusy) {
		if (ROUTE_DIRECTION_NOT_SUPPORTED.contains(routeDirectionM.getAuthority())) {
			return true; // not skipped // not supported
		}
		return findServiceUpdate(
				routeDirectionM.getAuthority(),
				routeDirectionM.getRouteDirection().getUUID(),
				routeDirectionM,
				serviceUpdateFilter,
				listeners,
				skipIfBusy
		);
	}

	private static final Collection<String> ROUTE_NOT_SUPPORTED;

	static {
		Set<String> collection = new HashSet<>();
		collection.add("org.mtransit.android.ca_laval_stl_bus" + (BuildConfig.DEBUG ? ".debug" : "")); // + ".nextbus"
		collection.add("org.mtransit.android.ca_montreal_stm_bus" + (BuildConfig.DEBUG ? ".debug" : "")); // + ".stminfoapi"
		ROUTE_NOT_SUPPORTED = collection;
	}

	public boolean findServiceUpdate(@NonNull RouteManager routeM,
									 @NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter,
									 @Nullable Collection<ServiceUpdateLoaderListener> listeners,
									 boolean skipIfBusy) {
		if (ROUTE_NOT_SUPPORTED.contains(routeM.getAuthority())) {
			return true; // not skipped // not supported
		}
		return findServiceUpdate(
				routeM.getAuthority(),
				routeM.getRoute().getUUID(),
				routeM,
				serviceUpdateFilter,
				listeners,
				skipIfBusy
		);
	}

	private boolean findServiceUpdate(@NonNull String targetAuthority,
									  @NonNull String targetUUID,
									  @NonNull ServiceUpdateLoaderListener mainListener,
									  @NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter,
									  @Nullable Collection<ServiceUpdateLoaderListener> listeners,
									  boolean skipIfBusy) {
		if (skipIfBusy && isBusy()) {
			return false;
		}
		final Collection<ServiceUpdateProviderProperties> providers = this.dataSourcesRepository.getServiceUpdateProviders(targetAuthority);
		if (providers.isEmpty()) {
			return true;
		}
		for (ServiceUpdateProviderProperties provider : providers) {
			if (provider == null) {
				continue;
			}
			new ServiceUpdateFetcherCallable(this.appContext,
					listeners,
					provider,
					targetUUID,
					mainListener,
					serviceUpdateFilter.appendProvidedKeys(this.keysManager.getKeysMap(provider.getAuthority()))
			).executeOnExecutor(getFetchServiceUpdateExecutor());
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
		private final String targetUUID;
		@NonNull
		private final WeakReference<ServiceUpdateLoaderListener> mainListenerWR;
		@NonNull
		private final WeakHashMap<ServiceUpdateLoaderListener, Object> listenerWR;
		@NonNull
		private final ServiceUpdateProviderContract.Filter serviceUpdateFilter;

		ServiceUpdateFetcherCallable(@Nullable Context context,
									 @Nullable Collection<ServiceUpdateLoaderListener> listeners,
									 @NonNull ServiceUpdateProviderProperties serviceUpdateProvider,
									 @NonNull String targetUUID,
									 @Nullable ServiceUpdateLoaderListener mainListener,
									 @NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
			this.contextWR = new WeakReference<>(context);
			this.listenerWR = new WeakHashMap<>();
			if (listeners != null) {
				for (ServiceUpdateLoaderListener listener : listeners) {
					this.listenerWR.put(listener, null);
				}
			}
			this.serviceUpdateProvider = serviceUpdateProvider;
			this.targetUUID = targetUUID;
			this.mainListenerWR = new WeakReference<>(mainListener);
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
			final ServiceUpdateLoaderListener mainListener = this.mainListenerWR.get();
			if (mainListener == null) {
				return;
			}
			mainListener.onServiceUpdatesLoaded(targetUUID, result);
			for (ServiceUpdateLoaderListener listener : this.listenerWR.keySet()) {
				listener.onServiceUpdatesLoaded(targetUUID, result);
			}
		}

		@Nullable
		ArrayList<ServiceUpdate> call() {
			final Context context = this.contextWR.get();
			if (context == null) {
				return null;
			}
			final ServiceUpdateLoaderListener mainListener = this.mainListenerWR.get();
			if (mainListener == null) {
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
