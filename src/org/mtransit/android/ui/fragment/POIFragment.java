package org.mtransit.android.ui.fragment;

import java.util.Arrays;
import java.util.Collection;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.SensorUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.ScheduleProviderProperties;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.POIStatusDetailViewController;
import org.mtransit.android.ui.view.POIViewController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;

public class POIFragment extends ABFragment implements POIViewController.POIDataProvider, MTActivityWithLocation.UserLocationListener, SensorEventListener,
		SensorUtils.CompassListener, SensorUtils.SensorTaskCompleted, FavoriteManager.FavoriteUpdateListener, TimeUtils.TimeChangedReceiver.TimeChangedListener {

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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_poi, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.poim != null && this.poim.poi != null) {
			outState.putString(EXTRA_POI_UUID, this.poim.poi.getUUID());
			outState.putString(EXTRA_AUTHORITY, this.poim.poi.getAuthority());
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		final String authority = BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, getArguments());
		final String uuid = BundleUtils.getString(EXTRA_POI_UUID, savedInstanceState, getArguments());
		if (!TextUtils.isEmpty(authority) && !TextUtils.isEmpty(uuid)) {
			final POIFilter poiFilter = new POIFilter(Arrays.asList(new String[] { uuid }));
			this.poim = DataSourceManager.findPOI(getActivity(), UriUtils.newContentUri(authority), poiFilter);
			this.poim.setScheduleMaxDataRequests(Schedule.ScheduleStatusFilter.DATA_REQUEST_MONTH);
			this.agency = DataSourceProvider.get(getActivity()).getAgency(authority);
			setupView(getView());
		}
		final Location optUserLocation = BundleUtils.getParcelable(EXTRA_USER_LOCATION, savedInstanceState, getArguments());
		if (optUserLocation != null) {
			onUserLocationChanged(optUserLocation);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	private void setupView(View view) {
		if (view == null || this.poim == null) {
			return;
		}
		POIViewController.updateView(this.poim, getPOIView(view), this);
		POIStatusDetailViewController.updateView(this.poim, getPOIStatusView(view), this);
		final View rtsScheduleBtn = view.findViewById(R.id.fullScheduleBtn);
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

	private View getPOIStatusView() {
		return getPOIStatusView(getView());
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

	private View getPOIView() {
		return getPOIView(getView());
	}

	private View getPOIView(View view) {
		if (view == null || this.poim == null) {
			return null;
		}
		if (view.findViewById(R.id.poi) == null) { // IF NOT present/inflated DO
			final int layoutResId = POIViewController.getLayoutResId(this.poim);
			((ViewStub) view.findViewById(R.id.poi_stub)).setLayoutResource(layoutResId);
			((ViewStub) view.findViewById(R.id.poi_stub)).inflate(); // inflate
		}
		return view.findViewById(R.id.poi);
	}

	@Override
	public void onStatusLoaded(POIStatus status) {
		POIViewController.updatePOIStatus(getActivity(), this, getPOIView(), status);
		POIStatusDetailViewController.updatePOIStatus(getActivity(), this, getPOIStatusView(), status);
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
				POIViewController.updatePOIDistanceAndCompass(this.poim, getPOIView(), this);
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
				POIViewController.updatePOIDistanceAndCompass(poim, getPOIView(), this);
			}
		}
	}

	@Override
	public void onModulesUpdated() {
		final FragmentActivity activity = getActivity();
		if (this.poim != null && activity != null) {
			final POIFilter poiFilter = new POIFilter(Arrays.asList(new String[] { this.poim.poi.getUUID() }));
			final POIManager newPoim = DataSourceManager.findPOI(activity, UriUtils.newContentUri(this.poim.poi.getAuthority()), poiFilter);
			if (newPoim == null) {
				((MainActivity) activity).popFragmentFromStack(this); // close this fragment
				return;
			} else if (!this.poim.poi.equals(newPoim.poi)) {
				this.poim = newPoim;
				setupView(getView());
			}
			POIViewController.updatePOIStatus(getPOIView(), this.poim, this);
			POIStatusDetailViewController.updatePOIStatus(getPOIStatusView(), this.poim, this);
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
		POIViewController.updatePOIStatus(getPOIView(), this.poim, this);
		POIStatusDetailViewController.updatePOIStatus(getPOIStatusView(), this.poim, this);
	}

	public void onPause() {
		super.onPause();
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(getActivity(), this);
			this.compassUpdatesEnabled = false;
		}
		disableTimeChangeddReceiver();
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
		POIViewController.updatePOIStatus(getPOIView(), this.poim, this);
		POIStatusDetailViewController.updatePOIStatus(getPOIStatusView(), this.poim, this);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.poi_menu, menu);
		getAbController().addMenuItem(R.id.menu_add_remove_favorite, menu.findItem(R.id.menu_add_remove_favorite));
		updateFavMenuItem();
	}

	private void updateFavMenuItem() {
		MenuItem addRemoveFavoriteMenuItem = getAbController().getMenuItem(R.id.menu_add_remove_favorite);
		if (addRemoveFavoriteMenuItem == null) {
			return;
		}
		if (this.poim != null && this.poim.isFavoritable()) {
			final boolean isFav = isFavorite();
			addRemoveFavoriteMenuItem.setIcon(isFav ? R.drawable.btn_star_on_normal_holo_light : R.drawable.btn_star_off_normal_holo_light);
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
		POIViewController.updateView(this.poim, getPOIView(), this);
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
	public int getABIconDrawableResId() {
		if (this.agency != null) {
			return this.agency.getType().getAbIconResId();
		}
		return super.getABIconDrawableResId();
	}
}
