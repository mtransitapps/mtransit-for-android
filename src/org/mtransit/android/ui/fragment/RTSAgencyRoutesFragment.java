package org.mtransit.android.ui.fragment;

import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.RTSAgencyRoutesLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTJPathsView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

public class RTSAgencyRoutesFragment extends MTFragmentV4 implements AgencyTypeFragment.AgencyFragment, LoaderManager.LoaderCallbacks<ArrayList<Route>>,
		AdapterView.OnItemClickListener {

	private static final String TAG = RTSAgencyRoutesFragment.class.getSimpleName();

	private String tag = TAG;

	@Override
	public String getLogTag() {
		return this.tag;
	}

	public void setLogTag(String tag) {
		this.tag = TAG + "-" + tag;
	}


	private static final String EXTRA_AGENCY_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";

	public static RTSAgencyRoutesFragment newInstance(int fragmentPosition, int lastVisibleFragmentPosition, String agencyAuthority) {
		RTSAgencyRoutesFragment f = new RTSAgencyRoutesFragment();
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
		f.setArguments(args);
		return f;
	}

	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private RTSRouteArrayAdapter adapter;
	private String emptyText = null;

	private String authority;

	@Override
	public String getAgencyAuthority() {
		return this.authority;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_rts_agency_routes, container, false);
		setupView(view);
		return view;
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
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AGENCY_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
		}
		Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, bundles);
		if (fragmentPosition != null) {
			if (fragmentPosition >= 0) {
				this.fragmentPosition = fragmentPosition;
			} else {
				this.fragmentPosition = -1;
			}
		}
		Integer newLastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, bundles);
		if (newLastVisibleFragmentPosition != null) {
			if (newLastVisibleFragmentPosition >= 0) {
				this.lastVisibleFragmentPosition = newLastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
		this.adapter.setAuthority(this.authority);
	}
	private void initAdapters(Activity activity) {
		this.adapter = new RTSRouteArrayAdapter(activity, this.authority, isShowingListInsteadOfGrid());
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		AbsListView absListView = (AbsListView) view.findViewById(isShowingListInsteadOfGrid() ? R.id.list : R.id.grid);
		absListView.setAdapter(this.adapter);
		absListView.setOnItemClickListener(this);
	}

	private Boolean showingListInsteadOfGrid = null;

	private boolean isShowingListInsteadOfGrid() {
		if (this.showingListInsteadOfGrid == null) {
			this.showingListInsteadOfGrid = isShowingListInsteadOfGridPref();
			if (!TextUtils.isEmpty(this.authority)) {
				PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(this.authority),
						this.showingListInsteadOfGrid, true);
			}
		}
		return this.showingListInsteadOfGrid;
	}

	private void checkIfShowingListInsteadOfGridChanged() {
		if (this.showingListInsteadOfGrid == null) {
			return;
		}
		boolean newShowingListInsteadOfGrid = isShowingListInsteadOfGridPref();
		if (newShowingListInsteadOfGrid != this.showingListInsteadOfGrid) {
			setShowingListInsteadOfGrid(newShowingListInsteadOfGrid);
		}
	}

	private boolean isShowingListInsteadOfGridPref() {
		boolean showingListInsteadOfGridLastSet = PreferenceUtils.getPrefDefault(getActivity(),
				PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_LAST_SET, PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT);
		if (TextUtils.isEmpty(this.authority)) {
			return showingListInsteadOfGridLastSet;
		}
		return PreferenceUtils.getPrefDefault(getActivity(), PreferenceUtils.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(this.authority),
				showingListInsteadOfGridLastSet);
	}

	private void setShowingListInsteadOfGrid(boolean newShowingListInsteadOfGrid) {
		if (this.showingListInsteadOfGrid != null && this.showingListInsteadOfGrid == newShowingListInsteadOfGrid) {
			return; // nothing changed
		}
		this.showingListInsteadOfGrid = newShowingListInsteadOfGrid; // switching to grid
		PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_LAST_SET, this.showingListInsteadOfGrid,
				false);
		if (!TextUtils.isEmpty(this.authority)) {
			PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(this.authority),
					this.showingListInsteadOfGrid, false);
		}
		if (this.adapter != null) {
			this.adapter.seShowingListInsteadOfGrid(this.showingListInsteadOfGrid);
			this.adapter.notifyDataSetChanged();
			setupView(getView());
			switchView(getView());
		}
		updateListGridToggleMenuItem();
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Route selectedRoute = this.adapter == null ? null : this.adapter.getItem(position);
		if (selectedRoute != null) {
			((MainActivity) getActivity()).addFragmentToStack(RTSRouteFragment.newInstance(this.authority, selectedRoute.id, null, null, selectedRoute));
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
				&& (//
				(this.fragmentPosition == visibleFragmentPosition && this.fragmentVisible) //
				|| //
				(this.fragmentPosition != visibleFragmentPosition && !this.fragmentVisible) //
				) //
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
	}

	@Override
	public boolean isFragmentVisible() {
		return this.fragmentVisible;
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		if (this.adapter == null || !this.adapter.isInitialized()) {
			LoaderUtils.restartLoader(getLoaderManager(), ROUTES_LOADER, null, this);
		} else {
			switchView(getView());
		}
		checkIfShowingListInsteadOfGridChanged();
		getActivity().supportInvalidateOptionsMenu(); // initialize action bar list/grid switch icon
		updateListGridToggleMenuItem();
	}

	private static final int ROUTES_LOADER = 0;

	@Override
	public Loader<ArrayList<Route>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case ROUTES_LOADER:
			if (TextUtils.isEmpty(this.authority) || getActivity() == null) {
				return null;
			}
			return new RTSAgencyRoutesLoader(getActivity(), this.authority);
		default:
			MTLog.w(this, "Loader ID '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<Route>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<Route>> loader, ArrayList<Route> data) {
		this.adapter.setRoutes(data);
		switchView(getView());
	}

	@Override
	public void onPause() {
		super.onPause();
		onFragmentInvisible();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.fragmentPosition < 0 || this.fragmentPosition == this.lastVisibleFragmentPosition) {
			onFragmentVisible();
		} // ELSE would be call later
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
		}
	}

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getCount() == 0) {
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
		view.findViewById(isShowingListInsteadOfGrid() ? R.id.grid : R.id.list).setVisibility(View.GONE); // hide
		view.findViewById(isShowingListInsteadOfGrid() ? R.id.list : R.id.grid).setVisibility(View.VISIBLE); // show
	}


	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.grid) != null) { // IF inflated/present DO
			view.findViewById(R.id.grid).setVisibility(View.GONE); // hide
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
		if (view.findViewById(R.id.grid) != null) { // IF inflated/present DO
			view.findViewById(R.id.grid).setVisibility(View.GONE); // hide
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

	private MenuItem listGridToggleMenuItem;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (menu.findItem(R.id.menu_toggle_list_grid) == null) {
			inflater.inflate(R.menu.menu_rts_agency_routes, menu);
			this.listGridToggleMenuItem = menu.findItem(R.id.menu_toggle_list_grid);
			if (!this.fragmentVisible) {
				this.listGridToggleMenuItem.setVisible(false);
			}
		} else {
			this.listGridToggleMenuItem = menu.findItem(R.id.menu_toggle_list_grid);
		}
		updateListGridToggleMenuItem();
	}

	private void updateListGridToggleMenuItem() {
		if (!this.fragmentVisible) {
			return;
		}
		if (this.listGridToggleMenuItem == null) {
			return;
		}
		this.listGridToggleMenuItem.setIcon(isShowingListInsteadOfGrid() ? R.drawable.ic_action_action_grid_holo_dark
				: R.drawable.ic_action_action_list_holo_dark);
		this.listGridToggleMenuItem.setTitle(isShowingListInsteadOfGrid() ? R.string.menu_action_grid : R.string.menu_action_list);
		this.listGridToggleMenuItem.setVisible(true);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!this.fragmentVisible) {
			return false; // not handled
		}
		switch (item.getItemId()) {
		case R.id.menu_toggle_list_grid:
			setShowingListInsteadOfGrid(!isShowingListInsteadOfGrid()); // switching
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	private static class RTSRouteArrayAdapter extends MTArrayAdapter<Route> implements MTLog.Loggable {

		private static final String TAG = RTSRouteArrayAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private ArrayList<Route> routes = null;
		private LayoutInflater layoutInflater;
		private String authority;
		private boolean showingListInsteadOfGrid;

		public RTSRouteArrayAdapter(Context context, String authority, boolean showingListInsteadOfGrid) {
			super(context, -1);
			this.layoutInflater = LayoutInflater.from(context);
			this.authority = authority;
			this.showingListInsteadOfGrid = showingListInsteadOfGrid;
		}

		public void seShowingListInsteadOfGrid(boolean showingListInsteadOfGrid) {
			this.showingListInsteadOfGrid = showingListInsteadOfGrid;
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}

		public void setRoutes(ArrayList<Route> routes) {
			this.routes = routes;
		}

		public boolean isInitialized() {
			return this.routes != null;
		}

		@Override
		public int getCount() {
			return this.routes == null ? 0 : this.routes.size();
		}

		@Override
		public int getItemViewType(int position) {
			return this.showingListInsteadOfGrid ? 0 : 1;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public Route getItem(int position) {
			return this.routes == null ? null : this.routes.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getRouteView(position, convertView, parent);
		}

		private View getRouteView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.showingListInsteadOfGrid ? R.layout.layout_rts_route_list_item
						: R.layout.layout_rts_route_grid_item, parent, false);
				RouteViewHolder holder = new RouteViewHolder();
				holder.routeFL = convertView.findViewById(R.id.route);
				holder.routeShortNameTv = (TextView) convertView.findViewById(R.id.route_short_name);
				holder.routeTypeImg = (MTJPathsView) convertView.findViewById(R.id.route_type_img);
				holder.routeLongNameTv = (TextView) convertView.findViewById(R.id.route_long_name);
				convertView.setTag(holder);
			}
			updateRouteView(position, convertView);
			return convertView;
		}

		private View updateRouteView(int position, View convertView) {
			Route route = getItem(position);
			if (convertView == null) {
				return null;
			}
			RouteViewHolder holder = (RouteViewHolder) convertView.getTag();
			if (route == null) {
				holder.routeFL.setVisibility(View.GONE);
			} else {
				int routeTextColor = Color.WHITE;
				if (TextUtils.isEmpty(route.shortName)) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					JPaths rtsRouteLogo = DataSourceProvider.get(getContext()).getRTSAgencyRouteLogo(getContext(), this.authority);
					if (rtsRouteLogo != null) {
						holder.routeTypeImg.setJSON(rtsRouteLogo);
						holder.routeTypeImg.setColor(routeTextColor);
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						holder.routeTypeImg.setVisibility(View.GONE);
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					SpannableStringBuilder ssb = new SpannableStringBuilder(route.shortName);
					if (ssb.length() > 3) {
						SpanUtils.set(ssb, SpanUtils.FIFTY_PERCENT_SIZE_SPAN);
						holder.routeShortNameTv.setSingleLine(false);
						holder.routeShortNameTv.setMaxLines(2);
					} else {
						holder.routeShortNameTv.setSingleLine(true);
						holder.routeShortNameTv.setMaxLines(1);
					}
					holder.routeShortNameTv.setText(ssb);
					holder.routeShortNameTv.setTextColor(routeTextColor);
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				if (holder.routeLongNameTv != null) {
					holder.routeLongNameTv.setTextColor(routeTextColor);
					if (TextUtils.isEmpty(route.longName)) {
						holder.routeLongNameTv.setVisibility(View.GONE);
					} else {
						holder.routeLongNameTv.setText(route.longName);
						holder.routeLongNameTv.setVisibility(View.VISIBLE);
					}
				}
				holder.routeFL.setBackgroundColor(POIManager.getRouteColor(getContext(), route, this.authority, Color.BLACK));
				holder.routeFL.setVisibility(View.VISIBLE);
			}
			return convertView;
		}

		public void onDestroy() {
			if (this.routes != null) {
				this.routes.clear();
				this.routes = null;
			}
		}

		public static class RouteViewHolder {
			TextView routeShortNameTv;
			View routeFL;
			MTJPathsView routeTypeImg;
			TextView routeLongNameTv;
		}

	}
}
