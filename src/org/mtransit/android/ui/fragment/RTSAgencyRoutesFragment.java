package org.mtransit.android.ui.fragment;

import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.task.RTSAgencyRoutesLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.AgencyTypeFragment;
import org.mtransit.android.ui.view.MTJPathsView;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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

public class RTSAgencyRoutesFragment extends MTFragmentV4 implements AgencyTypeFragment.AgencyFragment, LoaderManager.LoaderCallbacks<List<Route>>,
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

	public static RTSAgencyRoutesFragment newInstance(int fragmentPosition, int lastVisibleFragmentPosition, AgencyProperties agency) {
		RTSAgencyRoutesFragment f = new RTSAgencyRoutesFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AGENCY_AUTHORITY, agency.getAuthority());
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
		}
		f.setArguments(args);
		return f;
	}

	private AgencyProperties agency;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private RTSRouteArrayAdapter adapter;
	private String emptyText = null;

	@Override
	public AgencyProperties getAgency() {
		return agency;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_rts_agency_routes, container, false);
		setupView(view);
		return view;
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		MTLog.v(this, "restoreInstanceState(%s)", savedInstanceState);
		final String agencyAuthority = BundleUtils.getString(EXTRA_AGENCY_AUTHORITY, savedInstanceState, getArguments());
		if (!TextUtils.isEmpty(agencyAuthority)) {
			this.agency = DataSourceProvider.get(getActivity()).getAgency(agencyAuthority);
			if (this.agency != null) {
				setLogTag(this.agency.getShortName());
			}
		}
		final Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (fragmentPosition != null) {
			if (fragmentPosition.intValue() >= 0) {
				this.fragmentPosition = fragmentPosition.intValue();
			} else {
				this.fragmentPosition = -1;
			}
		}
		final Integer lastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (lastVisibleFragmentPosition != null) {
			if (lastVisibleFragmentPosition.intValue() >= 0) {
				this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		initAdapter();
		switchView(getView());
	}

	private void initAdapter() {
		if (this.adapter != null) {
			return;
		}
		this.adapter = new RTSRouteArrayAdapter(getActivity(), this.agency.getAuthority(), isShowingListInsteadOfGrid());
		final View view = getView();
		setupView(view);
		switchView(view);
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		final AbsListView absListView = (AbsListView) view.findViewById(isShowingListInsteadOfGrid() ? R.id.list : R.id.grid);
		absListView.setAdapter(this.adapter);
		absListView.setOnItemClickListener(this);
	}

	private Boolean showingListInsteadOfGrid = null;

	private boolean isShowingListInsteadOfGrid() {
		if (this.showingListInsteadOfGrid == null) {
			final boolean showingListInsteadOfGridLastSet = PreferenceUtils.getPrefDefault(getActivity(),
					PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_LAST_SET,
					PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT);
			this.showingListInsteadOfGrid = this.agency == null ? showingListInsteadOfGridLastSet : PreferenceUtils.getPrefDefault(getActivity(),
					PreferenceUtils.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(this.agency.getAuthority()), showingListInsteadOfGridLastSet);
		}
		return this.showingListInsteadOfGrid.booleanValue();
	}

	private void checkIfShowingListInsteadOfGridChanged() {
		if (this.showingListInsteadOfGrid == null) {
			return;
		}
		final boolean showingListInsteadOfGridLastSet = PreferenceUtils.getPrefDefault(getActivity(),
				PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_LAST_SET, PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT);
		boolean newShowingListInsteadOfGrid = this.agency == null ? showingListInsteadOfGridLastSet : PreferenceUtils.getPrefDefault(getActivity(),
				PreferenceUtils.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(this.agency.getAuthority()), showingListInsteadOfGridLastSet);
		if (newShowingListInsteadOfGrid != this.showingListInsteadOfGrid.booleanValue()) {
			setShowingListInsteadOfGrid(newShowingListInsteadOfGrid);
		}
	}

	private void setShowingListInsteadOfGrid(boolean newShowingListInsteadOfGrid) {
		if (this.showingListInsteadOfGrid != null && this.showingListInsteadOfGrid.booleanValue() == newShowingListInsteadOfGrid) {
			return; // nothing changed
		}
		this.showingListInsteadOfGrid = newShowingListInsteadOfGrid; // switching to grid
		PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_LAST_SET,
				this.showingListInsteadOfGrid.booleanValue(), false);
		if (this.agency != null) {
			PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(this.agency.getAuthority()),
					this.showingListInsteadOfGrid.booleanValue(), false);
		}
		initAdapter();
		this.adapter.seShowingListInsteadOfGrid(this.showingListInsteadOfGrid);
		setupView(getView());
		switchView(getView());
		this.adapter.notifyDataSetChanged();
		updateListGridToggleMenuItem();
	}

	private void switchListGrid() {
		setShowingListInsteadOfGrid(!isShowingListInsteadOfGrid()); // switching
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Route route = this.adapter.getItem(position);
		if (route != null) {
			((MainActivity) getActivity()).addFragmentToStack(RTSRouteFragment.newInstance(this.agency.getAuthority(), route, null, null));
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

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		if (this.adapter.isEmpty()) {
			getLoaderManager().restartLoader(ROUTES_LOADER, null, this);
		} else {
			switchView(getView());
		}
		checkIfShowingListInsteadOfGridChanged();
		updateListGridToggleMenuItem();
	}

	private static final int ROUTES_LOADER = 0;

	@Override
	public Loader<List<Route>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case ROUTES_LOADER:
			final RTSAgencyRoutesLoader rtsAgencyRoutesLoader = new RTSAgencyRoutesLoader(getActivity(), this.agency);
			return rtsAgencyRoutesLoader;
		default:
			MTLog.w(this, "Loader ID '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Route>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
			this.adapter = null;
		}
	}

	@Override
	public void onLoadFinished(Loader<List<Route>> loader, List<Route> data) {
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
		view.findViewById(isShowingListInsteadOfGrid() ? R.id.grid : R.id.list).setVisibility(View.GONE); // show
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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (menu.findItem(R.id.menu_toggle_list_grid) == null) {
			inflater.inflate(R.menu.rts_agency_routes, menu);
			((MainActivity) getActivity()).addMenuItem(R.id.menu_toggle_list_grid, menu.findItem(R.id.menu_toggle_list_grid));
			if (!this.fragmentVisible) {
				menu.findItem(R.id.menu_toggle_list_grid).setVisible(false);
			}
		}
		updateListGridToggleMenuItem();
	}

	private void updateListGridToggleMenuItem() {
		if (!this.fragmentVisible) {
			return;
		}
		final MenuItem listGridToggleMenuItem = ((MainActivity) getActivity()).getMenuItem(R.id.menu_toggle_list_grid);
		if (listGridToggleMenuItem == null) {
			return;
		}
		listGridToggleMenuItem.setIcon(isShowingListInsteadOfGrid() ? R.drawable.ic_action_grid_holo_light : R.drawable.ic_action_list_holo_light);
		listGridToggleMenuItem.setTitle(isShowingListInsteadOfGrid() ? R.string.menu_action_grid : R.string.menu_action_list);
		listGridToggleMenuItem.setVisible(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!this.fragmentVisible) {
			return false; // not handled
		}
		switch (item.getItemId()) {
		case R.id.menu_toggle_list_grid:
			switchListGrid();
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

		private List<Route> routes = null;
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

		public void setRoutes(List<Route> routes) {
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
				return convertView;
			}
			RouteViewHolder holder = (RouteViewHolder) convertView.getTag();
			if (route == null) {
				holder.routeFL.setVisibility(View.GONE);
			} else {
				final int routeTextColor = ColorUtils.parseColor(route.textColor);
				final int routeColor = ColorUtils.parseColor(route.color);
				if (TextUtils.isEmpty(route.shortName)) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					final JPaths rtsRouteLogo = DataSourceProvider.get(getContext()).getRTSRouteLogo(this.authority);
					if (rtsRouteLogo != null) {
						holder.routeTypeImg.setJSON(rtsRouteLogo);
						holder.routeTypeImg.setColor(routeTextColor);
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						holder.routeTypeImg.setVisibility(View.GONE);
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					holder.routeShortNameTv.setText(route.shortName);
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
				holder.routeFL.setBackgroundColor(routeColor);
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
