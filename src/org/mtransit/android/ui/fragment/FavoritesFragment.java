package org.mtransit.android.ui.fragment;

import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager.FavoriteUpdateListener;
import org.mtransit.android.task.FavoritesLoader;
import org.mtransit.android.ui.MTActivityWithLocation;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.TextView;

public class FavoritesFragment extends ABFragment implements LoaderManager.LoaderCallbacks<List<POIManager>>, MTActivityWithLocation.UserLocationListener,
		FavoriteUpdateListener {

	private static final String TAG = FavoritesFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static FavoritesFragment newInstance() {
		return new FavoritesFragment();
	}

	private POIArrayAdapter adapter;
	private CharSequence emptyText = null;
	private Location userLocation;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_favorites, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		initAdapter();
		this.adapter.onResume();
		getLoaderManager().restartLoader(FAVORITES_LOADER, null, this);
	}

	private static final int FAVORITES_LOADER = 0;

	@Override
	public Loader<List<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case FAVORITES_LOADER:
			final FavoritesLoader favoriteLoader = new FavoritesLoader(getActivity());
			return favoriteLoader;
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<List<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
			this.adapter.onPause();
		}
	}

	@Override
	public void onLoadFinished(Loader<List<POIManager>> loader, List<POIManager> data) {
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		switchView();
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				if (this.adapter != null) {
					this.adapter.setLocation(this.userLocation);
				}
			}
		}
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

	private void initAdapter() {
		if (this.adapter != null) {
			return;
		}
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(getLogTag());
		this.adapter.setShowFavorite(false); // all items in this screen are favorites
		this.adapter.setFavoriteUpdateListener(this);
		this.adapter.setShowTypeHeader(true);
		setupView(getView());
		switchView();
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		inflateList(view);
		this.adapter.setListView((AbsListView) getView().findViewById(R.id.list));
	}

	@Override
	public void onFavoriteUpdated() {
		getLoaderManager().restartLoader(FAVORITES_LOADER, null, this);
		if (this.adapter != null) {
			// TODO useful? (favorite star not displayed
			this.adapter.onFavoriteUpdated();
		}
	}

	private void switchView() {
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading();
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty();
		} else {
			showList();
		}
	}

	private void showList() {
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		inflateList(getView());
		getView().findViewById(R.id.list).setVisibility(View.VISIBLE); // show
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
		}
	}

	private void showLoading() {
		if (getView().findViewById(R.id.list) != null) { // IF inflated/present DO
			getView().findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.loading_stub)).inflate(); // inflate
		}
		getView().findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty() {
		if (getView().findViewById(R.id.list) != null) { // IF inflated/present DO
			getView().findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) getView().findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		getView().findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public int getABIconDrawableResId() {
		return R.drawable.ic_menu_favorites;
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.favorites);
	}

	@Override
	public CharSequence getSubtitle(Context context) {
		return null;
	}

	@Override
	public Integer getBgColor() {
		return ABFragment.NO_BG_COLOR;
	}
}
