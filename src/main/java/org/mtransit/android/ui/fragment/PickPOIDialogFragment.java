package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.lifecycle.LifecycleOwner;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.POIsLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import java.util.ArrayList;

import static org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES;

public class PickPOIDialogFragment extends MTDialogFragmentX implements
		LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		org.mtransit.android.data.DataSourceProvider.ModulesUpdateListener,
		MTActivityWithLocation.UserLocationListener,
		POIArrayAdapter.OnClickHandledListener,
		IActivity {

	private static final String TAG = PickPOIDialogFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String EXTRA_POI_UUIDS = "extra_poi_uuids";
	private static final String EXTRA_POI_AUTHORITIES = "extra_poi_authorities";

	@NonNull
	public static PickPOIDialogFragment newInstance(@NonNull ArrayMap<String, String> uuidsAndAuthorities) {
		ArrayList<String> uuids = new ArrayList<>();
		ArrayList<String> authorities = new ArrayList<>();
		if (uuidsAndAuthorities != null) {
			for (ArrayMap.Entry<String, String> uuidAndAuthority : uuidsAndAuthorities.entrySet()) {
				uuids.add(uuidAndAuthority.getKey());
				authorities.add(uuidAndAuthority.getValue());
			}
		}
		return newInstance(uuids, authorities);
	}

	@NonNull
	public static PickPOIDialogFragment newInstance(@NonNull ArrayList<String> uuids, @NonNull ArrayList<String> authorities) {
		PickPOIDialogFragment f = new PickPOIDialogFragment();
		Bundle args = new Bundle();
		args.putStringArrayList(EXTRA_POI_UUIDS, uuids);
		f.uuids = uuids;
		args.putStringArrayList(EXTRA_POI_AUTHORITIES, authorities);
		f.authorities = authorities;
		f.setArguments(args);
		return f;
	}

	@Nullable
	private ArrayList<String> uuids = null;
	@Nullable
	private ArrayList<String> authorities = null;
	@Nullable
	private POIArrayAdapter adapter = null;
	private boolean modulesUpdated = false;
	@Nullable
	private Location userLocation = null;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public PickPOIDialogFragment() {
		super();
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(this);
	}

	private void initAdapters(@NonNull IActivity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setOnClickHandledListener(this);
		this.adapter.setTag(getLogTag());
	}

	@Override
	public void onLeaving() {
		dismiss();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
		if (!F_CACHE_DATA_SOURCES) {
			org.mtransit.android.data.DataSourceProvider.addModulesUpdateListener(this);
		} else {
			this.dataSourcesRepository.readingAllAgencyAuthorities().observe(this, agencyAuthorities -> {
				if (this.authorities == null) {
					return;
				}
				for (String authority : this.authorities) {
					if (agencyAuthorities.contains(authority)) {
						continue;
					}
					MTLog.d(this, "Authority %s doesn't exist anymore, dismissing dialog.", authority);
					dismiss();
					break;
				}
			});
		}
	}

	private void restoreInstanceState(Bundle... bundles) {
		ArrayList<String> newUUIDs = BundleUtils.getStringArrayList(EXTRA_POI_UUIDS, bundles);
		if (CollectionUtils.getSize(newUUIDs) > 0) {
			this.uuids = newUUIDs;
		}
		ArrayList<String> newAuthorities = BundleUtils.getStringArrayList(EXTRA_POI_AUTHORITIES, bundles);
		if (CollectionUtils.getSize(newAuthorities) > 0) {
			this.authorities = newAuthorities;
		}
	}

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		if (!F_CACHE_DATA_SOURCES) {
			dismiss();
		}
		this.modulesUpdated = false; // processed
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_dialog_pick_poi, container, false);
		setupView(view);
		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		inflateList(view);
		this.adapter.setManualScrollView(view.findViewById(R.id.scrollview));
		this.adapter.setManualLayout(view.findViewById(R.id.list));
		switchView(view);
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
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
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
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (CollectionUtils.getSize(this.uuids) > 0) {
			outState.putStringArrayList(EXTRA_POI_UUIDS, this.uuids);
		}
		if (CollectionUtils.getSize(this.authorities) > 0) {
			outState.putStringArrayList(EXTRA_POI_AUTHORITIES, this.authorities);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(() -> {
					if (PickPOIDialogFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
		}
		switchView(view);
		if (this.adapter != null && this.adapter.isInitialized()) {
			this.adapter.onResume(this, this.userLocation);
		} else {
			LoaderUtils.restartLoader(this, POIS_LOADER, null, this);
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
	}

	private static final int POIS_LOADER = 0;

	@NonNull
	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case POIS_LOADER:
			if (this.uuids == null || this.authorities == null) {
				//noinspection deprecation
				CrashUtils.w(this, "onCreateLoader() > skip (no authority)");
				return null;
			}
			return new POIsLoader(requireContext(), this.uuids, this.authorities);
		default:
			//noinspection deprecation
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
		if (this.adapter != null) {
			this.adapter.setPois(data);
			this.adapter.updateDistanceNowAsync(this.userLocation);
			this.adapter.initManual();
		}
		switchView(getView());
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation != null) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				if (this.adapter != null) {
					this.adapter.setLocation(newLocation);
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
		if (!F_CACHE_DATA_SOURCES) {
			org.mtransit.android.data.DataSourceProvider.removeModulesUpdateListener(this);
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
}
