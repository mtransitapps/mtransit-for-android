package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.NearbyPOIListLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.android.ui.widget.ListViewSwipeRefreshLayout;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class NearbyAgencyTypeFragment extends MTFragmentX implements VisibilityAwareFragment,
		LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		NearbyFragment.NearbyLocationListener,
		POIArrayAdapter.InfiniteLoadingListener,
		IActivity {

	private static final String LOG_TAG = NearbyAgencyTypeFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG + "-" + this.typeId;
	}

	private static final String EXTRA_TYPE_ID = "extra_type_id";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";

	@NonNull
	public static NearbyAgencyTypeFragment newInstance(int fragmentPosition,
													   int lastVisibleFragmentPosition,
													   int typeId,
													   @SuppressWarnings("unused") @Nullable Location optNearbyLocation) {
		NearbyAgencyTypeFragment f = new NearbyAgencyTypeFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE_ID, typeId);
		f.typeId = typeId;
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
			f.fragmentPosition = fragmentPosition;
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
			f.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}
		f.setArguments(args);
		return f;
	}

	@Nullable
	private POIArrayAdapter adapter;
	@NonNull
	private LocationUtils.AroundDiff ad = LocationUtils.getNewDefaultAroundDiff();
	@Nullable
	private Double lastEmptyAroundDiff = null;
	@Nullable
	private Location nearbyLocation;
	@Nullable
	private Location userLocation;
	@Nullable
	private ListViewSwipeRefreshLayout swipeRefreshLayout;
	private boolean swipeRefreshLayoutEnabled = true;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	@Nullable
	private WeakReference<NearbyFragment> nearbyFragmentWR;

	@SuppressWarnings("FieldCanBeLocal")
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	@NonNull
	private final LiveData<List<AgencyProperties>> allAgenciesLD;
	@NonNull
	private final MutableLiveData<Integer> typeIdLD = new MutableLiveData<>(this.typeId);
	@SuppressWarnings("ConstantConditions")
	@NonNull
	private final MutableLiveData<Location> nearbyLocationLD = new MutableLiveData<>(this.nearbyLocation);
	@NonNull
	private final MutableLiveData<LocationUtils.AroundDiff> adLD = new MutableLiveData<>(this.ad);
	@NonNull
	private final MediatorLiveData<List<String>> typeAgenciesLD = new MediatorLiveData<>();

	public NearbyAgencyTypeFragment() {
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
		this.allAgenciesLD = this.dataSourcesRepository.readingAllAgenciesDistinct();
	}

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(this);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
		this.typeAgenciesLD.addSource(this.typeIdLD, newTypeId ->
				this.typeAgenciesLD.setValue(
						makeTypeAgencies(newTypeId, this.allAgenciesLD.getValue(), this.nearbyLocationLD.getValue(), this.adLD.getValue())
				)
		);
		this.typeAgenciesLD.addSource(this.allAgenciesLD, newAgencies ->
				this.typeAgenciesLD.setValue(
						makeTypeAgencies(this.typeIdLD.getValue(), newAgencies, this.nearbyLocationLD.getValue(), this.adLD.getValue())
				)
		);
		this.typeAgenciesLD.addSource(this.nearbyLocationLD, newNearbyLocation ->
				this.typeAgenciesLD.setValue(
						makeTypeAgencies(this.typeIdLD.getValue(), this.allAgenciesLD.getValue(), newNearbyLocation, this.adLD.getValue())
				)
		);
		this.typeAgenciesLD.addSource(this.adLD, newAd ->
				this.typeAgenciesLD.setValue(
						makeTypeAgencies(this.typeIdLD.getValue(), this.allAgenciesLD.getValue(), this.nearbyLocationLD.getValue(), newAd)
				)
		);
		this.typeAgenciesLD.observe(this, newTypeAgenciesAuthority -> {
					if (CollectionUtils.getSize(this.typeAgenciesAuthority) != CollectionUtils.getSize(newTypeAgenciesAuthority)) {
						this.typeAgenciesAuthority = newTypeAgenciesAuthority;
						useNewNearbyLocation(this.nearbyLocation, true); // force
						applyNewTypeAgenciesAuthority();
					} else if (newTypeAgenciesAuthority != null && this.typeAgenciesAuthority == null) { // not agencies in area, need to increase area
						MTLog.d(this, "onChanged() > increase area (because no agencies in current area)");
						LocationUtils.incAroundDiff(this.ad); // increase covered area
						this.adLD.postValue(this.ad); // triggering new combine
					}
				}
		);
		this.typeIdLD.postValue(this.typeId); // trigger once
		this.nearbyLocationLD.postValue(this.nearbyLocation); // trigger once
		this.adLD.postValue(this.ad); // trigger once
	}

	@Nullable
	private List<String> makeTypeAgencies(@Nullable Integer typeId,
										  @Nullable List<AgencyProperties> allAgencies,
										  @Nullable Location nearbyLocation,
										  @Nullable LocationUtils.AroundDiff ad) {
		if (typeId == null || allAgencies == null || nearbyLocation == null || ad == null) {
			MTLog.d(this, "combine() > SKIP (missing data)");
			return null;
		}
		if (allAgencies.isEmpty()) {
			MTLog.d(this, "combine() > SKIP (no more agency installed)");
			return null; // do NOT expand area
		}
		List<AgencyProperties> filteredAgencyType = new ArrayList<>();
		for (AgencyProperties agency : allAgencies) { // already sorted
			if (agency.getType().getId() != typeId) {
				continue;
			}
			filteredAgencyType.add(agency);
		}
		if (filteredAgencyType.isEmpty()) {
			MTLog.d(this, "combine() > SKIP (no more type agency installed)");
			return null; // do NOT expand area
		}
		return NearbyPOIListLoader.filterAgenciesAuthorityInArea(
				filteredAgencyType,
				nearbyLocation.getLatitude(),
				nearbyLocation.getLongitude(),
				ad.aroundDiff,
				this.lastEmptyAroundDiff
		);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_nearby_agency_type, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (this.typeId != null) {
			outState.putInt(EXTRA_TYPE_ID, this.typeId);
		}
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
		}
		super.onSaveInstanceState(outState);
	}

	@Nullable
	private Integer typeId;

	@Nullable
	Integer getTypeId() {
		return this.typeId;
	}

	@Nullable
	private List<String> typeAgenciesAuthority;

	@MainThread
	private void applyNewTypeAgenciesAuthority() {
		if (this.typeAgenciesAuthority == null) {
			return;
		}
		if (this.nearbyLocation != null) {
			LoaderUtils.restartLoader(this, NEARBY_POIS_LOADER, null, this);
		}
	}

	private void setupView(@NonNull View view) {
		this.swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
		this.swipeRefreshLayout.setColorSchemeColors(ThemeUtils.resolveColorAttribute(view.getContext(), R.attr.colorAccent));
		setSwipeRefreshLayoutEnabled(this.swipeRefreshLayoutEnabled);
		inflateList(view);
		switchView(view);
		linkAdapterWithListView(view);
		NearbyFragment nearbyFragment = this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get();
		if (nearbyFragment != null) {
			this.swipeRefreshLayout.setOnRefreshListener(nearbyFragment);
		}
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

	private void restoreInstanceState(Bundle... bundles) {
		Integer newTypeId = BundleUtils.getInt(EXTRA_TYPE_ID, bundles);
		if (newTypeId != null && !newTypeId.equals(this.typeId)) {
			this.typeId = newTypeId;
			this.typeIdLD.postValue(this.typeId);
		}
		Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, bundles);
		if (fragmentPosition != null) {
			if (fragmentPosition >= 0) {
				this.fragmentPosition = fragmentPosition;
			} else {
				this.fragmentPosition = -1;
			}
		}
		Integer lastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, bundles);
		if (lastVisibleFragmentPosition != null) {
			if (lastVisibleFragmentPosition >= 0) {
				this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
		this.adapter.setTag(String.valueOf(this.typeId));
	}

	private void initAdapters(IActivity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setInfiniteLoading(true);
		this.adapter.setInfiniteLoadingListener(this);
	}

	@MainThread
	private void doLoadMore() {
		this.minSize *= 2;
		this.maxSize *= 2;
		this.minCoverageInMeters *= 2;
		LocationUtils.incAroundDiff(this.ad);
		this.adLD.postValue(this.ad);
		LoaderUtils.restartLoader(this, NEARBY_POIS_LOADER, null, this);
	}

	@Override
	public boolean showingDone() {
		return true;
	}

	@Override
	public boolean isLoadingMore() {
		if (this.nearbyLocation == null) {
			return false;
		}
		if (this.minSize < 0 || this.maxSize < 0 || this.minCoverageInMeters < 0f) {
			return false;
		}
		if (LocationUtils.searchComplete(this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(), this.ad.aroundDiff)) {
			return false;
		}
		if (this.sizeCovered < this.minSize) {
			return true; // already loading
		}
		if (this.distanceCoveredInMeters < this.minCoverageInMeters) {
			return true; // already loading
		}
		doLoadMore();
		return true; // now loading
	}

	void setNearbyFragment(@Nullable NearbyFragment nearbyFragment) {
		this.nearbyFragmentWR = new WeakReference<>(nearbyFragment);
	}

	void setSwipeRefreshLayoutRefreshing(boolean refreshing) {
		if (this.swipeRefreshLayout != null) {
			if (refreshing) {
				if (!this.swipeRefreshLayout.isRefreshing()) {
					this.swipeRefreshLayout.setRefreshing(true);
				}
			} else {
				this.swipeRefreshLayout.setRefreshing(false);
			}
		}
	}

	void setSwipeRefreshLayoutEnabled(boolean swipeRefreshLayoutEnabled) {
		this.swipeRefreshLayoutEnabled = swipeRefreshLayoutEnabled;
		if (this.swipeRefreshLayout != null) {
			this.swipeRefreshLayout.setRefreshEnabled(swipeRefreshLayoutEnabled);
		}
	}

	@Override
	public void setFragmentPosition(int fragmentPosition) {
		this.fragmentPosition = fragmentPosition;
		setFragmentVisibleAtPosition(this.lastVisibleFragmentPosition); // force reset visibility
	}

	@Override
	public void setFragmentVisibleAtPosition(int visibleFragmentPosition) {
		if (this.lastVisibleFragmentPosition == visibleFragmentPosition) { //
			if ((this.fragmentPosition == visibleFragmentPosition && this.fragmentVisible) //
					|| (this.fragmentPosition != visibleFragmentPosition && !this.fragmentVisible)) {
				return;
			}
		}
		this.lastVisibleFragmentPosition = visibleFragmentPosition;
		if (this.fragmentPosition < 0) {
			MTLog.d(this, "setFragmentVisibleAtPosition() > SKIP (NO fragment position).");
			return;
		}
		if (!isResumed()) {
			return;
		}
		if (this.fragmentPosition == visibleFragmentPosition) {
			onFragmentVisible();
		} else {
			onFragmentInvisible();
		}
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		if (!isResumed()) {
			return;
		}
		this.fragmentVisible = true;
		switchView(getView());
		if (this.adapter != null) {
			this.adapter.onResume(this, this.userLocation);
		}
		NearbyFragment nearbyFragment = this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get();
		if (nearbyFragment != null) {
			useNewNearbyLocation(nearbyFragment.getNearbyLocation(), false); // nearby location was unknown yet or reset while not visible
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
	}

	@Override
	public boolean isFragmentVisible() {
		return this.fragmentVisible;
	}

	private void onFragmentInvisible() {
		if (!this.fragmentVisible) {
			return; // already invisible
		}
		this.fragmentVisible = false;
		if (this.adapter != null) {
			this.adapter.onPause();
		}
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
		} // ELSE will be called later
		this.adapter.setActivity(this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (this.swipeRefreshLayout != null) {
			this.swipeRefreshLayout.setOnRefreshListener(null);
			this.swipeRefreshLayout = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
	}

	@Override
	public void onNearbyLocationChanged(@Nullable Location newLocation) {
		if (newLocation != null) {
			useNewNearbyLocation(newLocation, false); // true);
		} else {
			useNewNearbyLocation(null, true);
		}
	}

	@MainThread
	private void useNewNearbyLocation(@Nullable Location newNearbyLocation, boolean force) {
		if (!force) {
			if (newNearbyLocation == null
					|| !this.fragmentVisible
					|| LocationUtils.areTheSame(newNearbyLocation, this.nearbyLocation)) {
				MTLog.d(this, "useNewNearbyLocation() > SKIP (not visible OR no location OR same location)");
				return;
			}
		}
		this.nearbyLocation = newNearbyLocation;
		this.nearbyLocationLD.postValue(this.nearbyLocation);
		if (this.nearbyLocation == null) {
			this.sizeCovered = 0;
			this.distanceCoveredInMeters = 0f;
			this.minSize = -1;
			this.maxSize = -1;
			this.minCoverageInMeters = -1f;
			this.ad = LocationUtils.getNewDefaultAroundDiff();
			this.adLD.postValue(this.ad);
			this.lastEmptyAroundDiff = null;
		} else if (this.minSize < 0 || this.maxSize < 0 || this.minCoverageInMeters < 0f) {
			this.sizeCovered = 0;
			this.distanceCoveredInMeters = 0f;
			this.minSize = LocationUtils.MIN_NEARBY_LIST;
			this.maxSize = LocationUtils.MAX_NEARBY_LIST;
			this.ad = LocationUtils.getNewDefaultAroundDiff();
			this.adLD.postValue(this.ad);
			this.lastEmptyAroundDiff = null;
			this.minCoverageInMeters =
					LocationUtils.getAroundCoveredDistanceInMeters(this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(), this.ad.aroundDiff);
			if (this.minCoverageInMeters < LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS) {
				this.minCoverageInMeters = LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS;
			}
			if (this.minCoverageInMeters < this.nearbyLocation.getAccuracy()) {
				this.minCoverageInMeters = this.nearbyLocation.getAccuracy();
			}
		}
		if (this.adapter != null) {
			this.adapter.clear();
		}
		switchView(getView());
		if (this.nearbyLocation != null && this.typeAgenciesAuthority != null) {
			LoaderUtils.restartLoader(this, NEARBY_POIS_LOADER, null, this);
		}
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.fragmentVisible) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				if (this.adapter != null) {
					this.adapter.setLocation(newLocation);
				}
			}
		}
	}

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty(view);
		} else {
			showList(view);
		}
	}

	private void showList(View view) {
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		inflateList(view);
		view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
			this.swipeRefreshLayout.setListViewWR(view.findViewById(R.id.list));
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		this.swipeRefreshLayout.setLoadingViewWR(view.findViewById(R.id.loading));
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
			this.swipeRefreshLayout.setEmptyViewWR(view.findViewById(R.id.empty));
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	private static final int NEARBY_POIS_LOADER = 0;

	private int sizeCovered = 0;

	private float distanceCoveredInMeters = 0f;

	private int maxSize = -1;

	private int minSize = -1;

	private float minCoverageInMeters = -1f;

	@NonNull
	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case NEARBY_POIS_LOADER:
			if (this.nearbyLocation == null || this.typeId == null || this.typeAgenciesAuthority == null) {
				//noinspection deprecation
				CrashUtils.w(this, "onCreateLoader() > nearby location or type not available yet.");
				//noinspection ConstantConditions // FIXME
				return null;
			}
			return new NearbyPOIListLoader(
					requireContext(),
					this.nearbyLocation.getLatitude(),
					this.nearbyLocation.getLongitude(),
					this.ad.aroundDiff,
					this.minCoverageInMeters,
					this.maxSize,
					false,
					true,
					this.typeAgenciesAuthority
			);
		default:
			//noinspection deprecation
			CrashUtils.w(this, "Loader id '%s' unknown!", id);
			//noinspection ConstantConditions // FIXME
			return null;
		}
	}

	@Override
	public void finish() {
		requireActivity().finish();
	}

	@Nullable
	@Override
	public <T extends View> T findViewById(int id) {
		if (getView() == null) {
			return null;
		}
		return getView().findViewById(id);
	}

	@NonNull
	@Override
	public LifecycleOwner getLifecycleOwner() {
		return this;
	}

	@Override
	public void onLoaderReset(@NonNull Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@MainThread
	@Override
	public void onLoadFinished(@NonNull Loader<ArrayList<POIManager>> loader, @Nullable ArrayList<POIManager> data) {
		final int dataSize = CollectionUtils.getSize(data);
		if (dataSize < this.minSize //
				&& this.nearbyLocation != null
				&& !LocationUtils.searchComplete(this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(), this.ad.aroundDiff)) {
			if (dataSize == 0) {
				this.lastEmptyAroundDiff = this.ad.aroundDiff;
			} else {
				this.lastEmptyAroundDiff = null;
			}
			LocationUtils.incAroundDiff(this.ad); // increase covered area
			this.adLD.postValue(this.ad);
			applyNewTypeAgenciesAuthority();
		} else {
			this.distanceCoveredInMeters = this.minCoverageInMeters;
			this.sizeCovered = data == null ? 0 : data.size();
			boolean scrollToTop = this.adapter.getPoisCount() == 0;
			this.adapter.appendPois(data);
			View view = getView();
			if (scrollToTop && view != null && view.findViewById(R.id.list) != null) {
				((AbsListView) view.findViewById(R.id.list)).setSelection(0);
			}
			if (this.fragmentVisible) {
				this.adapter.updateDistanceNowAsync(this.userLocation);
			} else {
				this.adapter.onPause();
			}
			switchView(view);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return NearbyAgencyTypeFragment.class.getSimpleName() + "{" +
				"type=" + typeId +
				", pos=" + fragmentPosition +
				", visible=" + fragmentVisible +
				'}';
	}
}
