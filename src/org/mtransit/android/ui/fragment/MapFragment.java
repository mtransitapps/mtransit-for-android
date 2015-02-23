package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.MapPOILoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.util.LoaderUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
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

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MapFragment extends ABFragment implements LoaderManager.LoaderCallbacks<Collection<MapViewController.POIMarker>>,
		MTActivityWithLocation.UserLocationListener, MapViewController.MapListener {

	private static final String TAG = MapFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Map";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_INITIAL_LOCATION = "extra_initial_location";
	private static final String EXTRA_SELECTED_UUID = "extra_selected_uuid";
	private static final String EXTRA_INCLUDE_TYPE_ID = "extra_include_type_id";

	public static MapFragment newInstance(Location optInitialLocation, String optSelectedUUID, Integer optIncludeTypeId) {
		MapFragment f = new MapFragment();
		Bundle args = new Bundle();
		if (optInitialLocation != null) {
			args.putParcelable(EXTRA_INITIAL_LOCATION, optInitialLocation);
		}
		if (!TextUtils.isEmpty(optSelectedUUID)) {
			args.putString(EXTRA_SELECTED_UUID, optSelectedUUID);
		}
		if (optIncludeTypeId != null) {
			args.putInt(EXTRA_INCLUDE_TYPE_ID, optIncludeTypeId);
			f.includedTypeId = optIncludeTypeId;
		}
		f.setArguments(args);
		return f;
	}

	private MapViewController mapViewController = new MapViewController(TAG, null, this, true, true, true, false, false, false, 64, false, true, true, false,
			true);

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mapViewController.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		restoreInstanceState(savedInstanceState, getArguments());
		this.mapViewController.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_map, container, false);
		this.mapViewController.onCreateView(view, savedInstanceState);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupView(view);
		this.mapViewController.onViewCreated(view, savedInstanceState);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
	}

	private void restoreInstanceState(Bundle... bundles) {
		Location newInitialLocation = BundleUtils.getParcelable(EXTRA_INITIAL_LOCATION, bundles);
		if (newInitialLocation != null) {
			this.mapViewController.setInitialLocation(newInitialLocation);
		}
		String newSelectedUUID = BundleUtils.getString(EXTRA_SELECTED_UUID, bundles);
		if (!TextUtils.isEmpty(newSelectedUUID)) {
			this.mapViewController.setInitialSelectedUUID(newSelectedUUID);
		}
		Integer newIncludedTypeId = BundleUtils.getInt(EXTRA_INCLUDE_TYPE_ID, bundles);
		if (newIncludedTypeId != null) {
			this.includedTypeId = newIncludedTypeId;
		}
		this.mapViewController.setTag(getLogTag());
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			view.post(new Runnable() {
				@Override
				public void run() {
					if (MapFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
		this.mapViewController.onResume();
		hasFilterTypeIds(); // triggers markers loading if necessary
		this.mapViewController.showMap(view);
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		if (hasFilterTypeIds()) {
			resetTypeFilterIds();
			initFilterTypeIdsAsync();
		}
		this.modulesUpdated = false; // processed
	}

	private Location userLocation;

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
		}
		this.mapViewController.onUserLocationChanged(newLocation);
	}

	private static final int POIS_LOADER = 0;

	@Override
	public Loader<Collection<MapViewController.POIMarker>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			return new MapPOILoader(getActivity(), getFilterTypeIdsOrNull(), this.loadingLatLngBounds, this.loadedLatLngBounds);
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<Collection<MapViewController.POIMarker>> loader) {
	}

	@Override
	public void onLoadFinished(Loader<Collection<MapViewController.POIMarker>> loader, Collection<MapViewController.POIMarker> data) {
		if (this.loadingLatLngBounds == null) {
			return;
		}
		if (this.loadedLatLngBounds != null) {
			this.mapViewController.addMarkers(data);
			this.loadedLatLngBounds = this.loadingLatLngBounds;
			this.loadingLatLngBounds = null;
			this.mapViewController.hideLoading();
		} else {
			this.mapViewController.addMarkers(data);
			this.loadedLatLngBounds = this.loadingLatLngBounds;
			this.loadingLatLngBounds = null;
			this.mapViewController.showMap(getView());
		}
	}

	private LatLngBounds loadingLatLngBounds;
	private LatLngBounds loadedLatLngBounds;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.mapViewController.onConfigurationChanged(newConfig);
	}

	@Override
	public void onMapClick(LatLng position) {
	}

	@Override
	public void onCameraChange(LatLngBounds latLngBounds) {
		if (latLngBounds == null) {
			return;
		}
		boolean loaded = this.loadedLatLngBounds != null && this.loadedLatLngBounds.contains(latLngBounds.northeast)
				&& this.loadedLatLngBounds.contains(latLngBounds.southwest);
		boolean loading = this.loadingLatLngBounds != null && this.loadingLatLngBounds.contains(latLngBounds.northeast)
				&& this.loadingLatLngBounds.contains(latLngBounds.southwest);
		if (!loaded && !loading) {
			this.mapViewController.showLoading();
			getLoaderManager().destroyLoader(POIS_LOADER); // cancel now
			if (this.loadingLatLngBounds != null) {
				if (!this.loadingLatLngBounds.contains(latLngBounds.northeast)) {
					this.loadingLatLngBounds = this.loadingLatLngBounds.including(latLngBounds.northeast);
				}
				if (!this.loadingLatLngBounds.contains(latLngBounds.southwest)) {
					this.loadingLatLngBounds = this.loadingLatLngBounds.including(latLngBounds.southwest);
				}
			} else if (this.loadedLatLngBounds != null) {
				this.loadingLatLngBounds = this.loadedLatLngBounds;
				if (!this.loadingLatLngBounds.contains(latLngBounds.northeast)) {
					this.loadingLatLngBounds = this.loadingLatLngBounds.including(latLngBounds.northeast);
				}
				if (!this.loadingLatLngBounds.contains(latLngBounds.southwest)) {
					this.loadingLatLngBounds = this.loadingLatLngBounds.including(latLngBounds.southwest);
				}
			} else {
				this.loadingLatLngBounds = latLngBounds;
				float factor = 1.0f;
				LatLngBounds bigLatLngBounds = this.mapViewController.getBigCameraPosition(getActivity(), factor);
				if (bigLatLngBounds != null) {
					this.loadingLatLngBounds = bigLatLngBounds;
				}
			}
			if (hasFilterTypeIds()) {
				LoaderUtils.restartLoader(this, POIS_LOADER, null, this);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_map, menu);
	}

	private Integer includedTypeId = null;
	private Set<Integer> filterTypeIds = null;

	private void resetTypeFilterIds() {
		this.filterTypeIds = null; // reset
	}

	private Set<Integer> getFilterTypeIdsOrNull() {
		if (!hasFilterTypeIds()) {
			return null;
		}
		return this.filterTypeIds;
	}

	private boolean hasFilterTypeIds() {
		if (this.filterTypeIds == null) {
			initFilterTypeIdsAsync();
			return false;
		}
		return true;
	}

	private void initFilterTypeIdsAsync() {
		if (this.loadFilterTypeIdsTask != null && this.loadFilterTypeIdsTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		this.loadFilterTypeIdsTask = new LoadFilterTypeIdsTask();
		this.loadFilterTypeIdsTask.execute();
	}

	private LoadFilterTypeIdsTask loadFilterTypeIdsTask = null;

	private class LoadFilterTypeIdsTask extends MTAsyncTask<Object, Void, Boolean> {

		@Override
		public String getLogTag() {
			return MapFragment.this.getLogTag() + ">" + LoadFilterTypeIdsTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Object... params) {
			return initFilterTypeIdsSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewFilterTypeIds();
			}
		}
	};

	private boolean initFilterTypeIdsSync() {
		if (this.filterTypeIds != null) {
			return false;
		}
		ArrayList<DataSourceType> availableTypes = DataSourceProvider.get(getActivity()).getAvailableAgencyTypes();
		Set<String> filterTypeIdStrings = PreferenceUtils.getPrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS,
				PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT);
		this.filterTypeIds = new HashSet<Integer>();
		boolean hasChanged = false;
		for (String typeIdString : filterTypeIdStrings) {
			try {
				DataSourceType type = DataSourceType.parseId(Integer.parseInt(typeIdString));
				if (type == null) {
					hasChanged = true;
					continue;
				}
				if (!availableTypes.contains(type)) {
					hasChanged = true;
					continue;
				}
				if (!type.isMapScreen()) {
					hasChanged = true;
					continue;
				}
				this.filterTypeIds.add(type.getId());
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing filter type ID '%s'!", typeIdString);
				hasChanged = true;
			}
		}
		if (this.includedTypeId != null && !this.filterTypeIds.contains(this.includedTypeId)) {
			try {
				DataSourceType type = DataSourceType.parseId(this.includedTypeId);
				if (type == null) {
				} else if (!availableTypes.contains(type)) {
				} else if (!type.isMapScreen()) {
				} else {
					this.filterTypeIds.add(type.getId());
					hasChanged = true;
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing filter type ID '%s'!", this.includedTypeId);
				hasChanged = true;
			}
		}
		if (hasChanged) { // old setting not valid anymore
			saveMapFilterTypeIdsSetting(false); // asynchronous
			this.includedTypeId = null;
		}
		return this.filterTypeIds != null;
	}

	private void saveMapFilterTypeIdsSetting(boolean sync) {
		Set<Integer> filterTypeIds = getFilterTypeIdsOrNull();
		if (filterTypeIds == null) {
			return;
		}
		Set<String> newFilterTypeIdStrings = new HashSet<String>();
		for (Integer filterTypeId : filterTypeIds) {
			newFilterTypeIdStrings.add(String.valueOf(filterTypeId));
		}
		PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS, newFilterTypeIdStrings, sync);
	}

	private void applyNewFilterTypeIds() {
		if (this.filterTypeIds == null) {
			return;
		}
		getLoaderManager().destroyLoader(POIS_LOADER); // cancel now
		getAbController().setABTitle(this, getABTitle(getActivity()), true);
		if (this.loadingLatLngBounds == null) {
			this.loadingLatLngBounds = this.loadedLatLngBounds; // use the loaded area
		}
		this.loadedLatLngBounds = null; // loaded with wrong filter
		this.mapViewController.clearMarkers();
		this.mapViewController.showMap(getView());
		if (this.loadingLatLngBounds != null) {
			LoaderUtils.restartLoader(this, POIS_LOADER, null, this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_filter:
			Set<Integer> filterTypeIds = getFilterTypeIdsOrNull();
			if (filterTypeIds == null) {
				return false;
			}
			ArrayList<CharSequence> typeNames = new ArrayList<CharSequence>();
			ArrayList<Boolean> checked = new ArrayList<Boolean>();
			final ArrayList<Integer> typeIds = new ArrayList<Integer>();
			final HashSet<Integer> selectedItems = new HashSet<Integer>();
			ArrayList<DataSourceType> availableAgencyTypes = DataSourceProvider.get(getActivity()).getAvailableAgencyTypes();
			for (DataSourceType type : availableAgencyTypes) {
				if (!type.isMapScreen()) {
					continue; // skip
				}
				typeIds.add(type.getId());
				typeNames.add(getString(type.getPoiShortNameResId()));
				checked.add(filterTypeIds.size() == 0 || filterTypeIds.contains(type.getId()));
			}
			boolean[] checkedItems = new boolean[checked.size()];
			for (int c = 0; c < checked.size(); c++) {
				checkedItems[c] = checked.get(c);
				if (checkedItems[c]) {
					selectedItems.add(c);
				}
			}
			new AlertDialog.Builder(getActivity()) //
					.setTitle(R.string.menu_action_filter) //
					.setMultiChoiceItems(typeNames.toArray(new CharSequence[] {}), checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which, boolean isChecked) {
							if (isChecked) {
								selectedItems.add(which);
							} else {
								selectedItems.remove(which);
							}
						}
					}) //
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							applyNewFilter(typeIds, selectedItems);
							dialog.dismiss();
						}
					}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).setCancelable(true).create().show();

			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
		this.mapViewController.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		this.mapViewController.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
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
		this.loadedLatLngBounds = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.mapViewController.onDestroy();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.mapViewController.onDetach();
	}

	@Override
	public CharSequence getABTitle(Context context) {
		StringBuilder sb = new StringBuilder(context.getString(R.string.map));
		sb.append(" (");
		Set<Integer> filterTypeIds = getFilterTypeIdsOrNull();
		if (CollectionUtils.getSize(filterTypeIds) > 0) {
			boolean hasType = false;
			for (Integer typeId : filterTypeIds) {
				if (typeId != null) {
					DataSourceType type = DataSourceType.parseId(typeId);
					if (type != null) {
						if (hasType) {
							sb.append(", ");
						}
						sb.append(getString(type.getAllStringResId()));
						hasType = true;
					}
				}
			}
		} else {
			sb.append(context.getString(R.string.all));
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public Integer getABBgColor(Context context) {
		return Color.TRANSPARENT;
	}

	private void applyNewFilter(ArrayList<Integer> typeIds, HashSet<Integer> selectedItems) {
		boolean filterChanged = false;
		if (selectedItems.size() == 0 || selectedItems.size() == typeIds.size()) {
			if (this.filterTypeIds.size() > 0) {
				this.filterTypeIds.clear();
				filterChanged = true;
			}
		} else {
			for (int i = 0; i < typeIds.size(); i++) {
				Integer typeId = typeIds.get(i);
				if (selectedItems.contains(i)) {
					if (!this.filterTypeIds.contains(typeId)) {
						this.filterTypeIds.add(typeId);
						filterChanged = true;
					}
				} else {
					if (this.filterTypeIds.contains(typeId)) {
						this.filterTypeIds.remove(typeId);
						filterChanged = true;
					}
				}
			}
		}
		if (filterChanged) {
			saveMapFilterTypeIdsSetting(false); // asynchronous
			applyNewFilterTypeIds();
		}
	}
}
