package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule.ScheduleStatusFilter;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.NewsProviderContract;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.NewsProviderProperties;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.ScheduleProviderProperties;
import org.mtransit.android.di.Injection;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.provider.sensor.MTSensorManager;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.task.NearbyPOIListLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTOnClickListener;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.ui.view.POINewsViewController;
import org.mtransit.android.ui.view.POIServiceUpdateViewController;
import org.mtransit.android.ui.view.POIStatusDetailViewController;
import org.mtransit.android.ui.view.POIViewController;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.DegreeUtils;
import org.mtransit.android.util.FragmentUtils;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.android.util.LoaderUtils;
import org.mtransit.android.util.MapUtils;
import org.mtransit.android.util.UITimeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class POIFragment extends ABFragment implements
		LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		POIViewController.POIDataProvider,
		MTActivityWithLocation.UserLocationListener,
		SensorEventListener,
		MTSensorManager.CompassListener,
		MTSensorManager.SensorTaskCompleted,
		FavoriteManager.FavoriteUpdateListener,
		UITimeUtils.TimeChangedReceiver.TimeChangedListener,
		MapViewController.MapMarkerProvider,
		IContext,
		IAdManager.RewardedAdListener,
		MapViewController.MapListener {

	private static final String LOG_TAG = POIFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		if (!TextUtils.isEmpty(this.uuid)) {
			return LOG_TAG + "-" + this.uuid;
		}
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "POI";

	@NonNull
	@Override
	public String getScreenName() {
		if (!TextUtils.isEmpty(this.uuid)) {
			return TRACKING_SCREEN_NAME + "/" + this.uuid;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";

	@NonNull
	public static POIFragment newInstance(@NonNull String uuid, @NonNull String authority, @Nullable AgencyProperties optAgency, @Nullable POIManager
			optPoim) {
		POIFragment f = new POIFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		f.agency = optAgency;
		args.putString(EXTRA_POI_UUID, uuid);
		f.uuid = uuid;
		f.poim = optPoim;
		f.setPOIProperties();
		f.setArguments(args);
		return f;
	}

	private String authority;

	@Nullable
	private AgencyProperties agency;

	private final MapViewController mapViewController =
			new MapViewController(LOG_TAG, this, this, false, true, false, false, false, false, 32, true, false, true, true, false);

	@NonNull
	private final LocationPermissionProvider locationPermissionProvider;
	@NonNull
	private final MTSensorManager sensorManager;
	@NonNull
	private final IAdManager adManager;

	public POIFragment() {
		super();
		this.locationPermissionProvider = Injection.providesLocationPermissionProvider();
		this.sensorManager = Injection.providesSensorManager();
		this.adManager = Injection.providesAdManager();
	}

	private boolean hasAgency() {
		if (this.agency == null) {
			initAgencyAsync();
			return false;
		}
		return true;
	}

	private void initAgencyAsync() {
		if (this.loadAgencyTask != null && this.loadAgencyTask.getStatus() == LoadAgencyTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadAgencyTask = new LoadAgencyTask(this);
		TaskUtils.execute(this.loadAgencyTask);
	}

	private LoadAgencyTask loadAgencyTask = null;

	private static class LoadAgencyTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, POIFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return POIFragment.class.getSimpleName() + ">" + LoadAgencyTask.class.getSimpleName();
		}

		LoadAgencyTask(POIFragment poiFragment) {
			super(poiFragment);
		}

		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull POIFragment poiFragment, Void... params) {
			return poiFragment.initAgencySync();
		}

		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull POIFragment poiFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				poiFragment.applyNewAgency();
			}
		}
	}

	private AgencyProperties getAgencyOrNull() {
		if (!hasAgency()) {
			return null;
		}
		return this.agency;
	}

	private boolean initAgencySync() {
		if (this.agency != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.authority)) {
			this.agency = DataSourceProvider.get(getContext()).getAgency(getContext(), this.authority);
		}
		return this.agency != null;
	}

	private void applyNewAgency() {
		if (this.agency == null) {
			return;
		}
		final Context context = getContext();
		if (context == null) {
			return;
		}
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIStatusDetailViewController.updateView(context, getPOIStatusView(getView()), poim, this);
		}
		if (getAbController() != null) {
			getAbController().setABTitle(this, getABTitle(context), false);
			getAbController().setABReady(this, isABReady(), true);
		}
	}

	private void resetAgency() {
		this.agency = null;
	}

	private String uuid;

	@Nullable
	private POIManager poim;

	private boolean hasPoim() {
		if (this.poim == null) {
			initPoimAsync();
			return false;
		}
		return true;
	}

	private void initPoimAsync() {
		if (this.loadPoimTask != null && this.loadPoimTask.getStatus() == LoadPoimTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadPoimTask = new LoadPoimTask(this);
		TaskUtils.execute(this.loadPoimTask);
	}

	private LoadPoimTask loadPoimTask = null;

	private static class LoadPoimTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, POIFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return POIFragment.class.getSimpleName() + ">" + LoadPoimTask.class.getSimpleName();
		}

		LoadPoimTask(@NonNull POIFragment poiFragment) {
			super(poiFragment);
		}

		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull POIFragment poiFragment, @Nullable Void... params) {
			return poiFragment.initPoimSync();
		}

		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull POIFragment poiFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				poiFragment.applyNewPoim();
			}
		}
	}

	@Nullable
	private POIManager getPoimOrNull() {
		if (!hasPoim()) {
			return null;
		}
		return this.poim;
	}

	private boolean initPoimSync() {
		if (this.poim != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.uuid) && !TextUtils.isEmpty(this.authority)) {
			this.poim = DataSourceManager.findPOI(requireContext(), this.authority, POIProviderContract.Filter.getNewUUIDFilter(this.uuid));
		}
		return this.poim != null;
	}

	private void setPOIProperties() {
		if (this.poim != null) {
			this.poim.setInFocus(true);
			this.poim.setScheduleMaxDataRequests(ScheduleStatusFilter.DATA_REQUEST_MONTHS);
			this.poim.resetLastFindTimestamps();
		}
	}

	private void applyNewPoim() {
		if (this.poim == null) {
			return;
		}
		final Context context = getContext();
		if (context == null) {
			return;
		}
		setPOIProperties();
		if (this.userLocation != null) {
			LocationUtils.updateDistanceWithString(context, this.poim, this.userLocation);
		}
		if (this.adapter != null) {
			this.adapter.clear();
		}
		resetFavorite();
		View view = getView();
		this.mapViewController.notifyMarkerChanged(this);
		this.mapViewController.showMap(view);
		POIViewController.updateView(context, getPOIView(view), this.poim, this);
		POIStatusDetailViewController.updateView(context, getPOIStatusView(view), this.poim, this);
		POIServiceUpdateViewController.updateView(context, getPOIServiceUpdateView(view), this.poim, this);
		POINewsViewController.updateView(context, getPOINewsView(view), getNewsOrNull());
		setupRTSFullScheduleBtn(view);
		setupMoreNewsButton(view);
		setupMoreNearbyButton(view);
		if (getActivity() != null) {
			getActivity().invalidateOptionsMenu(); // add/remove star from action bar
		}
		setupNearbyList();
	}

	@Nullable
	private ArrayList<News> news = null;

	@Nullable
	private ArrayList<News> getNewsOrNull() {
		if (!hasNews()) {
			return null;
		}
		return this.news;
	}

	private boolean hasNews() {
		if (this.news == null) {
			initNewsAsync();
			return false;
		}
		return true;
	}

	private void initNewsAsync() {
		if (this.loadNewsTask != null && this.loadNewsTask.getStatus() == LoadNewsTask.Status.RUNNING) {
			return;
		}
		if (!hasPoim()) {
			return;
		}
		this.loadNewsTask = new LoadNewsTask(this);
		TaskUtils.execute(this.loadNewsTask);
	}

	@Nullable
	private LoadNewsTask loadNewsTask = null;

	private static class LoadNewsTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, POIFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return POIFragment.class.getSimpleName() + ">" + LoadNewsTask.class.getSimpleName();
		}

		LoadNewsTask(POIFragment poiFragment) {
			super(poiFragment);
		}

		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull POIFragment poiFragment, Void... params) {
			return poiFragment.initNewsSync();
		}

		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull POIFragment poiFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				poiFragment.applyNewNews();
			}
		}
	}

	private boolean initNewsSync() {
		if (this.news != null) {
			return false;
		}
		POIManager poim = getPoimOrNull();
		if (poim == null) {
			return false;
		}
		Context context = getContext();
		if (context == null) {
			return false;
		}
		this.news = new ArrayList<>();
		HashSet<NewsProviderProperties> poiNewsProviders = DataSourceProvider.get(context).getTargetAuthorityNewsProviders(poim.poi.getAuthority());
		if (CollectionUtils.getSize(poiNewsProviders) == 0) {
			return true; // no news, need to apply
		}
		long nowInMs = UITimeUtils.currentTimeMillis();
		final NewsProviderContract.Filter newsFilter = NewsProviderContract.Filter
				.getNewTargetFilter(poim.poi)
				.setMinCreatedAtInMs(nowInMs - TimeUnit.DAYS.toMillis(14L));
		ArrayList<News> allNews = new ArrayList<>();
		if (poiNewsProviders != null) {
			for (NewsProviderProperties poiNewsProvider : poiNewsProviders) {
				ArrayList<News> providerNews = DataSourceManager.findNews(context, poiNewsProvider.getAuthority(),
						newsFilter);
				if (providerNews != null) {
					allNews.addAll(providerNews);
				}
			}
		}
		if (CollectionUtils.getSize(allNews) == 0) {
			return true; // no news, need to apply
		}
		CollectionUtils.sort(allNews, News.NEWS_SEVERITY_COMPARATOR);
		int noteworthiness = 1;
		while (this.news.isEmpty() && noteworthiness < 7) {
			for (News news : allNews) {
				if (news.getCreatedAtInMs() + news.getNoteworthyInMs() * noteworthiness < nowInMs) {
					continue; // news too old to be worthy
				}
				this.news.add(0, news);
				break;
			}
			noteworthiness++;
		}
		return true; // need to apply
	}

	private void applyNewNews() {
		if (this.news == null) {
			return;
		}
		final Context context = getContext();
		if (context == null) {
			return;
		}
		POINewsViewController.updateView(context, getPOINewsView(getView()), this.news);
	}

	@Nullable
	@Override
	public POIManager getClosestPOI() {
		return this.poim;
	}

	@Nullable
	@Override
	public POIManager getPOI(@Nullable String uuid) {
		return this.poim != null && this.poim.poi.getUUID().equals(uuid) ? this.poim : null;
	}

	@Nullable
	@Override
	public Collection<POIManager> getPOIs() {
		if (this.poim == null) {
			return null;
		}
		return Collections.singletonList(this.poim);
	}

	@Nullable
	@Override
	public Collection<MapViewController.POIMarker> getPOMarkers() {
		return null;
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.mapViewController.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCameraChange(@NonNull LatLngBounds latLngBounds) {
		// DO NOTHING
	}

	@Override
	public void onMapClick(@Nullable LatLng position) {
		if (!FragmentUtils.isFragmentReady(this)) {
			return;
		}
		POIManager poim = getPoimOrNull();
		if (poim == null) {
			return;
		}
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		((MainActivity) activity).addFragmentToStack( //
				MapFragment.newInstance(LocationUtils.getNewLocation(poim.getLat(), poim.getLng()), poim.poi.getUUID(), poim.poi.getDataSourceTypeId()), this);
	}

	private void resetPoim() {
		this.poim = null;
	}

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
		this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.permissionsGranted(this));
		this.mapViewController.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.mapViewController.onDetach();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		restoreInstanceState(savedInstanceState, getArguments());
		this.mapViewController.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_poi, container, false);
		this.mapViewController.onCreateView(view, savedInstanceState);
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupView(view);
		this.mapViewController.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (!TextUtils.isEmpty(this.uuid)) {
			outState.putString(EXTRA_POI_UUID, this.uuid);
		}
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AUTHORITY, this.authority);
		}
		this.mapViewController.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (newAuthority != null && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetAgency();
		}
		String newUuid = BundleUtils.getString(EXTRA_POI_UUID, bundles);
		if (newUuid != null && !newUuid.equals(this.uuid)) {
			this.uuid = newUuid;
			resetPoim();
		}
	}

	@Nullable
	private POIArrayAdapter adapter;

	private void setupNearbyList() {
		if (this.adapter != null && this.adapter.isInitialized()) {
			showNearbyList();
			return;
		}
		hideNearbyList(true);
		if (Constants.FORCE_NEARBY_POI_LIST_OFF) {
			return;
		}
		if (hasPoim()) {
			LoaderUtils.restartLoader(this, NEARBY_POIS_LOADER, null, this);
		}
	}

	private static final int NEARBY_POIS_LOADER = 0;
	private LocationUtils.AroundDiff ad = LocationUtils.getNewDefaultAroundDiff();

	@NonNull
	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case NEARBY_POIS_LOADER:
			POIManager poim = getPoimOrNull();
			if (TextUtils.isEmpty(this.authority) || poim == null) {
				//noinspection deprecation // FIXME
				CrashUtils.w(this, "onCreateLoader() > agency or poi not available yet (agency:%s|poi:%s).", this.authority, poim);
				//noinspection ConstantConditions // FIXME
				return null;
			}
			Context context = getContext();
			if (context == null) {
				//noinspection deprecation // FIXME
				CrashUtils.w(this, "onCreateLoader() > skip (no context)");
				//noinspection ConstantConditions // FIXME
				return null;
			}
			return new NearbyPOIListLoader(context, poim.poi.getLat(), poim.poi.getLng(), this.ad.aroundDiff,
					LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS, LocationUtils.MAX_POI_NEARBY_POIS_LIST, false, true, this.authority);
		default:
			//noinspection deprecation // FIXME
			CrashUtils.w(this, "Loader id '%s' unknown!", id);
			//noinspection ConstantConditions // FIXME
			return null;
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@Override
	public void onLoadFinished(@NonNull Loader<ArrayList<POIManager>> loader, @Nullable ArrayList<POIManager> data) {
		POIManager poim = getPoimOrNull();
		if (CollectionUtils.getSize(data) < LocationUtils.MIN_NEARBY_LIST && poim != null //
				&& !LocationUtils.searchComplete(poim.poi.getLat(), poim.poi.getLng(), this.ad.aroundDiff)) {
			LocationUtils.incAroundDiff(this.ad);
			LoaderUtils.restartLoader(this, NEARBY_POIS_LOADER, null, this);
			return;
		}
		if (poim != null && data != null) {
			int i = 0;
			Iterator<POIManager> it = data.iterator();
			while (it.hasNext()) {
				if (it.next().poi.getUUID().equals(poim.poi.getUUID())) {
					it.remove();
					continue;
				}
				if (i >= LocationUtils.MAX_POI_NEARBY_POIS_LIST) {
					it.remove();
				}
				i++;
			}
		}
		if (this.adapter != null) {
			this.adapter.setPois(data);
			this.adapter.updateDistanceNowAsync(this.userLocation);
			this.adapter.initManual();
			if (this.adapter.getPoisCount() > 0) {
				showNearbyList();
			} else {
				hideNearbyList(false);
			}
		}
	}

	private void hideNearbyList(boolean invisibleInsteadOfGone) {
		View view = getView();
		if (view == null) {
			return;
		}
		view.findViewById(R.id.poi_nearby_pois_title).setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
		view.findViewById(R.id.poi_nearby_pois_list).setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
	}

	private void showNearbyList() {
		View view = getView();
		if (view == null) {
			return;
		}
		view.findViewById(R.id.poi_nearby_pois_title).setVisibility(View.VISIBLE);
		view.findViewById(R.id.poi_nearby_pois_list).setVisibility(View.VISIBLE);
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setTag(getLogTag());
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter != null) {
			this.adapter.setManualScrollView(view.findViewById(R.id.scrollview));
			this.adapter.setManualLayout(view.findViewById(R.id.poi_nearby_pois_list));
		}
	}

	private void setupRTSFullScheduleBtn(View view) {
		if (view == null) {
			return;
		}
		View rtsScheduleBtn = view.findViewById(R.id.fullScheduleBtn);
		if (rtsScheduleBtn != null) {
			Collection<ScheduleProviderProperties> scheduleProviders = //
					DataSourceProvider.get(getContext()).getTargetAuthorityScheduleProviders(this.authority);
			if (CollectionUtils.getSize(scheduleProviders) == 0) {
				rtsScheduleBtn.setVisibility(View.GONE);
			} else {
				rtsScheduleBtn.setOnClickListener(new MTOnClickListener() {
					@Override
					public void onClickMT(View view) {
						POIManager poim = getPoimOrNull();
						if (poim == null || !(poim.poi instanceof RouteTripStop)) {
							MTLog.w(POIFragment.this, "onClick() > skip (no poi or not RTS)");
							return;
						}
						FragmentActivity activity = getActivity();
						if (activity == null) {
							MTLog.w(POIFragment.this, "onClick() > skip (no activity)");
							return;
						}
						((MainActivity) activity).addFragmentToStack( //
								ScheduleFragment.newInstance( //
										POIFragment.this.uuid, POIFragment.this.authority, (RouteTripStop) poim.poi, poim.getColor(activity)), //
								POIFragment.this);
					}
				});
				rtsScheduleBtn.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setupMoreNewsButton(View view) {
		if (view == null) {
			return;
		}
		View newsView = view.findViewById(R.id.poi_news);
		View moreBtn = newsView == null ? null : newsView.findViewById(R.id.moreBtn);
		if (moreBtn != null) {
			moreBtn.setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					POIManager poim = getPoimOrNull();
					if (poim == null) {
						return;
					}
					FragmentActivity activity = getActivity();
					if (activity == null) {
						MTLog.w(POIFragment.this, "onClick() > skip (no activity)");
						return;
					}
					Integer colorInt = poim.getColor(getContext());
					String subtitle = POIManager.getOneLineDescription(getContext(), poim.poi);
					((MainActivity) activity).addFragmentToStack( //
							NewsFragment.newInstance( //
									colorInt, subtitle, ArrayUtils.asArrayList(poim.poi.getAuthority()), null,
									NewsProviderContract.Filter.getNewTargetFilter(poim.poi).getTargets()), //
							POIFragment.this);
				}
			});
			moreBtn.setVisibility(View.VISIBLE);
		}
	}

	private void setupMoreNearbyButton(View view) {
		if (view == null) {
			return;
		}
		View moreBtn = view.findViewById(R.id.poi_nearby_pois_title).findViewById(R.id.moreBtn);
		if (moreBtn != null) {
			moreBtn.setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					Activity activity = getActivity();
					if (activity == null) {
						return;
					}
					POIManager poim = getPoimOrNull();
					if (poim == null) {
						return;
					}
					Integer optTypeId = null;
					AgencyProperties agency = getAgencyOrNull();
					if (agency != null) {
						optTypeId = agency.getType().getId();
					}
					((MainActivity) activity).addFragmentToStack( //
							NearbyFragment.newFixedOnInstance( //
									optTypeId, poim.getLat(), poim.getLng(), POIManager.getOneLineDescription(getContext(), poim.poi),
									poim.getColor(getContext())), POIFragment.this);
				}
			});
			moreBtn.setVisibility(View.VISIBLE);
		}
	}

	private View getPOIStatusView(View view) {
		POIManager poim = getPoimOrNull();
		if (view == null || poim == null) {
			return null;
		}
		if (view.findViewById(R.id.poi_status_detail) == null) { // IF NOT present/inflated DO
			Integer layoutResId = POIStatusDetailViewController.getLayoutResId(poim);
			if (layoutResId != null) {
				((ViewStub) view.findViewById(R.id.poi_status_detail_stub)).setLayoutResource(layoutResId);
				((ViewStub) view.findViewById(R.id.poi_status_detail_stub)).inflate(); // inflate
				setupRTSFullScheduleBtn(view);
			}
		}
		return view.findViewById(R.id.poi_status_detail);
	}

	private View getPOIServiceUpdateView(View view) {
		if (view == null) {
			return null;
		}
		if (view.findViewById(R.id.poi_service_update) == null) { // IF NOT present/inflated DO
			int layoutResId = POIServiceUpdateViewController.getLayoutResId();
			((ViewStub) view.findViewById(R.id.poi_service_update_stub)).setLayoutResource(layoutResId);
			((ViewStub) view.findViewById(R.id.poi_service_update_stub)).inflate(); // inflate
		}
		return view.findViewById(R.id.poi_service_update);
	}

	@Nullable
	private View getPOINewsView(@Nullable View view) {
		if (view == null) {
			return null;
		}
		if (view.findViewById(R.id.poi_news) == null) { // IF NOT present/inflated DO
			int layoutResId = POINewsViewController.getLayoutResId();
			((ViewStub) view.findViewById(R.id.poi_news_stub)).setLayoutResource(layoutResId);
			((ViewStub) view.findViewById(R.id.poi_news_stub)).inflate(); // inflate
			view.findViewById(R.id.the_poi_news).setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					ArrayList<News> news = getNewsOrNull();
					if (news == null || news.size() == 0) {
						return;
					}
					News lastNews = news.get(0);
					if (lastNews == null) {
						return;
					}
					Activity activity = getActivity();
					if (activity == null) {
						return;
					}
					((MainActivity) activity).addFragmentToStack( //
							NewsDetailsFragment.newInstance(lastNews.getUUID(), lastNews.getAuthority(), lastNews), //
							POIFragment.this);
				}
			});
		}
		return view.findViewById(R.id.poi_news);
	}

	@Nullable
	private View getPOIRewardedAdView(@Nullable View view) {
		if (view == null) {
			return null;
		}
		if (view.findViewById(R.id.poi_rewarded_ad) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.poi_rewarded_ad_stub)).inflate(); // inflate

			view.findViewById(R.id.rewardedAdsBtn).setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					onRewardedAdButtonClick(view.getContext());
				}
			});
		}
		return view.findViewById(R.id.poi_rewarded_ad);
	}

	private void onRewardedAdButtonClick(@NonNull Context context) {
		final Activity activity = getActivity();
		if (activity == null) {
			MTLog.w(this, "onRewardedAdButtonClick() > skip (no view or no activity)");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_default_failure_message);
			return;
		}
		if (!this.adManager.isRewardedAdAvailableToShow()) {
			MTLog.w(this, "onRewardedAdButtonClick() > skip (no ad available)");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_not_ready);
			return;
		}
		this.adManager.showRewardedAd(this);
		final View view = getView();
		if (view != null) {
			view.findViewById(R.id.rewardedAdsBtn).setEnabled(false);
		}
		this.adManager.showRewardedAd(POIFragment.this);
	}

	@Nullable
	private View getPOIView(@Nullable View view) {
		POIManager poim = getPoimOrNull();
		if (view == null || poim == null) {
			return null;
		}
		if (view.findViewById(R.id.this_poi) == null) { // IF NOT present/inflated DO
			int layoutResId = POIViewController.getLayoutResId(poim);
			((ViewStub) view.findViewById(R.id.this_poi_stub)).setLayoutResource(layoutResId);
			((ViewStub) view.findViewById(R.id.this_poi_stub)).inflate(); // inflate
		}
		return view.findViewById(R.id.this_poi);
	}

	@Override
	public void onStatusLoaded(@NonNull POIStatus status) {
		View view = getView();
		if (view == null) {
			return;
		}
		POIViewController.updatePOIStatus(view.getContext(), getPOIView(view), status, this);
		POIStatusDetailViewController.updatePOIStatus(view.getContext(), getPOIStatusView(view), status, this, getPoimOrNull());
	}

	@Override
	public void onServiceUpdatesLoaded(@NonNull String targetUUID, @Nullable ArrayList<ServiceUpdate> serviceUpdates) {
		View view = getView();
		if (view == null) {
			return;
		}
		POIViewController.updateServiceUpdatesView(getPOIView(view), serviceUpdates, this);
		POIServiceUpdateViewController.updateServiceUpdate(view.getContext(), getPOIServiceUpdateView(view), serviceUpdates, this);
	}

	@Override
	public boolean onURLClick(@NonNull String url) {
		return LinkUtils.open(requireActivity(), url, getString(R.string.web_browser), true);
	}

	@Nullable
	private Location userLocation;

	@Nullable
	@Override
	public Location getLocation() {
		return this.userLocation;
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null) {
			this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.permissionsGranted(this));
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			this.locationDeclination = sensorManager.getLocationDeclination(newLocation);
			if (!this.compassUpdatesEnabled) {
				sensorManager.registerCompassListener(this);
				this.compassUpdatesEnabled = true;
			}
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				LocationUtils.updateDistanceWithString(getContext(), poim, newLocation);
				POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), poim, this);
			}
			this.mapViewController.onUserLocationChanged(this.userLocation);
			if (this.adapter != null) {
				this.adapter.setLocation(newLocation);
			}
		}
	}

	private boolean compassUpdatesEnabled = false;

	@Override
	public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
	}

	private float[] accelerometerValues = new float[3];

	private float[] magneticFieldValues = new float[3];

	@Override
	public void onSensorChanged(@NonNull SensorEvent se) {
		sensorManager.checkForCompass(se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	private long lastCompassChanged = -1L;

	@Override
	public void updateCompass(float orientation, boolean force) {
		long now = UITimeUtils.currentTimeMillis();
		int roundedOrientation = DegreeUtils.convertToPositive360Degree((int) orientation);
		sensorManager.updateCompass(force, this.userLocation, roundedOrientation, now, AbsListView.OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
				this.lastCompassInDegree, Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS, this);
	}

	@Override
	public void onSensorTaskCompleted(boolean result, int roundedOrientation, long now) {
		if (result) {
			this.lastCompassInDegree = roundedOrientation;
			this.lastCompassChanged = now;
			if (this.compassUpdatesEnabled && this.userLocation != null && this.lastCompassInDegree >= 0) {
				POIManager poim = getPoimOrNull();
				if (poim != null) {
					POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), poim, this);
				}
			}
		}
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		MainActivity activity = (MainActivity) getActivity();
		if (activity == null) {
			return;
		}
		POIManager newPoim = DataSourceManager.findPOI(requireContext(), this.authority, POIProviderContract.Filter.getNewUUIDFilter(this.uuid));
		if (newPoim == null) {
			if (activity.isMTResumed()) {
				activity.popFragmentFromStack(this); // close this fragment
				this.modulesUpdated = false; // processed
			}
		} else {
			this.modulesUpdated = false; // nothing to do
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(() -> {
					if (POIFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
		}
		resetFavorite(); // force refresh
		getFavoriteFolderId();
		enableTimeChangedReceiver();
		this.mapViewController.onResume();
		if (this.adapter != null) {
			this.adapter.onResume(getActivity(), this.userLocation);
		}
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			this.mapViewController.notifyMarkerChanged(this);
			this.mapViewController.showMap(view);
			POIViewController.updateView(requireContext(), getPOIView(view), poim, this);
			POIStatusDetailViewController.updateView(requireContext(), getPOIStatusView(view), poim, this);
			POIServiceUpdateViewController.updateView(requireContext(), getPOIServiceUpdateView(view), poim, this);
			POINewsViewController.updateView(requireContext(), getPOINewsView(view), getNewsOrNull());
			setupRTSFullScheduleBtn(view);
			setupMoreNewsButton(view);
			setupMoreNearbyButton(view);
			setupNearbyList();
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
		this.adManager.setRewardedAdListener(this);
		refreshRewardedLayout(getView());
	}

	@Override
	public void onRewardedAdStatusChanged() {
		View view = getView();
		if (view != null) {
			refreshRewardedLayout(view);
		}
	}

	@NonNull
	private final ThreadSafeDateFormatter rewardedAdDateFormatter = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private void refreshRewardedLayout(@Nullable View view) {
		final View rewardedLayout = getPOIRewardedAdView(view);
		if (rewardedLayout != null) {
			final TextView rewardedAdTitleTv = rewardedLayout.findViewById(R.id.rewardAdTitle);
			final TextView rewardedAdsBtn = rewardedLayout.findViewById(R.id.rewardedAdsBtn);

			final boolean availableToShow = this.adManager.isRewardedAdAvailableToShow();
			final boolean rewardedNow = this.adManager.isRewardedNow();
			final long rewardedUntilInMs = this.adManager.getRewardedUntilInMs();
			final int rewardedAmount = this.adManager.getRewardedAdAmount();

			rewardedLayout.setVisibility(availableToShow ? View.VISIBLE : View.GONE);

			if (rewardedNow) {
				rewardedAdTitleTv.setText(getString(
						R.string.watch_rewarded_ad_title_text_and_date,
						this.rewardedAdDateFormatter.formatThreadSafe(rewardedUntilInMs)
				));
				rewardedAdTitleTv.setVisibility(View.VISIBLE);
			} else {
				rewardedAdTitleTv.setVisibility(View.GONE);
				rewardedAdTitleTv.setText(null);
			}

			rewardedAdsBtn.setText(getString(
					rewardedNow ?
							R.string.watch_rewarded_ad_btn_more_and_days :
							R.string.watch_rewarded_ad_btn_and_days,
					rewardedAmount
			));
			if (availableToShow) { // only if NOT paying user
				rewardedAdsBtn.setEnabled(true);
				rewardedAdsBtn.setVisibility(View.VISIBLE);
			} else {
				rewardedAdsBtn.setEnabled(false); // keep but disable
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.compassUpdatesEnabled) {
			sensorManager.unregisterSensorListener(this);
			this.compassUpdatesEnabled = false;
		}
		disableTimeChangedReceiver();
		this.mapViewController.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
		this.adManager.setRewardedAdListener(null);
	}

	private long nowToTheMinute = -1L;

	@Override
	public long getNowToTheMinute() {
		if (this.nowToTheMinute < 0L) {
			resetNowToTheMinute();
			enableTimeChangedReceiver();
		}
		return this.nowToTheMinute;
	}

	@Override
	public void onTimeChanged() {
		resetNowToTheMinute();
	}

	private void resetNowToTheMinute() {
		MTLog.i(this, "Refreshing UI data...");
		this.nowToTheMinute = UITimeUtils.currentTimeToTheMinuteMillis();
		View view = getView();
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			View poiView = getPOIView(view);
			POIViewController.updatePOIStatus(getContext(), poiView, poim, this);
			POIViewController.updatePOIServiceUpdate(getContext(), poiView, poim, this);
			POIStatusDetailViewController.updateView(requireContext(), getPOIStatusView(view), poim, this);
			POIServiceUpdateViewController.updateView(requireContext(), getPOIServiceUpdateView(view), poim, this);
		}
		ArrayList<News> news = getNewsOrNull();
		if (news != null) {
			POINewsViewController.updateView(requireContext(), getPOINewsView(view), news);
		}
	}

	private boolean timeChangedReceiverEnabled = false;

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			if (getContext() != null) {
				getContext().registerReceiver(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER);
			}
			this.timeChangedReceiverEnabled = true;
		}
	}

	private void disableTimeChangedReceiver() {
		if (this.timeChangedReceiverEnabled) {
			if (getContext() != null) {
				getContext().unregisterReceiver(this.timeChangedReceiver);
			}
			this.timeChangedReceiverEnabled = false;
			this.nowToTheMinute = -1L;
		}
	}

	private final UITimeUtils.TimeChangedReceiver timeChangedReceiver = new UITimeUtils.TimeChangedReceiver(this);

	@Override
	public boolean hasLocation() {
		return this.userLocation != null;
	}

	private int lastCompassInDegree = -1;

	@Override
	public int getLastCompassInDegree() {
		return lastCompassInDegree;
	}

	@Override
	public boolean hasLastCompassInDegree() {
		return lastCompassInDegree >= 0;
	}

	private float locationDeclination;

	@Override
	public float getLocationDeclination() {
		return this.locationDeclination;
	}

	@Override
	public boolean isClosestPOI(@NonNull String uuid) {
		return false;
	}

	@Nullable
	private Integer favoriteFolderId = null;

	@Nullable
	private Integer getFavoriteFolderId() {
		if (this.favoriteFolderId == null) {
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				this.favoriteFolderId = FavoriteManager.findFavoriteFolderId(requireContext(), poim.poi.getUUID());
			}
		}
		return this.favoriteFolderId;
	}

	public boolean isFavorite() {
		if (this.favoriteFolderId == null) {
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				this.favoriteFolderId = FavoriteManager.findFavoriteFolderId(requireContext(), poim.poi.getUUID());
			}
		}
		return getFavoriteFolderId() != null && getFavoriteFolderId() >= 0;
	}

	private void resetFavorite() {
		this.favoriteFolderId = null;
	}

	@Override
	public boolean isFavorite(@NonNull String uuid) {
		POIManager poim = getPoimOrNull();
		if (poim != null && poim.poi.getUUID().equals(uuid)) {
			return isFavorite();
		}
		return FavoriteManager.isFavorite(requireContext(), uuid);
	}

	@Override
	public boolean isShowingFavorite() {
		return false; // shown in the action bar
	}

	@Override
	public boolean isShowingStatus() {
		return true;
	}

	@Override
	public boolean isShowingExtra() {
		return true;
	}

	@Override
	public boolean isShowingServiceUpdates() {
		return true;
	}

	private MenuItem addRemoveFavoriteMenuItem;

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_poi, menu);
		this.addRemoveFavoriteMenuItem = menu.findItem(R.id.menu_add_remove_favorite);
		updateFavMenuItem();
	}

	private void updateFavMenuItem() {
		if (this.addRemoveFavoriteMenuItem == null) {
			return;
		}
		POIManager poim = getPoimOrNull();
		if (poim != null && poim.isFavoritable()) {
			boolean isFav = isFavorite();
			this.addRemoveFavoriteMenuItem.setIcon(isFav ? R.drawable.ic_star_black_24dp : R.drawable.ic_star_border_black_24dp);
			this.addRemoveFavoriteMenuItem.setTitle(isFav ? //
					FavoriteManager.get(requireContext()).isUsingFavoriteFolders() ? //
							R.string.menu_action_edit_favorite //
							: R.string.menu_action_remove_favorite //
					: R.string.menu_action_add_favorite);
			this.addRemoveFavoriteMenuItem.setVisible(true);
		} else {
			this.addRemoveFavoriteMenuItem.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_remove_favorite:
			POIManager poim = getPoimOrNull();
			if (poim != null && poim.isFavoritable()) {
				return FavoriteManager.get(requireActivity()).addRemoveFavorite(requireActivity(), poim.poi.getUUID(), this);
			}
			break;
		case R.id.menu_show_directions:
			POIManager poim2 = getPoimOrNull();
			if (poim2 != null) {
				MapUtils.showDirection(getActivity(), poim2.poi.getLat(), poim2.poi.getLng(), null, null, poim2.poi.getName());
				return true; // handled
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onFavoriteUpdated() {
		resetFavorite();
		updateFavMenuItem();
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIViewController.updateView(getContext(), getPOIView(getView()), poim, this);
		}
	}

	@Override
	public boolean isABReady() {
		return hasAgency();
	}

	@NonNull
	@Override
	public CharSequence getABTitle(@NonNull Context context) {
		AgencyProperties agency = getAgencyOrNull();
		if (agency != null) {
			return agency.getShortName();
		}
		return context.getString(R.string.ellipsis);
	}

	@NonNull
	@Override
	public Integer getABBgColor(@NonNull Context context) {
		return Color.TRANSPARENT;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		this.mapViewController.onLowMemory();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		this.mapViewController.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.mapViewController.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
		TaskUtils.cancelQuietly(this.loadAgencyTask, true);
		TaskUtils.cancelQuietly(this.loadPoimTask, true);
		TaskUtils.cancelQuietly(this.loadNewsTask, true);
	}
}
