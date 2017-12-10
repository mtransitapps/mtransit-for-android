package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.AgencyPOIsLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.util.LoaderUtils;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.CompoundButton;
import android.widget.TextView;

public class AgencyPOIsFragment extends MTFragmentV4 implements AgencyTypeFragment.AgencyFragment, LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		MTActivityWithLocation.UserLocationListener, MapViewController.MapMarkerProvider, MapViewController.MapListener,
		CompoundButton.OnCheckedChangeListener {

	private static final String TAG = AgencyPOIsFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.authority;
	}

	private static final String EXTRA_AGENCY_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_COLOR_INT = "extra_color_int";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_SHOWING_LIST_INSTEAD_OF_MAP = "extra_showing_list_instead_of_map";

	public static AgencyPOIsFragment newInstance(int fragmentPosition, int lastVisibleFragmentPosition, @NonNull String agencyAuthority,
			@Nullable Integer optColorInt, @Nullable Boolean optShowingListInsteadOfMap) {
		AgencyPOIsFragment f = new AgencyPOIsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AGENCY_AUTHORITY, agencyAuthority);
		f.authority = agencyAuthority;
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
			f.fragmentPosition = fragmentPosition;
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
			f.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}
		if (optColorInt != null) {
			args.putInt(EXTRA_COLOR_INT, optColorInt);
			f.colorInt = optColorInt;
		}
		if (optShowingListInsteadOfMap != null) {
			args.putBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, optShowingListInsteadOfMap);
			f.showingListInsteadOfMap = optShowingListInsteadOfMap;
		}
		f.setArguments(args);
		return f;
	}

	private Location userLocation;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private POIArrayAdapter adapter;
	private String emptyText = null;
	private String authority;
	private Integer colorInt;
	private MapViewController mapViewController =
			new MapViewController(TAG, this, this, true, true, true, false, false, false, 0, false, true, false, true, false);

	@Override
	public String getAgencyAuthority() {
		return this.authority;
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
		restoreInstanceState(savedInstanceState, getArguments());
		this.mapViewController.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_agency_pois, container, false);
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
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AGENCY_AUTHORITY, this.authority);
		}
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
		}
		if (this.colorInt != null) {
			outState.putInt(EXTRA_COLOR_INT, this.colorInt);
		}
		if (this.showingListInsteadOfMap != null) {
			outState.putBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, this.showingListInsteadOfMap);
		}
		this.mapViewController.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AGENCY_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
		}
		Boolean newShowingListInsteadOfMap = BundleUtils.getBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, bundles);
		if (newShowingListInsteadOfMap != null) {
			this.showingListInsteadOfMap = newShowingListInsteadOfMap;
		}
		Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, bundles);
		if (fragmentPosition != null) {
			if (fragmentPosition >= 0) {
				this.fragmentPosition = fragmentPosition;
			} else {
				this.fragmentPosition = -1;
			}
		}
		Integer newColorInt = BundleUtils.getInt(EXTRA_COLOR_INT, bundles);
		if (newColorInt != null) {
			this.colorInt = newColorInt;
		}
		Integer lastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, bundles);
		if (lastVisibleFragmentPosition != null) {
			if (lastVisibleFragmentPosition >= 0) {
				this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
		this.adapter.setTag(this.authority);
		this.mapViewController.setTag(getLogTag());
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
	}

	@Override
	public POIManager getClosestPOI() {
		return this.adapter == null ? null : this.adapter.getClosestPOI();
	}

	@Override
	public POIManager getPOI(String uuid) {
		return this.adapter == null ? null : this.adapter.getItem(uuid);
	}

	@Override
	public Collection<POIManager> getPOIs() {
		if (this.adapter == null || !this.adapter.isInitialized()) {
			return null;
		}
		HashSet<POIManager> pois = new HashSet<POIManager>();
		if (this.adapter != null && this.adapter.hasPois()) {
			for (int i = 0; i < this.adapter.getPoisCount(); i++) {
				pois.add(this.adapter.getItem(i));
			}
		}
		return pois;
	}

	@Override
	public Collection<MapViewController.POIMarker> getPOMarkers() {
		return null;
	}

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
	}

	private void linkAdapterWithListView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		View listView = view.findViewById(R.id.list);
		if (listView != null) {
			this.adapter.setListView((AbsListView) listView);
		}
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		if (isShowingListInsteadOfMap()) { // showing list
			inflateList(view);
			switchView(view);
		}
	}

	@Override
	public void setFragmentPosition(int fragmentPosition) {
		this.fragmentPosition = fragmentPosition;
		setFragmentVisibleAtPosition(this.lastVisibleFragmentPosition); // force reset visibility
	}

	@Override
	public void setFragmentVisibleAtPosition(int visibleFragmentPosition) {
		if (this.lastVisibleFragmentPosition == visibleFragmentPosition //
				&& ((this.fragmentPosition == visibleFragmentPosition && this.fragmentVisible) //
				|| //
				(this.fragmentPosition != visibleFragmentPosition && !this.fragmentVisible)) //
				) {
			return;
		}
		this.lastVisibleFragmentPosition = visibleFragmentPosition;
		if (this.fragmentPosition < 0) {
			return;
		}
		if (this.fragmentPosition == visibleFragmentPosition) {
			onFragmentVisible();
		} else {
			onFragmentInvisible();
		}
	}

	private void onFragmentInvisible() {
		if (!this.fragmentVisible) {
			return; // already invisible
		}
		this.fragmentVisible = false;
		if (this.adapter != null) {
			this.adapter.onPause();
		}
		if (!isShowingListInsteadOfMap()) { // map
			this.mapViewController.onPause();
		}
	}

	@Override
	public boolean isFragmentVisible() {
		return this.fragmentVisible;
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		if (!isResumed()) {
			return;
		}
		this.fragmentVisible = true;
		if (!isShowingListInsteadOfMap()) { // map
			this.mapViewController.onResume();
		}
		switchView(getView());
		if (!this.adapter.isInitialized()) {
			LoaderUtils.restartLoader(this, POIS_LOADER, null, this);
		} else {
			this.adapter.onResume(getActivity(), this.userLocation);
		}
		checkIfShowingListInsteadOfMapChanged();
		if (getActivity() != null) {
			getActivity().invalidateOptionsMenu(); // initialize action bar list/map switch icon
		}
		updateListMapToggleMenuItem();
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
	}

	private static final int POIS_LOADER = 0;

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			if (TextUtils.isEmpty(this.authority)) {
				return null;
			}
			return new AgencyPOIsLoader(getActivity(), this.authority);
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
		this.mapViewController.notifyMarkerChanged(this);
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
		this.adapter.setPois(data);
		this.mapViewController.notifyMarkerChanged(this);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		switchView(getView());
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			if (this.adapter != null) {
				this.adapter.setLocation(newLocation);
			}
		}
		this.mapViewController.onUserLocationChanged(newLocation);
	}

	@Override
	public void onPause() {
		super.onPause();
		onFragmentInvisible();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.fragmentPosition >= 0 && this.fragmentPosition == this.lastVisibleFragmentPosition) {
			onFragmentVisible();
		} // ELSE would be call later
		if (this.adapter != null) {
			this.adapter.setActivity(getActivity());
		}
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
		}
		this.mapViewController.onDestroy();
	}

	private MenuItem listMapToggleMenuItem;
	private SwitchCompat listMapSwitchMenuItem;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (this.fragmentVisible) {
			if (menu.findItem(R.id.menu_toggle_list_map) == null) {
				inflater.inflate(R.menu.menu_agency_pois, menu);
			}
			this.listMapToggleMenuItem = menu.findItem(R.id.menu_toggle_list_map);
			this.listMapSwitchMenuItem = (SwitchCompat) this.listMapToggleMenuItem.getActionView().findViewById(R.id.action_bar_switch_list_map);
			this.listMapSwitchMenuItem.setThumbDrawable(getListMapToggleSelector());
		} else {
			if (this.listMapSwitchMenuItem != null) {
				this.listMapSwitchMenuItem.setOnCheckedChangeListener(null);
				this.listMapSwitchMenuItem.setVisibility(View.GONE);
				this.listMapSwitchMenuItem = null;
			}
			if (this.listMapToggleMenuItem != null) {
				this.listMapToggleMenuItem.setVisible(false);
				this.listMapSwitchMenuItem = null;
			}
		}
		updateListMapToggleMenuItem();
	}

	private StateListDrawable listMapToggleSelector = null;

	@NonNull
	private StateListDrawable getListMapToggleSelector() {
		if (listMapToggleSelector == null) {
			listMapToggleSelector = new StateListDrawable();
			LayerDrawable listLayerDrawable = (LayerDrawable) SupportFactory.get().getResourcesDrawable(getResources(), R.drawable.switch_thumb_list, null);
			GradientDrawable listOvalShape = (GradientDrawable) listLayerDrawable.findDrawableByLayerId(R.id.switch_list_oval_shape);
			if (this.colorInt != null) {
				listOvalShape.setColor(this.colorInt);
			}
			listMapToggleSelector.addState(new int[]{android.R.attr.state_checked}, listLayerDrawable);
			LayerDrawable mapLayerDrawable = (LayerDrawable) SupportFactory.get().getResourcesDrawable(getResources(), R.drawable.switch_thumb_map, null);
			GradientDrawable mapOvalShape = (GradientDrawable) mapLayerDrawable.findDrawableByLayerId(R.id.switch_map_oval_shape);
			if (this.colorInt != null) {
				mapOvalShape.setColor(this.colorInt);
			}
			listMapToggleSelector.addState(StateSet.WILD_CARD, mapLayerDrawable);
		}
		return this.listMapToggleSelector;
	}

	private void updateListMapToggleMenuItem() {
		if (!this.fragmentVisible) {
			return;
		}
		if (this.listMapToggleMenuItem == null) {
			return;
		}
		if (this.listMapSwitchMenuItem == null) {
			return;
		}
		boolean showingListInsteadOfMap = isShowingListInsteadOfMap();
		this.listMapSwitchMenuItem.setChecked(showingListInsteadOfMap);
		this.listMapSwitchMenuItem.setOnCheckedChangeListener(this);
		this.listMapSwitchMenuItem.setVisibility(View.VISIBLE);
		this.listMapToggleMenuItem.setVisible(true);
	}

	private Boolean showingListInsteadOfMap = null;

	private boolean isShowingListInsteadOfMap() {
		if (this.showingListInsteadOfMap == null) {
			boolean showingListInsteadOfMapLastSet = PreferenceUtils.getPrefDefault( //
					getContext(), //
					PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET,
					PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT);
			this.showingListInsteadOfMap = TextUtils.isEmpty(this.authority) ? showingListInsteadOfMapLastSet : PreferenceUtils.getPrefDefault(getActivity(),
					PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(this.authority), showingListInsteadOfMapLastSet);
		}
		return this.showingListInsteadOfMap;
	}

	private void checkIfShowingListInsteadOfMapChanged() {
		if (this.showingListInsteadOfMap == null) {
			return;
		}
		boolean showingListInsteadOfMapLastSet = PreferenceUtils.getPrefDefault(getActivity(),
				PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET, PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT);
		boolean newShowingListInsteadOfMap = TextUtils.isEmpty(this.authority) ? showingListInsteadOfMapLastSet : PreferenceUtils.getPrefDefault(getActivity(),
				PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(this.authority), showingListInsteadOfMapLastSet);
		if (newShowingListInsteadOfMap != this.showingListInsteadOfMap) {
			setShowingListInsteadOfMap(newShowingListInsteadOfMap);
		}
	}

	private void setShowingListInsteadOfMap(boolean newShowingListInsteadOfMap) {
		if (this.showingListInsteadOfMap != null && this.showingListInsteadOfMap == newShowingListInsteadOfMap) {
			return; // nothing changed
		}
		this.showingListInsteadOfMap = newShowingListInsteadOfMap; // switching
		PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET, this.showingListInsteadOfMap,
				false);
		if (!TextUtils.isEmpty(this.authority)) {
			PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(this.authority),
					this.showingListInsteadOfMap, false);
		}
		if (this.adapter != null) {
			View view = getView();
			setupView(view);
			if (!this.showingListInsteadOfMap) { // map
				this.mapViewController.onResume();
			} else { // list
				this.mapViewController.onPause();
			}
			switchView(view);
		}
		updateListMapToggleMenuItem();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!this.fragmentVisible) {
			return false; // not handled
		}
		switch (item.getItemId()) {
		case R.id.menu_toggle_list_map:
			setShowingListInsteadOfMap(!isShowingListInsteadOfMap()); // switching
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (!this.fragmentVisible) {
			return;
		}
		if (buttonView.getId() == R.id.action_bar_switch_list_map) {
			setShowingListInsteadOfMap(isChecked);
		}
	}

	private void switchView(View view) {
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty(view);
		} else {
			showListOrMap(view);
		}
	}

	private void showListOrMap(View view) {
		if (isShowingListInsteadOfMap()) { // list
			this.mapViewController.hideMap();
			inflateList(view);
			view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
		} else { // map
			this.mapViewController.showMap(view);
			if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
				view.findViewById(R.id.list).setVisibility(View.GONE); // hide
			}
		}
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
			((AbsListView) view.findViewById(R.id.list)).setFastScrollEnabled(true); // long list
			linkAdapterWithListView(view);
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		this.mapViewController.hideMap();
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		this.mapViewController.hideMap();
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}
}
