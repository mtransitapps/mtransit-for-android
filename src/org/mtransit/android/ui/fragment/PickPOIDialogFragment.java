package org.mtransit.android.ui.fragment;

import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.POIsLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import android.app.Activity;
import android.app.Dialog;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.widget.ScrollView;

public class PickPOIDialogFragment extends MTDialogFragmentV4 implements LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		DataSourceProvider.ModulesUpdateListener, MTActivityWithLocation.UserLocationListener, POIArrayAdapter.OnClickHandledListener {

	private static final String TAG = PickPOIDialogFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String EXTRA_POI_UUIDS = "extra_poi_uuids";
	private static final String EXTRA_POI_AUTHORITIES = "extra_poi_authorities";

	public static PickPOIDialogFragment newInstance(ArrayMap<String, String> uuidsAndAuthorities) {
		ArrayList<String> uuids = new ArrayList<String>();
		ArrayList<String> authorities = new ArrayList<String>();
		if (uuidsAndAuthorities != null) {
			for (ArrayMap.Entry<String, String> uuidAndAuthority : uuidsAndAuthorities.entrySet()) {
				uuids.add(uuidAndAuthority.getKey());
				authorities.add(uuidAndAuthority.getValue());
			}
		}
		return newInstance(uuids, authorities);
	}

	public static PickPOIDialogFragment newInstance(ArrayList<String> uuids, ArrayList<String> authorities) {
		PickPOIDialogFragment f = new PickPOIDialogFragment();
		Bundle args = new Bundle();
		args.putStringArrayList(EXTRA_POI_UUIDS, uuids);
		f.uuids = uuids;
		args.putStringArrayList(EXTRA_POI_AUTHORITIES, authorities);
		f.authorities = authorities;
		f.setArguments(args);
		return f;
	}

	private ArrayList<String> uuids = null;
	private ArrayList<String> authorities = null;
	private POIArrayAdapter adapter = null;
	private boolean modulesUpdated = false;
	private Location userLocation = null;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setOnClickHandledListener(this);
		this.adapter.setTag(getLogTag());
	}

	@Override
	public void onLeaving() {
		dismiss();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
		DataSourceProvider.addModulesUpdateListener(this);
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
		dismiss();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_dialog_pick_poi, container, false);
		setupView(view);
		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
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
		this.adapter.setManualScrollView((ScrollView) view.findViewById(R.id.scrollview));
		this.adapter.setManualLayout((ViewGroup) view.findViewById(R.id.list));
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
	public void onSaveInstanceState(Bundle outState) {
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
				view.post(new Runnable() {
					@Override
					public void run() {
						if (PickPOIDialogFragment.this.modulesUpdated) {
							onModulesUpdated();
						}
					}
				});
			}
		}
		switchView(view);
		if (this.adapter != null && this.adapter.isInitialized()) {
			this.adapter.onResume(getActivity(), this.userLocation);
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
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			return new POIsLoader(getContext(), this.uuids, this.authorities);
		default:
			CrashUtils.w(this, "Loader id '%s' unknown!", id);
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
	public void onLoadFinished(@NonNull Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		this.adapter.initManual();
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
		DataSourceProvider.removeModulesUpdateListener(this);
	}
}
