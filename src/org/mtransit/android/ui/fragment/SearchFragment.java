package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.KeyboardUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.POISearchLoader;
import org.mtransit.android.ui.MTActivityWithLocation;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

public class SearchFragment extends ABFragment implements LoaderManager.LoaderCallbacks<List<POIManager>>, MTActivityWithLocation.UserLocationListener,
		POIArrayAdapter.TypeHeaderButtonsClickListener, SearchView.OnQueryTextListener {

	private static final String TAG = SearchFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Search";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_QUERY = "extra_query";
	private static final String EXTRA_TYPE_FILTER = "extra_type_filter";

	public static SearchFragment newInstance(String optQuery, DataSourceType optTypeFilter) {
		SearchFragment f = new SearchFragment();
		Bundle args = new Bundle();
		if (!TextUtils.isEmpty(optQuery)) {
			args.putString(EXTRA_QUERY, optQuery);
		}
		if (optTypeFilter != null) {
			args.putInt(EXTRA_TYPE_FILTER, optTypeFilter.getId());
		}
		f.setArguments(args);
		return f;
	}

	private POIArrayAdapter adapter;
	private CharSequence emptyText = null;
	private Location userLocation;
	private String query;
	private TypeFilter typeFilter = TypeFilter.ALL;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_search, container, false);
		setupView(view);
		return view;
	}

	private void initAdapter() {
		if (this.adapter != null) {
			return;
		}
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(getLogTag());
		this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE);
		this.adapter.setOnTypeHeaderButtonsClickListener(this);
		final View view = getView();
		setupView(view);
		switchView(view);
	}

	@Override
	public void onTypeHeaderButtonClick(int buttonId, DataSourceType type) {
		if (buttonId == POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_MORE) {
			setTypeFilter(TypeFilter.fromDataSourceType(type));
			return;
		}
		this.adapter.onTypeHeaderButtonClick(buttonId, type);
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		inflateList(view);
		this.adapter.setListView((AbsListView) view.findViewById(R.id.list));
		Spinner typeFiltersSpinner = (Spinner) view.findViewById(R.id.typeFilters);
		typeFiltersSpinner.setAdapter(getTypeFiltersAdapter());
		final int position = getTypeFiltersAdapter().getPosition(this.typeFilter);
		typeFiltersSpinner.setSelection(position, false);
		typeFiltersSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

			private int lastPosition = 0;

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (this.lastPosition != position) { // stops initial onItemSelection() call when spinner becomes visible for the 1st time
					setTypeFilter(getTypeFiltersAdapter().getItem(position));
					this.lastPosition = position;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		if (this.typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
			typeFiltersSpinner.setVisibility(View.GONE);
		} else {
			typeFiltersSpinner.setVisibility(View.VISIBLE);
		}
	}

	private TypeFiltersAdapter typeFiltersAdapter = null;

	private TypeFiltersAdapter getTypeFiltersAdapter() {
		if (this.typeFiltersAdapter == null) {
			this.typeFiltersAdapter = new TypeFiltersAdapter(getActivity());
		}
		return this.typeFiltersAdapter;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initAdapter();
		restoreInstance(savedInstanceState);
	}

	private void restoreInstance(Bundle savedInstanceState) {
		final Integer typeIdFilter = BundleUtils.getInt(EXTRA_TYPE_FILTER, savedInstanceState, getArguments());
		if (typeIdFilter != null) {
			this.typeFilter = TypeFilter.fromDataSourceType(DataSourceType.parseId(typeIdFilter));
		}
		final String query = BundleUtils.getString(EXTRA_QUERY, savedInstanceState, getArguments());
		if (query != null) {
			setQuery(query, false);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.query != null) {
			outState.putString(EXTRA_QUERY, this.query);
		}
		if (this.typeFilter != null) {
			outState.putInt(EXTRA_TYPE_FILTER, this.typeFilter.getDataSourceTypeId());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.adapter != null) {
			this.adapter.onResume(getActivity());
		}
		if (TextUtils.isEmpty(this.query)) {
			if (getSearchView() != null) {
				getSearchView().requestFocusFromTouch();
			}
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
		KeyboardUtils.hideKeyboard(getActivity(), getView());
	}

	@Override
	public void onModulesUpdated() {
		getTypeFiltersAdapter().reset();
		getLoaderManager().restartLoader(POI_SEARCH_LOADER, null, this);
	}

	private static final int POI_SEARCH_LOADER = 0;

	@Override
	public Loader<List<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POI_SEARCH_LOADER:
			final POISearchLoader poiSearchLoader = new POISearchLoader(getActivity(), this.query, this.typeFilter, this.userLocation);
			return poiSearchLoader;
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
		switchView(getView());
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

	public void setTypeFilter(TypeFilter typeFilter) {
		if (typeFilter != null && this.typeFilter.getDataSourceTypeId() != typeFilter.getDataSourceTypeId()) {
			this.typeFilter = typeFilter;
			Spinner typeFiltersSpinner = (Spinner) getView().findViewById(R.id.typeFilters);
			if (this.typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE);
				typeFiltersSpinner.setVisibility(View.GONE);
			} else {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_NONE);
				typeFiltersSpinner.setVisibility(View.VISIBLE); // trigger onItemSelected() the 1st time
			}
			final int position = getTypeFiltersAdapter().getPosition(this.typeFilter);
			typeFiltersSpinner.setSelection(position, true);
			this.adapter.clear();
			switchView(getView());
			getLoaderManager().restartLoader(POI_SEARCH_LOADER, null, this);
		}
	}

	public void setQuery(String query, boolean alreadyInSearchView) {
		if (!StringUtils.equals(StringUtils.trim(this.query), StringUtils.trim(query))) {
			this.query = query;
			if (TextUtils.isEmpty(this.query)) {
				this.emptyText = getString(R.string.search_hint);
			} else {
				this.emptyText = getString(R.string.search_no_result_for_and_query, this.query);
			}
			if (!alreadyInSearchView && getSearchView() != null) {
				getSearchView().setQuery(this.query, false);
			}
			this.adapter.clear();
			switchView(getView());

			getLoaderManager().destroyLoader(POI_SEARCH_LOADER); // cancel now

			cancelRestartSearchLater();
			this.restartSearchLater = new RestartSearchLater();
			this.handler.postDelayed(this.restartSearchLater, TimeUtils.ONE_SECOND_IN_MS);

		}
	}

	private void cancelRestartSearchLater() {
		if (this.restartSearchLater != null) {
			this.handler.removeCallbacks(this.restartSearchLater);
			this.restartSearchLater = null;
		}
	}

	private Handler handler = new Handler();

	private RestartSearchLater restartSearchLater = null;

	private class RestartSearchLater implements Runnable {
		@Override
		public void run() {
			getLoaderManager().restartLoader(POI_SEARCH_LOADER, null, SearchFragment.this);
			cancelRestartSearchLater();
		}
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		setQuery(newText, true);
		return true; // handled
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		setQuery(query, true);
		KeyboardUtils.hideKeyboard(getActivity(), getView());
		return true; // handled
	}

	private void switchView(View view) {
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
		if (view.findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.loading_stub)).inflate(); // inflate
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
	public int getABIconDrawableResId() {
		return R.mipmap.ic_launcher; // search icon already in custom action bar search view
	}

	@Override
	public boolean isABShowSearchMenuItem() {
		return false;
	}

	@Override
	public View getABCustomView() {
		return getSearchView();
	}

	private SearchView searchView;

	private SearchView getSearchView() {
		if (this.searchView == null) {
			initSearchView();
		}
		return this.searchView;
	}

	private void initSearchView() {
		final Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		this.searchView = new SearchView(activity);
		this.searchView.setQueryHint(getString(R.string.search_hint));
		this.searchView.setFocusable(true);
		this.searchView.setIconifiedByDefault(true);
		this.searchView.setIconified(false);
		this.searchView.setOnQueryTextListener(this);
		this.searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					KeyboardUtils.hideKeyboard(getActivity(), getView());
				}
			}

		});
		this.searchView.setQuery(this.query, false);
		if (TextUtils.isEmpty(this.query)) {
			this.searchView.requestFocusFromTouch();
		}
		this.searchView.setQueryRefinementEnabled(true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelRestartSearchLater();
		this.searchView = null;
		this.handler = null;
	}

	public static class TypeFilter {

		public static final TypeFilter ALL = new TypeFilter(-1, R.string.all, -1);

		private int dataSourceTypeId;

		private int nameResId;

		private int iconResId;

		public TypeFilter(int dataSourceTypeId, int nameResId, int iconResId) {
			this.dataSourceTypeId = dataSourceTypeId;
			this.nameResId = nameResId;
			this.iconResId = iconResId;
		}

		@Override
		public String toString() {
			return new StringBuilder(TypeFilter.class.getSimpleName()).append('[') //
					.append("dataSourceTypeId:").append(this.dataSourceTypeId).append(',') //
					.append("nameResId:").append(this.nameResId).append(',') //
					.append("iconResId:").append(this.iconResId).append(',') //
					.append(']').toString();
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof TypeFilter)) {
				return false;
			}
			final TypeFilter other = (TypeFilter) o;
			if (this.dataSourceTypeId != other.dataSourceTypeId) {
				return false;
			}
			if (this.nameResId != other.nameResId) {
				return false;
			}
			if (this.iconResId != other.iconResId) {
				return false;
			}
			return true;
		}

		public int getNameResId() {
			return nameResId;
		}

		public int getIconResId() {
			return iconResId;
		}

		public int getDataSourceTypeId() {
			return dataSourceTypeId;
		}

		public static TypeFilter fromDataSourceType(DataSourceType dst) {
			return new TypeFilter(dst.getId(), dst.getPoiShortNameResId(), dst.getAbIconResId());
		}

	}

	private static class TypeFiltersAdapter extends MTArrayAdapter<TypeFilter> {

		private static final String TAG = TypeFiltersAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private LayoutInflater layoutInflater;

		public TypeFiltersAdapter(Context context) {
			super(context, -1);
			this.layoutInflater = LayoutInflater.from(context);
			init();
		}

		private void init() {
			List<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
			typeFilters.add(TypeFilter.ALL);
			final List<DataSourceType> availableTypes = DataSourceProvider.get().getAvailableAgencyTypes(getContext());
			if (availableTypes != null) {
				for (DataSourceType dst : availableTypes) {
					if (dst.isSearchable()) {
						typeFilters.add(TypeFilter.fromDataSourceType(dst));
					}
				}
			}
			addAll(typeFilters);
		}

		public void reset() {
			clear();
			init();
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getTheView(position, convertView, parent);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getTheView(position, convertView, parent);
		}

		private View getTheView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_type_item, parent, false);
				TypeViewHolder holder = new TypeViewHolder();
				holder.nameTv = (TextView) convertView.findViewById(R.id.name);
				convertView.setTag(holder);
			}
			TypeViewHolder holder = (TypeViewHolder) convertView.getTag();
			final TypeFilter type = getItem(position);
			holder.nameTv.setText(type.getNameResId());
			if (type.getIconResId() != -1) {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(type.getIconResId(), 0, 0, 0);
			} else {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
			return convertView;
		}

		private static class TypeViewHolder {
			TextView nameTv;
		}
	}
}
