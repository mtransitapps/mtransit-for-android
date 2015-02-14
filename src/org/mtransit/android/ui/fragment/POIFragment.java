package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SensorUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.ScheduleProviderProperties;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.NearbyPOIListLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.ui.view.POIServiceUpdateViewController;
import org.mtransit.android.ui.view.POIStatusDetailViewController;
import org.mtransit.android.ui.view.POIViewController;
import org.mtransit.android.util.MapUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.ScrollView;

public class POIFragment extends ABFragment implements POIViewController.POIDataProvider, MTActivityWithLocation.UserLocationListener, SensorEventListener,
		SensorUtils.CompassListener, SensorUtils.SensorTaskCompleted, FavoriteManager.FavoriteUpdateListener,
		LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, TimeUtils.TimeChangedReceiver.TimeChangedListener, MapViewController.MapMarkerProvider {

	private static final String TAG = POIFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		if (this.poim != null && this.poim.poi != null) {
			return TAG + "-" + this.poim.poi.getUUID();
		}
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "POI";

	@Override
	public String getScreenName() {
		if (!TextUtils.isEmpty(this.uuid)) {
			return TRACKING_SCREEN_NAME + "/" + this.uuid;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";

	public static POIFragment newInstance(String uuid, String authority, AgencyProperties optAgency, POIManager optPoim) {
		POIFragment f = new POIFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		f.agency = optAgency;
		args.putString(EXTRA_POI_UUID, uuid);
		f.uuid = uuid;
		f.poim = optPoim;
		f.poim.setInFocus(true);
		f.poim.resetLastFindTimestamps();
		f.setArguments(args);
		return f;
	}

	private String authority;

	private AgencyProperties agency;

	private MapViewController mapViewController = new MapViewController(TAG, this, true, false, false, false, false, 32, true, false);

	private boolean hasAgency() {
		if (this.agency == null) {
			initAgencyAsync();
			return false;
		}
		return true;
	}

	private void initAgencyAsync() {
		if (this.loadAgencyTask != null && this.loadAgencyTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadAgencyTask = new LoadAgencyTask();
		this.loadAgencyTask.execute();
	}

	private LoadAgencyTask loadAgencyTask = null;

	private class LoadAgencyTask extends MTAsyncTask<Void, Void, Boolean> {

		@Override
		public String getLogTag() {
			return POIFragment.this.getLogTag() + ">" + LoadAgencyTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initAgencySync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewAgency();
			}
		}
	};

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
			this.agency = DataSourceProvider.get(getActivity()).getAgency(getActivity(), this.authority);
		}
		return this.agency != null;
	}

	private void applyNewAgency() {
		if (this.agency == null) {
			return;
		}
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIStatusDetailViewController.updateView(getActivity(), getPOIStatusView(getView()), this.agency, poim, this);
		}
		getAbController().setABTitle(this, getABTitle(getActivity()), false);
		getAbController().setABReady(this, isABReady(), true);
	}

	private void resetAgency() {
		this.agency = null;
	}

	private String uuid;

	private POIManager poim;

	private boolean hasPoim() {
		if (this.poim == null) {
			initPoimAsync();
			return false;
		}
		return true;
	}

	private void initPoimAsync() {
		if (this.loadPoimTask != null && this.loadPoimTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadPoimTask = new LoadPoimTask();
		this.loadPoimTask.execute();
	}

	private LoadPoimTask loadPoimTask = null;

	private class LoadPoimTask extends MTAsyncTask<Void, Void, Boolean> {

		@Override
		public String getLogTag() {
			return POIFragment.this.getLogTag() + ">" + LoadPoimTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initPoimSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewPoim();
			}
		}
	};

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
			this.poim = DataSourceManager.findPOI(getActivity(), this.authority, new POIFilter(Arrays.asList(new String[] { this.uuid })));
		}
		return this.poim != null;
	}

	private void applyNewPoim() {
		if (this.poim == null) {
			return;
		}
		this.poim.setInFocus(true);
		this.poim.setScheduleMaxDataRequests(Schedule.ScheduleStatusFilter.DATA_REQUEST_MONTH);
		if (this.adapter != null) {
			this.adapter.clear();
		}
		this.isFavorite = null; // reset
		View view = getView();
		this.mapViewController.notifyMarkerChanged(this);
		this.mapViewController.showMap();
		POIViewController.updateView(getActivity(), getPOIView(view), this.poim, this);
		AgencyProperties agency = getAgencyOrNull();
		if (agency != null) {
			POIStatusDetailViewController.updateView(getActivity(), getPOIStatusView(view), agency, this.poim, this);
		}
		POIServiceUpdateViewController.updateView(getActivity(), getPOIServiceUpdateView(view), this.poim, this);
		setupRTSFullScheduleBtn(view);
		setupMoreNearbyButton(view);
		getActivity().supportInvalidateOptionsMenu(); // add/remove star from action bar
		setupNearbyList();
	}

	@Override
	public POIManager getClosestPOI() {
		return this.poim;
	}

	@Override
	public POIManager getPOI(String uuid) {
		return this.poim != null && this.poim.poi.getUUID().equals(uuid) ? this.poim : null;
	}

	@Override
	public Collection<POIManager> getPOIs() {
		return Arrays.asList(this.poim);
	}

	private void resetPoim() {
		this.poim = null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
		this.mapViewController.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.mapViewController.onDetach();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_poi, container, false);
		this.mapViewController.onCreateView(view, savedInstanceState);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupView(view);
		this.mapViewController.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
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
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetAgency();
		}
		String newUuid = BundleUtils.getString(EXTRA_POI_UUID, bundles);
		if (!TextUtils.isEmpty(newUuid) && !newUuid.equals(this.uuid)) {
			this.uuid = newUuid;
			resetPoim();
		}
	}

	private POIArrayAdapter adapter;

	private void setupNearbyList() {
		if (this.adapter != null && this.adapter.isInitialized()) {
			showNearbyList();
			return;
		}
		hideNearbyList(true);
		if (hasPoim()) {
			LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
		}
	}

	private static final int NEARBY_POIS_LOADER = 0;
	private LocationUtils.AroundDiff ad = LocationUtils.getNewDefaultAroundDiff();

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case NEARBY_POIS_LOADER:
			POIManager poim = getPoimOrNull();
			if (TextUtils.isEmpty(this.authority) || poim == null) {
				return null;
			}
			Context context = getActivity();
			if (context == null) {
				return null;
			}
			return new NearbyPOIListLoader(context, poim.poi.getLat(), poim.poi.getLng(), this.ad.aroundDiff,
					LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS, LocationUtils.MAX_POI_NEARBY_POIS_LIST, false, this.authority);
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
		POIManager poim = getPoimOrNull();
		if (CollectionUtils.getSize(data) < LocationUtils.MIN_NEARBY_LIST && poim != null
				&& !LocationUtils.searchComplete(poim.poi.getLat(), poim.poi.getLng(), this.ad.aroundDiff)) {
			LocationUtils.incAroundDiff(this.ad);
			LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
			return;
		}
		if (poim != null && data != null) {
			Iterator<POIManager> it = data.iterator();
			while (it.hasNext()) {
				if (it.next().poi.getUUID().equals(poim.poi.getUUID())) {
					it.remove();
					break;
				}
			}
		}
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		this.adapter.initManual();
		if (this.adapter.getPoisCount() > 0) {
			showNearbyList();
		} else {
			hideNearbyList(false);
		}
	}

	private void hideNearbyList(boolean invisibleInsteadOfGone) {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.poi_nearby_pois_title).setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
			view.findViewById(R.id.poi_nearby_pois_list).setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
		}
	}

	private void showNearbyList() {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.poi_nearby_pois_title).setVisibility(View.VISIBLE);
			view.findViewById(R.id.poi_nearby_pois_list).setVisibility(View.VISIBLE);
		}
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setTag(getLogTag());
	}


	private void setupView(View view) {
		if (view == null) {
			return;
		}
		this.adapter.setManualScrollView((ScrollView) view.findViewById(R.id.scrollview));
		this.adapter.setManualLayout((ViewGroup) view.findViewById(R.id.poi_nearby_pois_list));
	}

	private void setupRTSFullScheduleBtn(View view) {
		if (view == null) {
			return;
		}
		View rtsScheduleBtn = view.findViewById(R.id.fullScheduleBtn);
		if (rtsScheduleBtn != null) {
			Collection<ScheduleProviderProperties> scheduleProviders = DataSourceProvider.get(getActivity())
					.getTargetAuthorityScheduleProviders(this.authority);
			if (CollectionUtils.getSize(scheduleProviders) == 0) {
				rtsScheduleBtn.setVisibility(View.GONE);
			} else {
				rtsScheduleBtn.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						RouteTripStop optRts = null;
						POIManager poim = getPoimOrNull();
						if (poim != null && poim.poi instanceof RouteTripStop) {
							optRts = (RouteTripStop) poim.poi;
						}
						((MainActivity) getActivity()).addFragmentToStack(ScheduleFragment.newInstance(POIFragment.this.uuid, POIFragment.this.authority,
								POIFragment.this.agency, optRts));
					}
				});
				rtsScheduleBtn.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setupMoreNearbyButton(View view) {
		if (view == null) {
			return;
		}
		View moreBtn = view.findViewById(R.id.poi_nearby_pois_title).findViewById(R.id.moreBtn);
		if (moreBtn != null) {
			moreBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer optTypeId = null;
					AgencyProperties agency = getAgencyOrNull();
					if (agency != null) {
						optTypeId = agency.getType().getId();
					}
					((MainActivity) getActivity()).addFragmentToStack(NearbyFragment.newFixedOnInstance(optTypeId, poim.getLat(), poim.getLng(),
							poim.poi.getName(), poim.getColor(POIFragment.this.getActivity())));
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
			Integer layoutResId = POIServiceUpdateViewController.getLayoutResId();
			if (layoutResId != null) {
				((ViewStub) view.findViewById(R.id.poi_service_update_stub)).setLayoutResource(layoutResId);
				((ViewStub) view.findViewById(R.id.poi_service_update_stub)).inflate(); // inflate
			}
		}
		return view.findViewById(R.id.poi_service_update);
	}


	private View getPOIView(View view) {
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
	public void onStatusLoaded(POIStatus status) {
		View view = getView();
		POIViewController.updatePOIStatus(getActivity(), getPOIView(view), status, this);
		POIStatusDetailViewController.updatePOIStatus(getActivity(), getPOIStatusView(view), status, this);
	}

	@Override
	public void onServiceUpdatesLoaded(String targetUUID, ArrayList<ServiceUpdate> serviceUpdates) {
		View view = getView();
		POIViewController.updateServiceUpdatesView(getPOIView(view), serviceUpdates, this);
		POIServiceUpdateViewController.updateServiceUpdate(getActivity(), getPOIServiceUpdateView(view), serviceUpdates, this);
	}

	private Location userLocation;

	@Override
	public Location getLocation() {
		return this.userLocation;
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			this.locationDeclination = SensorUtils.getLocationDeclination(newLocation);
			if (!this.compassUpdatesEnabled) {
				SensorUtils.registerCompassListener(getActivity(), this);
				this.compassUpdatesEnabled = true;
			}
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				LocationUtils.updateDistanceWithString(getActivity(), poim, newLocation);
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
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private float[] accelerometerValues = new float[3];

	private float[] magneticFieldValues = new float[3];

	@Override
	public void onSensorChanged(SensorEvent se) {
		SensorUtils.checkForCompass(getActivity(), se, this.accelerometerValues, this.magneticFieldValues, this);
	}


	private long lastCompassChanged = -1l;

	@Override
	public void updateCompass(float orientation, boolean force) {
		long now = TimeUtils.currentTimeMillis();
		int roundedOrientation = SensorUtils.convertToPosivite360Degree((int) orientation);
		SensorUtils.updateCompass(force, this.userLocation, roundedOrientation, now, AbsListView.OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
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
		POIManager newPoim = DataSourceManager.findPOI(activity, this.authority, new POIFilter(Arrays.asList(new String[] { this.uuid })));
		if (newPoim == null) {
			if (activity.isMTResumed()) {
				((MainActivity) activity).popFragmentFromStack(this); // close this fragment
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
			view.post(new Runnable() {
				@Override
				public void run() {
					if (POIFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
		this.isFavorite = null; // force refresh
		this.mapViewController.onResume();
		if (this.adapter != null) {
			this.adapter.onResume(getActivity(), this.userLocation);
		}
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			this.mapViewController.notifyMarkerChanged(this);
			this.mapViewController.showMap();
			POIViewController.updateView(getActivity(), getPOIView(view), poim, this);
			AgencyProperties agency = getAgencyOrNull();
			if (agency != null) {
				POIStatusDetailViewController.updateView(getActivity(), getPOIStatusView(view), agency, poim, this);
			}
			POIServiceUpdateViewController.updateView(getActivity(), getPOIServiceUpdateView(view), poim, this);
			setupRTSFullScheduleBtn(view);
			setupMoreNearbyButton(view);
			setupNearbyList();
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(getActivity(), this);
			this.compassUpdatesEnabled = false;
		}
		disableTimeChangeddReceiver();
		this.mapViewController.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
	}

	private long nowToTheMinute = -1l;

	@Override
	public long getNowToTheMinute() {
		if (this.nowToTheMinute < 0) {
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
		this.nowToTheMinute = TimeUtils.currentTimeToTheMinuteMillis();
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			View view = getView();
			View poiView = getPOIView(view);
			POIViewController.updatePOIStatus(getActivity(), poiView, poim, this);
			POIViewController.updatePOIServiceUpdate(getActivity(), poiView, poim, this);
			AgencyProperties agency = getAgencyOrNull();
			if (agency != null) {
				POIStatusDetailViewController.updateView(getActivity(), getPOIStatusView(view), agency, poim, this);
			}
			POIServiceUpdateViewController.updateView(getActivity(), getPOIServiceUpdateView(view), poim, this);
		}
	}

	private boolean timeChangedReceiverEnabled = false;

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			getActivity().registerReceiver(timeChangedReceiver, TimeUtils.TIME_CHANGED_INTENT_FILTER);
			this.timeChangedReceiverEnabled = true;
		}
	}

	private void disableTimeChangeddReceiver() {
		if (this.timeChangedReceiverEnabled) {
			getActivity().unregisterReceiver(this.timeChangedReceiver);
			this.timeChangedReceiverEnabled = false;
			this.nowToTheMinute = -1l;
		}
	}

	private final BroadcastReceiver timeChangedReceiver = new TimeUtils.TimeChangedReceiver(this);

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
	public boolean isClosestPOI(String uuid) {
		return false;
	}

	private Boolean isFavorite = null;

	public boolean isFavorite() {
		if (this.isFavorite == null) {
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				this.isFavorite = FavoriteManager.isFavorite(getActivity(), poim.poi.getUUID());
			}
		}
		return isFavorite == null ? false : isFavorite;
	}

	@Override
	public boolean isFavorite(String uuid) {
		POIManager poim = getPoimOrNull();
		if (poim != null && poim.poi != null && poim.poi.getUUID().equals(uuid)) {
			return isFavorite();
		}
		return FavoriteManager.isFavorite(getActivity(), uuid);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
			this.addRemoveFavoriteMenuItem.setIcon(isFav ? R.drawable.ic_action_toggle_star_material_dark
					: R.drawable.ic_action_toggle_star_outline_material_dark);
			this.addRemoveFavoriteMenuItem.setTitle(isFav ? R.string.menu_action_remove_favorite : R.string.menu_action_add_favorite);
			this.addRemoveFavoriteMenuItem.setVisible(true);
		} else {
			this.addRemoveFavoriteMenuItem.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_remove_favorite:
			POIManager poim = getPoimOrNull();
			if (poim != null && poim.isFavoritable()) {
				poim.addRemoteFavorite(getActivity(), isFavorite(), this);
				return true; // handled
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
		this.isFavorite = null; // reset
		updateFavMenuItem();
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIViewController.updateView(getActivity(), getPOIView(getView()), poim, this);
		}
	}

	@Override
	public boolean isABReady() {
		return hasAgency();
	}

	@Override
	public CharSequence getABTitle(Context context) {
		AgencyProperties agency = getAgencyOrNull();
		if (agency != null) {
			return agency.getShortName();
		}
		return context.getString(R.string.ellipsis);
	}

	@Override
	public Integer getABBgColor(Context context) {
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
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
	}
}
