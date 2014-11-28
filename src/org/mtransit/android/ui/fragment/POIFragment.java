package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class POIFragment extends ABFragment implements POIViewController.POIDataProvider, MTActivityWithLocation.UserLocationListener, SensorEventListener,
		SensorUtils.CompassListener, SensorUtils.SensorTaskCompleted, FavoriteManager.FavoriteUpdateListener,
		LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, TimeUtils.TimeChangedReceiver.TimeChangedListener, LocationSource {

	private static final String TAG = POIFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "POI";

	@Override
	public String getScreenName() {
		if (this.poim != null && this.poim.poi != null) {
			return TRACKING_SCREEN_NAME + "/" + this.poim.poi.getUUID();
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";
	private static final String EXTRA_USER_LOCATION = "extra_user_location";

	public static POIFragment newInstance(POIManager poim, Location optUserLocation) {
		POIFragment f = new POIFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, poim.poi.getAuthority());
		args.putString(EXTRA_POI_UUID, poim.poi.getUUID());
		if (optUserLocation != null) {
			args.putParcelable(EXTRA_USER_LOCATION, optUserLocation);
		}
		f.setArguments(args);
		return f;
	}

	private POIManager poim;
	private AgencyProperties agency;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		restoreInstanceState(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_poi, container, false);
		setupView(view);
		getMapView(view).onCreate(savedInstanceState);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.poim != null && this.poim.poi != null) {
			outState.putString(EXTRA_POI_UUID, this.poim.poi.getUUID());
			outState.putString(EXTRA_AUTHORITY, this.poim.poi.getAuthority());
		}
		if (this.mapView != null) {
			this.mapView.onSaveInstanceState(outState);
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		String authority = BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, getArguments());
		String uuid = BundleUtils.getString(EXTRA_POI_UUID, savedInstanceState, getArguments());
		if (!TextUtils.isEmpty(authority) && !TextUtils.isEmpty(uuid)) {
			new LoadNewPOITask(this, authority, uuid).execute();
		}
		final Location optUserLocation = BundleUtils.getParcelable(EXTRA_USER_LOCATION, savedInstanceState, getArguments());
		if (optUserLocation != null) {
			onUserLocationChanged(optUserLocation);
		}
	}

	private static final class LoadNewPOITask extends MTAsyncTask<String, Void, POIManager> {

		private final String TAG = POIFragment.class.getSimpleName() + ">" + LoadNewPOITask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<POIFragment> poiFragmentWR;

		private String authority;
		private String uuid;

		public LoadNewPOITask(POIFragment poiFragment, String authority, String uuid) {
			this.poiFragmentWR = new WeakReference<POIFragment>(poiFragment);
			this.authority = authority;
			this.uuid = uuid;
		}

		@Override
		protected POIManager doInBackgroundMT(String... params) {
			if (TextUtils.isEmpty(this.authority) || TextUtils.isEmpty(this.uuid)) {
				MTLog.w(this, "doInBackground() > wrong parameters (this.authority:%s|this.uuid:%s)", this.authority, this.uuid);
				return null;
			}
			POIFragment poiFragment = this.poiFragmentWR == null ? null : this.poiFragmentWR.get();
			if (poiFragment == null) {
				MTLog.w(this, "doInBackground() > no fragment");
				return null;
			}
			poiFragment.agency = DataSourceProvider.get(poiFragment.getActivity()).getAgency(this.authority);
			POIFilter poiFilter = new POIFilter(Arrays.asList(new String[] { this.uuid }));
			POIManager newPOI = DataSourceManager.findPOI(poiFragment.getActivity(), this.authority, poiFilter);
			return newPOI;
		}

		@Override
		protected void onPostExecute(POIManager newPOI) {
			super.onPostExecute(newPOI);
			POIFragment poiFragment = this.poiFragmentWR == null ? null : this.poiFragmentWR.get();
			if (poiFragment == null) {
				return;
			}
			poiFragment.setupNewPOI(newPOI);
		}
	}

	private void setupNewPOI(POIManager newPOI) {
		if (newPOI == null) {
			((MainActivity) getActivity()).popFragmentFromStack(this); // close this fragment
			return;
		}
		if (this.poim != null && this.poim.poi.equals(newPOI.poi)) {
			return;
		}
		this.poim = newPOI;
		this.poim.setScheduleMaxDataRequests(Schedule.ScheduleStatusFilter.DATA_REQUEST_MONTH);
		View view = getView();
		if (this.adapter != null) {
			this.adapter.clear();
		}
		this.isFavorite = null; // reset
		forceOnUserLocationChanged();
		updateMapPosition(false);
		POIViewController.updateView(getActivity(), getPOIView(view), this.poim, this);
		POIStatusDetailViewController.updateView(getActivity(), getPOIStatusView(view), this.agency, this.poim, this);
		POIServiceUpdateViewController.updateView(getActivity(), getPOIServiceUpdateView(view), this.poim, this);
		setupRTSFullScheduleBtn(view);
		setupMoreNearbyButton(view);
		getActivity().supportInvalidateOptionsMenu(); // add/remove star from action bar
		setupNearbyList();
	}

	private POIArrayAdapter adapter;

	private void setupNearbyList() {
		if (this.poim == null) {
			return;
		} else if (this.adapter != null && this.adapter.isInitialized()) {
			showNearbyList();
			return;
		}
		hideNearbyList(true);
		LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
	}

	private static final int NEARBY_POIS_LOADER = 0;
	private LocationUtils.AroundDiff ad = LocationUtils.getNewDefaultAroundDiff();

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case NEARBY_POIS_LOADER:
			if (this.agency == null || this.poim == null) {
				return null;
			}
			Context context = getActivity();
			if (context == null) {
				return null;
			}
			NearbyPOIListLoader nearbyPOIListLoader = new NearbyPOIListLoader(context, this.poim.poi.getLat(), this.poim.poi.getLng(), this.ad.aroundDiff,
					LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE, LocationUtils.MAX_POI_NEARBY_POIS_LIST, false, this.agency);
			return nearbyPOIListLoader;
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
		if (CollectionUtils.getSize(data) < LocationUtils.MIN_NEARBY_LIST && ad.aroundDiff < LocationUtils.MAX_AROUND_DIFF) {
			LocationUtils.incAroundDiff(this.ad);
			LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
			return;
		}
		Iterator<POIManager> it = data.iterator();
		while (it.hasNext()) {
			if (it.next().poi.getUUID().equals(this.poim.poi.getUUID())) {
				it.remove();
				break;
			}
		}
		if (this.adapter == null) {
			initAdapter();
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

	private void initAdapter() {
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(TAG);
		View view = getView();
		this.adapter.setManualScrollView((ScrollView) view.findViewById(R.id.scrollview));
		this.adapter.setManualLayout((ViewGroup) view.findViewById(R.id.poi_nearby_pois_list));
	}

	private void forceOnUserLocationChanged() {
		if (this.userLocation != null) {
			Location lastUserLocation = this.userLocation;
			this.userLocation = null; // reset
			onUserLocationChanged(lastUserLocation);
		}
	}

	private MapView mapView = null;
	private Marker poiMarker = null;

	private MapView getMapView(View view) {
		if (this.mapView == null) {
			initMapView(view);
		}
		return this.mapView;
	}

	private void initMapView(View view) {
		if (view != null) {
			this.mapView = (MapView) view.findViewById(R.id.map);
			if (this.mapView != null) {
				initMap(this.mapView);
				if (this.mapView.getViewTreeObserver().isAlive()) {
					this.mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

						@Override
						public void onGlobalLayout() {
							SupportFactory.get().removeOnGlobalLayoutListener(POIFragment.this.mapView.getViewTreeObserver(), this);
							updateMapPosition(false);
						}

					});
				}
			}
		}
	}

	private void updateMapPosition(boolean anim) {
		try {
			if (this.poim != null) {
				MapView mapView = getMapView(getView());
				GoogleMap map = getMap(mapView);
				if (map != null) {
					if (!anim) {
						mapView.setVisibility(View.INVISIBLE);
					}
					MapsInitializer.initialize(getActivity());
					LatLng poiLatLng = new LatLng(this.poim.poi.getLat(), this.poim.poi.getLng());
					if (this.poiMarker == null) {
						MarkerOptions poiMarkerOptions = new MarkerOptions() //
								.title(this.poim.poi.getName()) //
								.position(poiLatLng) //
								.icon(getBitmapDescriptor());
						this.poiMarker = map.addMarker(poiMarkerOptions);
					} else {
						this.poiMarker.setVisible(false);
						this.poiMarker.setTitle(this.poim.poi.getName());
						this.poiMarker.setPosition(poiLatLng);
						this.poiMarker.setIcon(getBitmapDescriptor());
						this.poiMarker.setVisible(true);
					}
					LatLngBounds.Builder llb = LatLngBounds.builder().include(poiLatLng);
					if (this.userLocation != null) {
						llb.include(new LatLng(this.userLocation.getLatitude(), this.userLocation.getLongitude()));
					}
					CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(llb.build(), (int) ResourceUtils.convertSptoPx(getActivity(), 32));
					if (anim) {
						map.animateCamera(cameraUpdate);
					} else {
						map.moveCamera(cameraUpdate);
					}
					mapView.setVisibility(View.VISIBLE);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while initializing map position!");
		}
	}

	private BitmapDescriptor getBitmapDescriptor() {
		try {
			int markerColor = Color.WHITE;
			if (this.poim.poi instanceof RouteTripStop) {
				markerColor = ((RouteTripStop) this.poim.poi).route.getColorInt();
			} else if (this.agency != null) {
				markerColor = this.agency.getColorInt();
			}
			if (markerColor == Color.BLACK) {
				markerColor = Color.DKGRAY;
			}
			int bitmapResId = R.drawable.ic_place_white_slim;
			return BitmapDescriptorFactory.fromBitmap(ColorUtils.colorizeBitmapResource(getActivity(), markerColor, bitmapResId));
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
		this.map = mapView == null ? null : mapView.getMap();
		if (this.map != null) {
			this.map.setMyLocationEnabled(true);
			this.map.setLocationSource(this);
			this.map.getUiSettings().setMyLocationButtonEnabled(false);
			this.map.getUiSettings().setIndoorLevelPickerEnabled(false);
			this.map.setTrafficEnabled(false);
			this.map.setIndoorEnabled(false);
			int paddingTop = (int) ResourceUtils.convertSptoPx(getActivity(), 32);
			this.map.setPadding(0, paddingTop, 0, 0);
		}
	}


	private void setupView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter != null) {
			this.adapter.setManualScrollView((ScrollView) view.findViewById(R.id.scrollview));
			this.adapter.setManualLayout((ViewGroup) view.findViewById(R.id.poi_nearby_pois_list));
		}
	}

	private void setupRTSFullScheduleBtn(View view) {
		if (view == null || this.poim == null) {
			return;
		}
		View rtsScheduleBtn = view.findViewById(R.id.fullScheduleBtn);
		if (rtsScheduleBtn != null) {
			Collection<ScheduleProviderProperties> scheduleProviders = DataSourceProvider.get(getActivity()).getTargetAuthorityScheduleProviders(
					this.poim.poi.getAuthority());
			if (CollectionUtils.getSize(scheduleProviders) == 0) {
				rtsScheduleBtn.setVisibility(View.GONE);
			} else {
				rtsScheduleBtn.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((MainActivity) getActivity()).addFragmentToStack(ScheduleFragment.newInstance((RouteTripStop) POIFragment.this.poim.poi));
					}
				});
				rtsScheduleBtn.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setupMoreNearbyButton(View view) {
		if (view == null || this.poim == null) {
			return;
		}
		View moreBtn = view.findViewById(R.id.poi_nearby_pois_title).findViewById(R.id.moreBtn);
		if (moreBtn != null) {
			moreBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (POIFragment.this.poim != null && POIFragment.this.agency != null) {
						((MainActivity) getActivity()).addFragmentToStack(NearbyFragment.newInstance(null, POIFragment.this.agency.getType(),
								POIFragment.this.poim));
					}
				}
			});
			moreBtn.setVisibility(View.VISIBLE);
		}
	}

	private View getPOIStatusView(View view) {
		if (view == null || this.poim == null) {
			return null;
		}
		if (view.findViewById(R.id.poi_status_detail) == null) { // IF NOT present/inflated DO
			final Integer layoutResId = POIStatusDetailViewController.getLayoutResId(this.poim);
			if (layoutResId != null) {
				((ViewStub) view.findViewById(R.id.poi_status_detail_stub)).setLayoutResource(layoutResId.intValue());
				((ViewStub) view.findViewById(R.id.poi_status_detail_stub)).inflate(); // inflate
			}
		}
		return view.findViewById(R.id.poi_status_detail);
	}


	private View getPOIServiceUpdateView(View view) {
		if (view == null || this.poim == null) {
			return null;
		}
		if (view.findViewById(R.id.poi_service_update) == null) { // IF NOT present/inflated DO
			final Integer layoutResId = POIServiceUpdateViewController.getLayoutResId(this.poim);
			if (layoutResId != null) {
				((ViewStub) view.findViewById(R.id.poi_service_update_stub)).setLayoutResource(layoutResId.intValue());
				((ViewStub) view.findViewById(R.id.poi_service_update_stub)).inflate(); // inflate
			}
		}
		return view.findViewById(R.id.poi_service_update);
	}


	private View getPOIView(View view) {
		if (view == null || this.poim == null) {
			return null;
		}
		if (view.findViewById(R.id.this_poi) == null) { // IF NOT present/inflated DO
			final int layoutResId = POIViewController.getLayoutResId(this.poim);
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
		POIViewController.updateServiceUpdatesView(getActivity(), getPOIView(view), serviceUpdates, this);
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
				LocationUtils.updateDistanceWithString(getActivity(), this.poim, this.userLocation);
				POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), this.poim, this);
				updateMapPosition(true);
				if (this.adapter != null) {
					this.adapter.setLocation(this.userLocation);
				}
			}
		}
		if (this.locationChandedListener != null) {
			this.locationChandedListener.onLocationChanged(newLocation);
		}
	}

	private OnLocationChangedListener locationChandedListener;

	@Override
	public void activate(OnLocationChangedListener locationChandedListener) {
		this.locationChandedListener = locationChandedListener;
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

	private int scrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

	private long lastCompassChanged = -1l;

	@Override
	public void updateCompass(float orientation, boolean force) {
		long now = System.currentTimeMillis();
		int roundedOrientation = SensorUtils.convertToPosivite360Degree((int) orientation);
		SensorUtils.updateCompass(force, this.userLocation, roundedOrientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				Constants.ADAPTER_NOTIFY_THRESOLD_IN_MS, this);
	}

	@Override
	public void onSensorTaskCompleted(boolean result, int roundedOrientation, long now) {
		if (result) {
			this.lastCompassInDegree = roundedOrientation;
			this.lastCompassChanged = now;
			if (this.compassUpdatesEnabled && this.userLocation != null && this.lastCompassInDegree >= 0) {
				POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), this.poim, this);
			}
		}
	}

	@Override
	public void onModulesUpdated() {
		final FragmentActivity activity = getActivity();
		if (this.poim != null && activity != null) {
			new LoadNewPOITask(this, this.poim.poi.getAuthority(), this.poim.poi.getUUID());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!this.compassUpdatesEnabled) {
			SensorUtils.registerCompassListener(getActivity(), this);
			this.compassUpdatesEnabled = true;
		}
		this.isFavorite = null; // force refresh
		View view = getView();
		POIViewController.updateView(getActivity(), getPOIView(view), this.poim, this);
		POIStatusDetailViewController.updateView(getActivity(), getPOIStatusView(view), this.agency, this.poim, this);
		POIServiceUpdateViewController.updateView(getActivity(), getPOIServiceUpdateView(view), this.poim, this);
		if (this.mapView != null) {
			this.mapView.onResume();
		}
		setupRTSFullScheduleBtn(view);
		setupMoreNearbyButton(view);
		forceOnUserLocationChanged();
		setupNearbyList();
		if (this.adapter != null) {
			this.adapter.onResume(getActivity());
		}
	}

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
		View view = getView();
		View poiView = getPOIView(view);
		POIViewController.updatePOIStatus(getActivity(), poiView, this.poim, this);
		POIViewController.updatePOIServiceUpdate(getActivity(), poiView, this.poim, this);
		POIStatusDetailViewController.updateView(getActivity(), getPOIStatusView(view), this.agency, this.poim, this);
		POIServiceUpdateViewController.updateView(getActivity(), getPOIServiceUpdateView(view), this.poim, this);
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
			this.isFavorite = FavoriteManager.isFavorite(getActivity(), this.poim.poi.getUUID());
		}
		return isFavorite;
	}

	@Override
	public boolean isFavorite(String uuid) {
		if (this.poim != null && this.poim.poi != null && this.poim.poi.getUUID().equals(uuid)) {
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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_poi, menu);
		getAbController().addMenuItem(R.id.menu_add_remove_favorite, menu.findItem(R.id.menu_add_remove_favorite));
		updateFavMenuItem();
	}

	private void updateFavMenuItem() {
		MenuItem addRemoveFavoriteMenuItem = getAbController().getMenuItem(R.id.menu_add_remove_favorite);
		if (addRemoveFavoriteMenuItem == null) {
			return;
		}
		if (this.poim != null && this.poim.isFavoritable()) {
			boolean isFav = isFavorite();
			addRemoveFavoriteMenuItem.setIcon(isFav ? R.drawable.ic_action_toggle_star_material_dark : R.drawable.ic_action_toggle_star_outline_material_dark);
			addRemoveFavoriteMenuItem.setTitle(isFav ? R.string.menu_action_remove_favorite : R.string.menu_action_add_favorite);
			addRemoveFavoriteMenuItem.setVisible(true);
		} else {
			addRemoveFavoriteMenuItem.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_remove_favorite:
			if (this.poim != null && this.poim.isFavoritable()) {
				this.poim.addRemoteFavorite(getActivity(), isFavorite(), this);
				return true; // handled
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onFavoriteUpdated() {
		this.isFavorite = null; // reset
		updateFavMenuItem();
		POIViewController.updateView(getActivity(), getPOIView(getView()), this.poim, this);
	}

	@Override
	public boolean isABReady() {
		return this.agency != null;
	}

	@Override
	public CharSequence getABTitle(Context context) {
		if (this.agency != null) {
			return this.agency.getShortName();
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
