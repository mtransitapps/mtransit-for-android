package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ResourceUtils;
import org.mtransit.android.commons.SensorUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.api.SupportFactory;
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
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ScrollView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class POIFragment extends ABFragment implements POIViewController.POIDataProvider, MTActivityWithLocation.UserLocationListener, SensorEventListener,
		SensorUtils.CompassListener, SensorUtils.SensorTaskCompleted, FavoriteManager.FavoriteUpdateListener,
		LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, TimeUtils.TimeChangedReceiver.TimeChangedListener, LocationSource, OnMapReadyCallback {

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

	private boolean hasAgency() {
		if (this.agency == null) {
			initAgencyAsync();
			return false;
		}
		return true;
	}

	private void initAgencyAsync() {
		if (this.loadAgencyTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadAgencyTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadAgencyTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadAgencyTask";
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
		if (this.loadPoimTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadPoimTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadPoimTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadPoimTask";
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
		updateMapPosition(false);
		View view = getView();
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

	private void resetPoim() {
		this.poim = null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
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
		setupView(view);
		initMapView(view, savedInstanceState);
		return view;
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (!TextUtils.isEmpty(this.uuid)) {
			outState.putString(EXTRA_POI_UUID, this.uuid);
		}
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AUTHORITY, this.authority);
		}
		if (this.mapView != null) {
			this.mapView.onSaveInstanceState(outState);
		}
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
		if (CollectionUtils.getSize(data) < LocationUtils.MIN_NEARBY_LIST
				&& !LocationUtils.searchComplete(poim.poi.getLat(), poim.poi.getLng(), this.ad.aroundDiff)) {
			LocationUtils.incAroundDiff(this.ad);
			LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
			return;
		}
		POIManager poim = getPoimOrNull();
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


	private MapView mapView = null;
	private Marker poiMarker = null;

	private MapView getMapView(View view) {
		if (this.mapView == null) {
			initMapView(view, null);
		}
		return this.mapView;
	}

	private void initMapView(View view, Bundle savedInstanceState) {
		if (view != null) {
			this.mapView = (MapView) view.findViewById(R.id.map);
			if (this.mapView != null) {
				this.mapView.onCreate(savedInstanceState);
				if (this.isResumed()) {
					this.mapView.onResume();
				}
				initMap(this.mapView);
				if (this.mapView.getViewTreeObserver().isAlive()) {
					this.mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

						@Override
						public void onGlobalLayout() {
							if (POIFragment.this.mapView != null) {
								SupportFactory.get().removeOnGlobalLayoutListener(POIFragment.this.mapView.getViewTreeObserver(), this);
								updateMapPosition(false);
							}
						}

					});
				}
			}
		}
	}

	private void updateMapPosition(boolean anim) {
		POIManager poim = getPoimOrNull();
		if (poim == null) {
			return;
		}
		MapView mapView = getMapView(getView());
		GoogleMap map = getMap(mapView);
		if (map == null) {
			return;
		}
		Context context = getActivity();
		if (context == null) {
			return;
		}
		LatLng poiLatLng = new LatLng(poim.poi.getLat(), poim.poi.getLng());
		BitmapDescriptor bitmapDescriptor = getBitmapDescriptor(context);
		if (this.poiMarker == null) {
			MarkerOptions newMarkerOptions = new MarkerOptions() //
					.position(poiLatLng);
			if (bitmapDescriptor != null) {
				newMarkerOptions.icon(bitmapDescriptor);
			}
			this.poiMarker = map.addMarker(newMarkerOptions); //
		} else {
			this.poiMarker.setVisible(false);
			this.poiMarker.setPosition(poiLatLng);
			if (bitmapDescriptor != null) {
				this.poiMarker.setIcon(bitmapDescriptor);
			}
			this.poiMarker.setVisible(true);
		}
		LatLngBounds.Builder llb = LatLngBounds.builder().include(poiLatLng);
		if (this.userLocation != null) {
			llb.include(new LatLng(this.userLocation.getLatitude(), this.userLocation.getLongitude()));
		}
		MapUtils.updateMapPosition(context, map, mapView, anim, llb.build(), MapUtils.getMapWithoutButtonsCameraPaddingInPx(context));
	}
	private BitmapDescriptor getBitmapDescriptor(Context context) {
		try {
			if (context == null) {
				return BitmapDescriptorFactory.defaultMarker();
			}
			int markerColor = MapUtils.DEFAULT_MARKET_COLOR;
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				markerColor = poim.getColor(context);
			}
			if (markerColor == Color.BLACK) {
				markerColor = Color.DKGRAY;
			}
			int bitmapResId = R.drawable.ic_place_white_slim;
			return BitmapDescriptorFactory.fromBitmap(ColorUtils.colorizeBitmapResource(context, markerColor, bitmapResId));
		} catch (Exception e) {
			return BitmapDescriptorFactory.defaultMarker();
		}
	}

	private GoogleMap map = null;

	private GoogleMap getMap(MapView mapView) {
		if (this.map == null) {
			initMap(mapView);
		}
		return this.map;
	}

	private void initMap(MapView mapView) {
		if (mapView != null) {
			mapView.getMapAsync(this);
		}
	}

	@Override
	public void onMapReady(GoogleMap map) {
		this.map = map;
		setupMap();
	}

	private void setupMap() {
		if (this.map != null) {
			this.map.setMyLocationEnabled(true);
			this.map.setLocationSource(this);
			this.map.getUiSettings().setMyLocationButtonEnabled(false);
			this.map.getUiSettings().setIndoorLevelPickerEnabled(false);
			this.map.setTrafficEnabled(false);
			this.map.setIndoorEnabled(false);
			int paddingTop = (int) ResourceUtils.convertSPtoPX(getActivity(), 32); // action bar
			this.map.setPadding(0, paddingTop, 0, 0);
			updateMapPosition(false);
		}
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
						if (POIFragment.this.poim != null && POIFragment.this.poim.poi instanceof RouteTripStop) {
							optRts = (RouteTripStop) POIFragment.this.poim.poi;
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
					((MainActivity) getActivity()).addFragmentToStack(NearbyFragment.newFixedOnInstance(optTypeId, POIFragment.this.poim.getLat(),
							POIFragment.this.poim.getLng(), POIFragment.this.poim.poi.getName(), POIFragment.this.poim.getColor(POIFragment.this.getActivity())));
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
		if (newLocation != null) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				this.locationDeclination = SensorUtils.getLocationDeclination(newLocation);
				POIManager poim = getPoimOrNull();
				if (poim != null) {
					LocationUtils.updateDistanceWithString(getActivity(), poim, newLocation);
					POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), poim, this);
				}
				updateMapPosition(true);
				if (this.adapter != null) {
					this.adapter.setLocation(newLocation);
				}
			}
		}
		if (this.locationChandedListener != null) {
			this.locationChandedListener.onLocationChanged(newLocation);
		}
	}

	private LocationSource.OnLocationChangedListener locationChandedListener;

	@Override
	public void activate(LocationSource.OnLocationChangedListener locationChandedListener) {
		this.locationChandedListener = locationChandedListener;
		if (this.userLocation != null && this.locationChandedListener != null) {
			this.locationChandedListener.onLocationChanged(this.userLocation);
		}
	}

	@Override
	public void deactivate() {
		this.locationChandedListener = null;
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
		if (this.modulesUpdated) {
			getView().post(new Runnable() {
				@Override
				public void run() {
					if (POIFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
		if (!this.compassUpdatesEnabled) {
			SensorUtils.registerCompassListener(getActivity(), this);
			this.compassUpdatesEnabled = true;
		}
		this.isFavorite = null; // force refresh
		if (this.mapView != null) {
			this.mapView.onResume();
		}
		if (this.adapter != null) {
			this.adapter.onResume(getActivity());
		}
		POIManager poim = getPoimOrNull();
		if (poim != null) {
			View view = getView();
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
		if (this.mapView != null) {
			this.mapView.onPause();
		}
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
		return lastCompassInDegree > 0;
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
		if (getMapView(getView()) != null) {
			getMapView(getView()).onLowMemory();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (this.mapView != null) {
			this.mapView.onDestroy();
			this.mapView = null;
		}
		this.map = null;
		this.poiMarker = null;
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
