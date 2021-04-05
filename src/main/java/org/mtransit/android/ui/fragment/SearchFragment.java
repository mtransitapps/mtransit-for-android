package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.KeyboardUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.POISearchLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTSearchView;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SearchFragment extends ABFragment implements LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, MTActivityWithLocation.UserLocationListener,
		POIArrayAdapter.TypeHeaderButtonsClickListener, AdapterView.OnItemSelectedListener {

	private static final String TAG = SearchFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Search";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_QUERY = "extra_query";
	private static final String EXTRA_TYPE_FILTER = "extra_type_filter";
	private static final String EXTRA_SEARCH_HAS_FOCUS = "extra_search_has_focus";

	@NonNull
	public static SearchFragment newInstance(@Nullable String optQuery, @Nullable Integer optTypeIdFilter, @Nullable TypeFilter optTypeFilter) {
		SearchFragment f = new SearchFragment();
		Bundle args = new Bundle();
		if (!TextUtils.isEmpty(optQuery)) {
			args.putString(EXTRA_QUERY, optQuery);
			f.query = optQuery;
		}
		if (optTypeIdFilter != null) {
			args.putInt(EXTRA_TYPE_FILTER, optTypeIdFilter);
			f.typeIdFilter = optTypeIdFilter;
		}
		f.typeFilter = optTypeFilter;
		f.setArguments(args);
		return f;
	}

	@Nullable
	private POIArrayAdapter adapter;
	@Nullable
	private CharSequence emptyText = null;
	@Nullable
	private Location userLocation;
	@Nullable
	private String query = null;
	@Nullable
	private Integer typeIdFilter = null;
	@Nullable
	private TypeFilter typeFilter = null;
	private boolean searchHasFocus = true;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public SearchFragment() {
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	private void resetTypeFilter() {
		this.typeFilter = null;
	}

	@SuppressWarnings("UnusedReturnValue")
	private boolean initTypeFilterSync() {
		if (this.typeFilter != null) {
			return false;
		}
		if (this.typeIdFilter != null) {
			if (this.typeIdFilter.equals(TypeFilter.ALL.getDataSourceTypeId())) {
				this.typeFilter = TypeFilter.ALL;
			} else {
				final DataSourceType parseId = DataSourceType.parseId(this.typeIdFilter);
				if (parseId != null) {
					this.typeFilter = TypeFilter.fromDataSourceType(parseId);
				}
			}
		}
		return this.typeFilter != null;
	}

	private void setTypeFilterFromType(@Nullable Integer newTypeId) {
		if (newTypeId == null) {
			MTLog.d(this, "setTypeFilterFromType() > SKIP (null)");
			return;
		}
		if (this.typeIdFilter != null && this.typeIdFilter.equals(newTypeId)) {
			MTLog.d(this, "setTypeFilterFromType() > SKIP (same)");
			return;
		}
		this.typeIdFilter = newTypeId;
		resetTypeFilter();
		initTypeFilterSync();
		applyNewTypeFilter();
	}

	private void applyNewTypeFilter() {
		if (this.typeFilter == null) {
			return;
		}
		LoaderUtils.destroyLoader(this, POI_SEARCH_LOADER); // cancel now
		cancelRestartSearchLater();
		if (this.adapter != null) {
			if (this.typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE);
			} else {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_NONE);
			}
		}
		final View view = getView();
		if (this.typeFiltersAdapter != null) {
			final int position = this.typeFiltersAdapter.getPosition(this.typeFilter);
			if (view != null) {
				((Spinner) view.findViewById(R.id.typeFilters)).setSelection(position, true);
			}
		}
		if (this.adapter != null) {
			this.adapter.clear();
		}
		switchView(view);
		restartSearchLater();
	}

	@Nullable
	private TypeFilter getTypeFilterOrNull() {
		if (this.typeFilter == null) {
			if (this.typeIdFilter == null) {
				this.typeIdFilter = TypeFilter.ALL.getDataSourceTypeId(); // default
			}
			initTypeFilterSync();
			applyNewTypeFilter();
		}
		return this.typeFilter;
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
		this.dataSourcesRepository.readingAllDataSourceTypesDistinct().observe(this, newDataSourceTypes -> {
			if (this.typeFiltersAdapter == null) {
				return; // SKIP (should never happen)
			}
			this.typeFiltersAdapter.setData(
					filterSearchable(newDataSourceTypes)
			);
			if (this.typeFilter != null) {
				final int position = this.typeFiltersAdapter.getPosition(this.typeFilter);
				if (position < 0) {
					setTypeFilterFromType(TypeFilter.ALL.getDataSourceTypeId());
				} else {
					final View view = getView();
					if (view != null) {
						((Spinner) view.findViewById(R.id.typeFilters)).setSelection(position, false);
					}
				}
			}
		});
		this.dataSourcesRepository.readingAllAgenciesDistinct().observe(this, newAllAgencies -> {
			cancelRestartSearchLater();
			restartSearchLater();
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_search, container, false);
		setupView(view);
		return view;
	}

	private void initAdapters(@NonNull IActivity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setTag(getLogTag());
		this.adapter.setOnTypeHeaderButtonsClickListener(this);
		this.typeFiltersAdapter = new TypeFiltersAdapter(activity.requireContext());
	}

	@Override
	public boolean onTypeHeaderButtonClick(int buttonId, @NonNull DataSourceType type) {
		if (buttonId == POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_MORE) {
			KeyboardUtils.hideKeyboard(getActivity(), getView());
			setTypeFilterFromType(type.getId());
			return true; // handled
		}
		return false; // not handled
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		Spinner typeFiltersSpinner = view.findViewById(R.id.typeFilters);
		TypeFilter typeFilter = getTypeFilterOrNull();
		if (this.typeFiltersAdapter != null) {
			final int position = this.typeFiltersAdapter.getPosition(typeFilter);
			typeFiltersSpinner.setSelection(position);
		}
		if (typeFiltersSpinner.getOnItemSelectedListener() == null) {
			typeFiltersSpinner.setOnItemSelectedListener(this);
		}
		setupAdapters(view);
	}

	private void setupAdapters(View view) {
		if (view == null) {
			return;
		}
		inflateList(view);
		switchView(view);
		linkAdapterWithListView(view);
		Spinner typeFiltersSpinner = view.findViewById(R.id.typeFilters);
		typeFiltersSpinner.setAdapter(this.typeFiltersAdapter);
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
	public void onItemSelected(@Nullable AdapterView<?> parent, @Nullable View view, int position, long id) {
		if (this.typeFiltersAdapter != null) {
			final TypeFilter item = this.typeFiltersAdapter.getItem(position);
			if (item != null) {
				setTypeFilterFromType(item.getDataSourceTypeId());
			}
		}
	}

	@Override
	public void onNothingSelected(@Nullable AdapterView<?> parent) {
		// DO NOTHING
	}

	@Nullable
	private TypeFiltersAdapter typeFiltersAdapter = null;

	private void restoreInstanceState(Bundle... bundles) {
		Integer newTypeIdFilter = BundleUtils.getInt(EXTRA_TYPE_FILTER, bundles);
		if (newTypeIdFilter != null && !newTypeIdFilter.equals(this.typeIdFilter)) {
			this.typeIdFilter = newTypeIdFilter;
			resetTypeFilter();
		}
		String newQuery = BundleUtils.getString(EXTRA_QUERY, bundles);
		if (newQuery != null && !newQuery.equals(this.query)) {
			this.query = newQuery;
		}
		Boolean newSearchHasFocus = BundleUtils.getBoolean(EXTRA_SEARCH_HAS_FOCUS, bundles);
		if (newSearchHasFocus != null) {
			this.searchHasFocus = newSearchHasFocus;
		}
		if (this.typeIdFilter == null) {
			this.typeFilter = TypeFilter.ALL;
			this.typeIdFilter = this.typeFilter.getDataSourceTypeId();
		}
		if (this.adapter != null) {
			if (this.typeIdFilter == TypeFilter.ALL.getDataSourceTypeId()) {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE);
			} else {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_NONE);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (this.query != null) {
			outState.putString(EXTRA_QUERY, this.query);
		}
		if (this.typeIdFilter != null) {
			outState.putInt(EXTRA_TYPE_FILTER, this.typeIdFilter);
		}
		refreshSearchHasFocus();
		outState.putBoolean(EXTRA_SEARCH_HAS_FOCUS, this.searchHasFocus);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		switchView(getView());
		if (this.adapter != null) {
			this.adapter.onResume(this, this.userLocation);
			if (!this.adapter.isInitialized()) {
				applyNewQuery();
			}
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
	}

	@NonNull
	private List<TypeFilter> filterSearchable(@Nullable List<DataSourceType> availableTypes) {
		ArrayList<TypeFilter> typeFilters = new ArrayList<>();
		typeFilters.add(TypeFilter.ALL);
		if (availableTypes != null) {
			for (DataSourceType dst : availableTypes) {
				if (dst.isSearchable()) {
					final TypeFilter typeFilter = TypeFilter.fromDataSourceType(dst);
					typeFilters.add(typeFilter);
				}
			}
		}
		return typeFilters;
	}

	private static final int POI_SEARCH_LOADER = 0;

	@NonNull
	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case POI_SEARCH_LOADER:
			TypeFilter typeFilter = getTypeFilterOrNull();
			return new POISearchLoader(requireContext(), this.dataSourcesRepository, this.query, typeFilter, this.userLocation);
		default:
			//noinspection deprecation // FXIME
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

	private void applyNewQuery() {
		LoaderUtils.destroyLoader(this, POI_SEARCH_LOADER); // cancel now
		cancelRestartSearchLater();
		if (TextUtils.isEmpty(this.query)) {
			this.emptyText = getString(R.string.search_hint);
			if (this.adapter != null) {
				this.adapter.setPois(new ArrayList<>()); // empty search = no result
			}
			switchView(getView());
			return;
		}
		this.emptyText = getString(R.string.search_no_result_for_and_query, this.query);
		if (this.adapter != null) {
			this.adapter.clear();
		}
		switchView(getView());
		restartSearchLater();
	}

	private static final String DEV_QUERY = "MTDEV";

	@Nullable
	private Boolean devEnabled = null;

	public void setSearchQuery(@Nullable String query, @SuppressWarnings("unused") boolean alreadyInSearchView) {
		if (DEV_QUERY.equals(query)) {
			this.devEnabled = !Boolean.TRUE.equals(this.devEnabled); // flip
			PreferenceUtils.savePrefLcl(getContext(), PreferenceUtils.PREFS_LCL_DEV_MODE_ENABLED, this.devEnabled, false); // ASYNC
			ToastUtils.makeTextAndShowCentered(getContext(), "DEV MODE: " + this.devEnabled);
			return;
		}
		if (this.query == null || !StringUtils.equals(StringUtils.trim(this.query), StringUtils.trim(query))) {
			this.query = query == null ? StringUtils.EMPTY : query;
			applyNewQuery();
		}
	}

	@NonNull
	private final Handler handler = new Handler();

	@Nullable
	private RestartSearchLater restartSearchLater = null;

	private class RestartSearchLater implements Runnable {
		@Override
		public void run() {
			if (getTypeFilterOrNull() != null) {
				LoaderUtils.restartLoader(SearchFragment.this, POI_SEARCH_LOADER, null, SearchFragment.this);
				cancelRestartSearchLater();
			}
		}
	}

	private void restartSearchLater() {
		this.restartSearchLater = new RestartSearchLater();
		this.handler.postDelayed(this.restartSearchLater, TimeUnit.SECONDS.toMillis(1L));
	}

	private void cancelRestartSearchLater() {
		if (this.restartSearchLater != null) {
			this.handler.removeCallbacks(this.restartSearchLater);
			this.restartSearchLater = null;
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
		TypeFilter typeFilter = getTypeFilterOrNull();
		if (typeFilter == null || typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
			if (view.findViewById(R.id.typeFilters).getVisibility() != View.GONE) {
				view.findViewById(R.id.typeFilters).setVisibility(View.GONE);
			}
		} else {
			if (view.findViewById(R.id.typeFilters).getVisibility() != View.VISIBLE) {
				view.findViewById(R.id.typeFilters).setVisibility(View.VISIBLE);
			}
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
	public boolean isABReady() {
		return this.searchView != null;
	}

	@Override
	public boolean isABShowSearchMenuItem() {
		return false;
	}

	@Override
	public boolean isABCustomViewFocusable() {
		return true;
	}

	@Override
	public boolean isABCustomViewRequestFocus() {
		return searchHasFocus();
	}

	private boolean searchHasFocus() {
		refreshSearchHasFocus();
		return this.searchHasFocus;
	}

	private void refreshSearchHasFocus() {
		if (this.searchView != null) {
			this.searchHasFocus = this.searchView.hasFocus();
		}
	}

	@Nullable
	@Override
	public View getABCustomView() {
		return getSearchView();
	}

	@Nullable
	private MTSearchView searchView;

	@Nullable
	private MTSearchView getSearchView() {
		if (this.searchView == null) {
			initSearchView();
		}
		return this.searchView;
	}

	private void initSearchView() {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		MainActivity mainActivity = (MainActivity) activity;
		ActionBar supportActionBar = mainActivity.getSupportActionBar();
		Context context = supportActionBar == null ? mainActivity : supportActionBar.getThemedContext();
		this.searchView = new MTSearchView(mainActivity, context);
		this.searchView.setQuery(this.query, false);
		if (!this.searchHasFocus) {
			this.searchView.clearFocus();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelRestartSearchLater();
		this.searchView = null;
	}

	public static class TypeFilter implements MTLog.Loggable {

		private static final String TAG = SearchFragment.class.getSimpleName() + ">" + TypeFilter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		public static final TypeFilter ALL = new TypeFilter(-1, R.string.all, -1);

		private final int dataSourceTypeId;

		private final int nameResId;

		private final int iconResId;

		TypeFilter(int dataSourceTypeId, int nameResId, int iconResId) {
			this.dataSourceTypeId = dataSourceTypeId;
			this.nameResId = nameResId;
			this.iconResId = iconResId;
		}

		@NonNull
		@Override
		public String toString() {
			return TypeFilter.class.getSimpleName() + '[' + //
					"dataSourceTypeId:" + this.dataSourceTypeId + ',' + //
					"nameResId:" + this.nameResId + ',' + //
					"iconResId:" + this.iconResId + ',' + //
					']';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TypeFilter that = (TypeFilter) o;
			return dataSourceTypeId == that.dataSourceTypeId //
					&& nameResId == that.nameResId //
					&& iconResId == that.iconResId;
		}

		@Override
		public int hashCode() {
			int result = 0;
			result = 31 * result + dataSourceTypeId;
			result = 31 * result + nameResId;
			result = 31 * result + iconResId;
			return result;
		}

		int getNameResId() {
			return nameResId;
		}

		int getIconResId() {
			return iconResId;
		}

		public int getDataSourceTypeId() {
			return dataSourceTypeId;
		}

		@Nullable
		static TypeFilter fromDataSourceType(@Nullable DataSourceType dst) {
			if (dst == null) {
				return null;
			}
			return new TypeFilter(dst.getId(), dst.getPoiShortNameResId(), dst.getIconResId());
		}
	}

	private static class TypeFiltersAdapter extends MTArrayAdapter<TypeFilter> {

		private static final String TAG = SearchFragment.class.getSimpleName() + ">" + TypeFiltersAdapter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		@NonNull
		private final LayoutInflater layoutInflater;

		TypeFiltersAdapter(@NonNull Context context) {
			super(context, -1);
			this.layoutInflater = LayoutInflater.from(context);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			return getTheView(position, convertView, parent);
		}

		@NonNull
		@Override
		public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			return getTheView(position, convertView, parent);
		}

		@NonNull
		private View getTheView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_type_item, parent, false);
				TypeViewHolder holder = new TypeViewHolder();
				holder.nameTv = convertView.findViewById(R.id.name);
				convertView.setTag(holder);
			}
			TypeViewHolder holder = (TypeViewHolder) convertView.getTag();
			TypeFilter type = getItem(position);
			if (type != null) {
				holder.nameTv.setText(type.getNameResId());
			} else {
				holder.nameTv.setText(null);
			}
			if (type != null && type.getIconResId() != -1) {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(type.getIconResId(), 0, 0, 0);
			} else {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
			return convertView;
		}

		public void setData(@NonNull List<TypeFilter> typeFilters) {
			clearSilently();
			addAll(typeFilters);
		}

		private void clearSilently() {
			setNotifyOnChange(false);
			clear(); // skip 1st notifyDataSetChanged()
			setNotifyOnChange(true);
		}

		private static class TypeViewHolder {
			TextView nameTv;
		}
	}
}
