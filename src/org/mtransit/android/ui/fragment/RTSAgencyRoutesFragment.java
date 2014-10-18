package org.mtransit.android.ui.fragment;

import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.task.RTSAgencyRoutesLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTJPathsView;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RTSAgencyRoutesFragment extends MTFragmentV4 implements VisibilityAwareFragment, LoaderManager.LoaderCallbacks<List<Route>>,
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

	public static RTSAgencyRoutesFragment newInstance(int fragmentPosition, int lastVisisbleFragmentPosition, AgencyProperties agency) {
		RTSAgencyRoutesFragment f = new RTSAgencyRoutesFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AGENCY_AUTHORITY, agency.getAuthority());
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
		}
		if (lastVisisbleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisisbleFragmentPosition);
		}
		f.setArguments(args);
		return f;
	}

	private AgencyProperties agency;
	private int fragmentPosition = -1;
	private int lastVisisbleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private RTSRouteArrayAdapter adapter;
	private String emptyText = null;

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
			this.agency = DataSourceProvider.get().getAgency(getActivity(), agencyAuthority);
			setLogTag(this.agency.getShortName());
		}
		final Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (fragmentPosition != null) {
			if (fragmentPosition.intValue() >= 0) {
				this.fragmentPosition = fragmentPosition.intValue();
			} else {
				this.fragmentPosition = -1;
			}
		}
		final Integer lastVisisbleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (lastVisisbleFragmentPosition != null) {
			if (lastVisisbleFragmentPosition.intValue() >= 0) {
				this.lastVisisbleFragmentPosition = lastVisisbleFragmentPosition;
			} else {
				this.lastVisisbleFragmentPosition = -1;
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		if (this.adapter == null) {
			initAdapter();
		}
		switchView();
	}

	private void initAdapter() {
		this.adapter = new RTSRouteArrayAdapter(getActivity(), this.agency.getAuthority());
		setupView(getView());
		switchView();
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		inflateList(view);
		final AbsListView absListView = (AbsListView) view.findViewById(R.id.list);
		absListView.setAdapter(this.adapter);
		absListView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Route route = this.adapter.getItem(position);
		if (route != null) {
			((MainActivity) getActivity()).addFragmentToStack(RTSRouteFragment.newInstance(this.agency.getAuthority(), route));
		}
	}

	@Override
	public void setFragmentVisisbleAtPosition(int visisbleFragmentPosition) {
		if (this.lastVisisbleFragmentPosition == visisbleFragmentPosition //
				&& (//
				(this.fragmentPosition == visisbleFragmentPosition && this.fragmentVisible) //
				|| //
				(this.fragmentPosition != visisbleFragmentPosition && !this.fragmentVisible) //
				) //
		) {
			return;
		}
		this.lastVisisbleFragmentPosition = visisbleFragmentPosition;
		if (this.fragmentPosition < 0) {
			return;
		}
		if (this.fragmentPosition == visisbleFragmentPosition) {
			onFragmentVisisble();
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

	private void onFragmentVisisble() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		if (this.adapter == null || this.adapter.isEmpty()) {
			getActivity().getSupportLoaderManager().restartLoader(ROUTES_LOADER, null, this);
		} else {
			switchView();
		}
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
		switchView();
	}

	@Override
	public void onPause() {
		super.onPause();
		onFragmentInvisible();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.fragmentPosition < 0 || this.fragmentPosition == this.lastVisisbleFragmentPosition) {
			onFragmentVisisble();
		} // ELSE would be call later
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
		}
	}

	private void switchView() {
		MTLog.v(this, "switchView()");
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading();
		} else if (this.adapter.getCount() == 0) {
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

	private static class RTSRouteArrayAdapter extends ArrayAdapter<Route> implements MTLog.Loggable {

		private static final String TAG = RTSRouteArrayAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private List<Route> routes = null;
		private LayoutInflater layoutInflater;
		private String authority;

		public RTSRouteArrayAdapter(Context context, String authority) {
			super(context, R.layout.layout_rts_route_item, R.id.route_short_name);
			this.layoutInflater = LayoutInflater.from(context);
			this.authority = authority;
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
		public Route getItem(int position) {
			return this.routes == null ? null : this.routes.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getRouteView(position, convertView, parent);
		}

		private View getRouteView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_rts_route_item, parent, false);
				RouteViewHolder holder = new RouteViewHolder();
				holder.routeFL = convertView.findViewById(R.id.route);
				holder.routeShortNameTv = (TextView) convertView.findViewById(R.id.route_short_name);
				holder.routeTypeImg = (MTJPathsView) convertView.findViewById(R.id.route_type_img);
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
					final JPaths rtsRouteLogo = DataSourceProvider.get().getRTSRouteLogo(getContext(), this.authority);
					if (rtsRouteLogo != null) {
						holder.routeTypeImg.setJSON(rtsRouteLogo);
						holder.routeTypeImg.setColor(routeTextColor);
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						holder.routeTypeImg.setVisibility(View.GONE);
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					holder.routeShortNameTv.setText(route.shortName.trim());
					holder.routeShortNameTv.setTextColor(routeTextColor);
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
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
		}

	}
}
