package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.ServiceUpdateProviderContract;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.ServiceUpdateProviderProperties;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceUpdateLoader implements MTLog.Loggable {

	private static final String LOG_TAG = ServiceUpdateLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static ServiceUpdateLoader instance;

	public static ServiceUpdateLoader get() {
		if (instance == null) {
			instance = new ServiceUpdateLoader();
		}
		return instance;
	}

	private ServiceUpdateLoader() {
	}

	private ThreadPoolExecutor fetchServiceUpdateExecutor;

	private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES > 1 ? RuntimeUtils.NUMBER_OF_CORES / 2 : 1;
	private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES > 1 ? RuntimeUtils.NUMBER_OF_CORES / 2 : 1;

	public ThreadPoolExecutor getFetchServiceUpdateExecutor() {
		if (this.fetchServiceUpdateExecutor == null) {
			this.fetchServiceUpdateExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
					new LIFOBlockingDeque<>());
		}
		return fetchServiceUpdateExecutor;
	}

	public boolean isBusy() {
		return this.fetchServiceUpdateExecutor != null && this.fetchServiceUpdateExecutor.getActiveCount() > 0;
	}

	public void clearAllTasks() {
		if (this.fetchServiceUpdateExecutor != null) {
			this.fetchServiceUpdateExecutor.shutdown();
			this.fetchServiceUpdateExecutor = null;
		}
	}

	public boolean findServiceUpdate(Context context, POIManager poim, ServiceUpdateProviderContract.Filter serviceUpdateFilter,
									 ServiceUpdateLoader.ServiceUpdateLoaderListener listener, boolean skipIfBusy) {
		if (skipIfBusy && isBusy()) {
			return false;
		}
		HashSet<ServiceUpdateProviderProperties> providers = DataSourceProvider.get(context).getTargetAuthorityServiceUpdateProviders(poim.poi.getAuthority());
		if (providers != null) {
			if (providers.size() > 0) {
				ServiceUpdateProviderProperties provider = providers.iterator().next();
				ServiceUpdateFetcherCallable task = new ServiceUpdateFetcherCallable(context, listener, provider, poim, serviceUpdateFilter);
				task.executeOnExecutor(getFetchServiceUpdateExecutor());
			}
		}
		return true;
	}

	private static class ServiceUpdateFetcherCallable extends MTCancellableAsyncTask<Void, Void, ArrayList<ServiceUpdate>> {

		private static final String LOG_TAG = ServiceUpdateLoader.LOG_TAG + '>' + ServiceUpdateFetcherCallable.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private final WeakReference<Context> contextWR;
		private final ServiceUpdateProviderProperties serviceUpdateProvider;
		private final WeakReference<POIManager> poiWR;
		private final ServiceUpdateLoader.ServiceUpdateLoaderListener listener;
		private final ServiceUpdateProviderContract.Filter serviceUpdateFilter;

		public ServiceUpdateFetcherCallable(Context context, ServiceUpdateLoader.ServiceUpdateLoaderListener listener,
											ServiceUpdateProviderProperties serviceUpdateProvider, POIManager poim, ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
			this.contextWR = new WeakReference<>(context);
			this.listener = listener;
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
			POIManager poim = this.poiWR == null ? null : this.poiWR.get();
			if (poim == null) {
				return;
			}
			poim.setServiceUpdates(result);
			if (listener == null) {
				return;
			}
			listener.onServiceUpdatesLoaded(poim.poi.getUUID(), poim.getServiceUpdatesOrNull());
		}

		@Nullable
		ArrayList<ServiceUpdate> call() {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return null;
			}
			POIManager poim = this.poiWR == null ? null : this.poiWR.get();
			if (poim == null) {
				return null;
			}
			if (this.serviceUpdateFilter == null) {
				return null;
			}
			return DataSourceManager.findServiceUpdates(context, this.serviceUpdateProvider.getAuthority(), this.serviceUpdateFilter);
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

	public interface ServiceUpdateLoaderListener {
		void onServiceUpdatesLoaded(String targetUUID, ArrayList<ServiceUpdate> serviceUpdates);
	}
}
