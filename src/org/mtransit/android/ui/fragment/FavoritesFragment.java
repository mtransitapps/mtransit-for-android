package org.mtransit.android.ui.fragment;

import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.FavoritesLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.TextView;

public class FavoritesFragment extends ABFragment implements LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, MTActivityWithLocation.UserLocationListener,
		FavoriteManager.FavoriteUpdateListener {

	private static final String TAG = FavoritesFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Favorites";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	public static FavoritesFragment newInstance() {
		return new FavoritesFragment();
	}

	private POIArrayAdapter adapter;
	private CharSequence emptyText = null;
	private Location userLocation;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_favorites, container, false);
		setupView(view);
		return view;
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
						if (FavoritesFragment.this.modulesUpdated) {
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
			LoaderUtils.restartLoader(this, FAVORITES_LOADER, null, this);
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
	}

	private static final int FAVORITES_LOADER = 0;

	@NonNull
	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case FAVORITES_LOADER:
			return new FavoritesLoader(requireContext());
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
		this.emptyText = getString(R.string.no_favorites);
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_favorites, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_favorite_folder:
			FavoriteManager.showAddFolderDialog(getContext(), this, null, null);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
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

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setTag(getLogTag());
		this.adapter.setShowFavorite(false); // all items in this screen are favorites
		this.adapter.setFavoriteUpdateListener(this);
		this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_ALL_NEARBY);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		inflateList(view);
		switchView(view);
		linkAdapterWithListView(view);
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

	@Override
	public void onFavoriteUpdated() {
		LoaderUtils.restartLoader(this, FAVORITES_LOADER, null, this);
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		LoaderUtils.restartLoader(this, FAVORITES_LOADER, null, this);
		this.modulesUpdated = false; // processed
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
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.favorites);
	}
}
