package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.mtransit.android.R;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.di.Injection;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.task.MapPOILoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MTDialog;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MapFragment extends ABFragment implements
		LoaderManager.LoaderCallbacks<Collection<MapViewController.POIMarker>>,
		MTActivityWithLocation.UserLocationListener,
		IContext,
		MapViewController.MapListener {

	private static final String LOG_TAG = MapFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Map";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_INITIAL_LOCATION = "extra_initial_location";
	private static final String EXTRA_SELECTED_UUID = "extra_selected_uuid";
	private static final String EXTRA_INCLUDE_TYPE_ID = "extra_include_type_id";

	@NonNull
	public static MapFragment newInstance(@Nullable Location optInitialLocation, @Nullable String optSelectedUUID, @Nullable Integer optIncludeTypeId) {
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

	@NonNull
	private final MapViewController mapViewController =
			new MapViewController(LOG_TAG, null, this, true, true, true, false, false, false, 64, false, true, true, false, true);

	@NonNull
	private final LocationPermissionProvider locationPermissionProvider;

	public MapFragment() {
		super();
		this.locationPermissionProvider = Injection.providesLocationPermissionProvider();
	}

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.permissionsGranted(this));
		this.mapViewController.onAttach(activity);
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
		View view = inflater.inflate(R.layout.fragment_map, container, false);
		this.mapViewController.onCreateView(view, savedInstanceState);
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupView(view);
		this.mapViewController.onViewCreated(view, savedInstanceState);
	}

	private void setupView(@SuppressWarnings("unused") View view) {
		// DO NOTHING
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
			if (view != null) {
				view.post(() -> {
					if (MapFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
		}
		this.mapViewController.onResume();
		hasFilterTypeIds(); // triggers markers loading if necessary
		this.mapViewController.showMap(view);
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
		}
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
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null) {
			this.mapViewController.clearMarkers(); // previous marker for unspecific location
			this.loadedLatLngBounds = null; // reset loaded marker area
			this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.permissionsGranted(this));
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
		}
		this.mapViewController.onUserLocationChanged(newLocation);
	}

	private static final int POIS_LOADER = 0;

	@NonNull
	@Override
	public Loader<Collection<MapViewController.POIMarker>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case POIS_LOADER:
			return new MapPOILoader(getContext(), getFilterTypeIdsOrNull(), this.loadingLatLngBounds, this.loadedLatLngBounds);
		default:
			CrashUtils.w(this, "Loader id '%s' unknown!", id);
			//noinspection ConstantConditions // TODO fix latter
			return null;
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Collection<MapViewController.POIMarker>> loader) {
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Collection<MapViewController.POIMarker>> loader, @Nullable Collection<MapViewController.POIMarker> data) {
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

	@Nullable
	private LatLngBounds loadingLatLngBounds = null;
	@Nullable
	private LatLngBounds loadedLatLngBounds = null;

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.mapViewController.onConfigurationChanged(newConfig);
	}

	@Override
	public void onMapClick(LatLng position) {
		// DO NOTHING
	}

	@Override
	public void onCameraChange(LatLngBounds latLngBounds) {
		if (latLngBounds == null) {
			return;
		}
		boolean loaded = this.loadedLatLngBounds != null //
				&& this.loadedLatLngBounds.contains(latLngBounds.northeast) //
				&& this.loadedLatLngBounds.contains(latLngBounds.southwest);
		boolean loading = this.loadingLatLngBounds != null //
				&& this.loadingLatLngBounds.contains(latLngBounds.northeast) //
				&& this.loadingLatLngBounds.contains(latLngBounds.southwest);
		if (!loaded && !loading) {
			this.mapViewController.showLoading();
			LoaderUtils.destroyLoader(this, POIS_LOADER); // cancel now
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
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
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
		if (this.loadFilterTypeIdsTask != null && this.loadFilterTypeIdsTask.getStatus() == LoadFilterTypeIdsTask.Status.RUNNING) {
			return;
		}
		this.loadFilterTypeIdsTask = new LoadFilterTypeIdsTask(this);
		TaskUtils.execute(this.loadFilterTypeIdsTask);
	}

	@Nullable
	private LoadFilterTypeIdsTask loadFilterTypeIdsTask = null;

	private static class LoadFilterTypeIdsTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, MapFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return MapFragment.class.getSimpleName() + ">" + LoadFilterTypeIdsTask.class.getSimpleName();
		}

		LoadFilterTypeIdsTask(@NonNull MapFragment mapFragment) {
			super(mapFragment);
		}

		@Nullable
		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull MapFragment mapFragment, @Nullable Void... params) {
			return mapFragment.initFilterTypeIdsSync();
		}

		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull MapFragment mapFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				mapFragment.applyNewFilterTypeIds();
			}
		}
	}

	private boolean initFilterTypeIdsSync() {
		if (this.filterTypeIds != null) {
			return false;
		}
		final Context context = getContext();
		if (context == null) {
			return false;
		}
		ArrayList<DataSourceType> availableTypes = filterTypes(DataSourceProvider.get(context).getAvailableAgencyTypes());
		Set<String> filterTypeIdStrings = PreferenceUtils.getPrefLcl( //
				context, PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS, PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT);
		this.filterTypeIds = new HashSet<>();
		boolean hasChanged = false;
		if (filterTypeIdStrings != null) {
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
					this.filterTypeIds.add(type.getId());
				} catch (Exception e) {
					MTLog.w(this, e, "Error while parsing filter type ID '%s'!", typeIdString);
					hasChanged = true;
				}
			}
		}
		if (this.includedTypeId != null) {
			if (this.filterTypeIds.size() > 0 && !this.filterTypeIds.contains(this.includedTypeId)) {
				try {
					DataSourceType type = DataSourceType.parseId(this.includedTypeId);
					if (type == null) {
						// DO NOTHING
					} else if (!availableTypes.contains(type)) {
						// DO NOTHING
					} else {
						this.filterTypeIds.add(type.getId());
						hasChanged = true;
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while parsing filter type ID '%s'!", this.includedTypeId);
					hasChanged = true;
				}
			}
			this.includedTypeId = null; // only once
		}
		if (hasChanged) { // old setting not valid anymore
			saveMapFilterTypeIdsSetting(false); // asynchronous
		}
		return this.filterTypeIds != null;
	}

	@NonNull
	private ArrayList<DataSourceType> filterTypes(@NonNull ArrayList<DataSourceType> availableTypes) {
		Iterator<DataSourceType> it = availableTypes.iterator();
		while (it.hasNext()) {
			if (!it.next().isMapScreen()) {
				it.remove();
			}
		}
		return availableTypes;
	}

	private void saveMapFilterTypeIdsSetting(boolean sync) {
		Set<Integer> filterTypeIds = getFilterTypeIdsOrNull();
		if (filterTypeIds == null) {
			return;
		}
		Set<String> newFilterTypeIdStrings = new HashSet<>();
		for (Integer filterTypeId : filterTypeIds) {
			newFilterTypeIdStrings.add(String.valueOf(filterTypeId));
		}
		PreferenceUtils.savePrefLcl(getContext(), PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS, newFilterTypeIdStrings, sync);
	}

	private void applyNewFilterTypeIds() {
		if (this.filterTypeIds == null) {
			return;
		}
		LoaderUtils.destroyLoader(this, POIS_LOADER); // cancel now
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setABTitle(this, getABTitle(requireContext()), true);
		}
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
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_filter:
			Set<Integer> filterTypeIds = getFilterTypeIdsOrNull();
			if (filterTypeIds == null) {
				return false;
			}
			ArrayList<CharSequence> typeNames = new ArrayList<>();
			ArrayList<Boolean> checked = new ArrayList<>();
			final ArrayList<Integer> typeIds = new ArrayList<>();
			final HashSet<Integer> selectedItems = new HashSet<>();
			ArrayList<DataSourceType> availableAgencyTypes = filterTypes(DataSourceProvider.get(getContext()).getAvailableAgencyTypes());
			for (DataSourceType type : availableAgencyTypes) {
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
			new MTDialog.Builder(requireActivity()) //
					.setTitle(R.string.menu_action_filter) //
					.setMultiChoiceItems( //
							typeNames.toArray(new CharSequence[0]), //
							checkedItems, //
							(dialog, which, isChecked) -> {
								if (isChecked) {
									selectedItems.add(which);
								} else {
									selectedItems.remove(which);
								}
							} //
					) //
					.setPositiveButton(android.R.string.ok, (dialog, which) -> {
						applyNewFilter(typeIds, selectedItems);
						dialog.dismiss();
					}) //
					.setNegativeButton(android.R.string.cancel, (dialog, which) ->
							dialog.dismiss()
					) //
					.setCancelable(true) //
					.create()
					.show();

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
	public void onSaveInstanceState(@NonNull Bundle outState) {
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
		TaskUtils.cancelQuietly(this.loadFilterTypeIdsTask, true);
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
		if (filterTypeIds != null && filterTypeIds.size() > 0) {
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
		if (this.filterTypeIds == null) {
			return;
		}
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
