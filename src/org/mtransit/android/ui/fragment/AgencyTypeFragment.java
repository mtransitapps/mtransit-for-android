package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.SlidingTabLayout;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

public class AgencyTypeFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener {

	private static final String TAG = AgencyTypeFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.type;
	}

	private static final String EXTRA_TYPE_ID = "extra_type_id";

	public static AgencyTypeFragment newInstance(DataSourceType type) {
		AgencyTypeFragment f = new AgencyTypeFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE_ID, type.getId());
		f.setArguments(args);
		return f;
	}

	private DataSourceType type;
	private Location userLocation;
	private AgencyPagerAdapter adapter;
	private int lastPageSelected = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_agency_type, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		if (this.adapter == null) {
			initTabsAndViewPager();
		}
		switchView(getView());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.adapter = null;
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		viewPager.setOffscreenPageLimit(3);
		final SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
		tabs.setOnPageChangeListener(this);
		tabs.setSelectedIndicatorColors(0xff666666);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		Integer typeId = BundleUtils.getInt(EXTRA_TYPE_ID, savedInstanceState, getArguments());
		if (typeId != null) {
			this.type = DataSourceType.parseId(typeId);
			((MainActivity) getActivity()).notifyABChange();
		}
	}

	private void initTabsAndViewPager() {
		final List<AgencyProperties> availableAgencies = this.type == null ? null : DataSourceProvider.get().getTypeDataSources(getActivity(), this.type);
		if (availableAgencies == null) {
			return;
		}
		this.adapter = new AgencyPagerAdapter(this, availableAgencies);
		final View view = getView();
		setupView(view);
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		this.lastPageSelected = 0;
		new MTAsyncTask<Void, Void, Integer>() {

			private final String TAG = AgencyTypeFragment.class.getSimpleName() + ">LoadLastPageSelectedFromUserPreferences";

			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Integer doInBackgroundMT(Void... params) {
				try {
					final String agencyAuthority = PreferenceUtils.getPrefLcl(getActivity(),
							PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(AgencyTypeFragment.this.type.getId()),
							PreferenceUtils.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT);
					for (int i = 0; i < availableAgencies.size(); i++) {
						if (availableAgencies.get(i).getAuthority().equals(agencyAuthority)) {
							return i;
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while determining the select agency tab!");
				}
				return null;
			}

			@Override
			protected void onPostExecute(Integer lastPageSelected) {
				if (AgencyTypeFragment.this.lastPageSelected != 0) {
					return; // user has manually move to another page before, too late
				}
				if (lastPageSelected != null) {
					AgencyTypeFragment.this.lastPageSelected = lastPageSelected.intValue();
					viewPager.setCurrentItem(AgencyTypeFragment.this.lastPageSelected);
				}
				switchView(view);
				onPageSelected(AgencyTypeFragment.this.lastPageSelected); // tell current page it's selected

			}
		}.execute();
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			this.userLocation = newLocation;
			final List<Fragment> fragments = getChildFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment != null && fragment instanceof MTActivityWithLocation.UserLocationListener) {
						((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(this.userLocation);
					}
				}
			}
			if (this.adapter != null) {
				this.adapter.setUserLocation(newLocation);
			}
		}
	}

	private void switchView(View view) {
		if (this.adapter == null) {
			showLoading(view);
		} else if (this.adapter.getCount() > 0) {
			showTabsAndViewPager(view);
		} else {
			showEmpty(view);
		}
	}

	private void showTabsAndViewPager(View view) {
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		view.findViewById(R.id.tabs).setVisibility(View.VISIBLE); // show
		view.findViewById(R.id.viewpager).setVisibility(View.VISIBLE); // show
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.tabs) != null) { // IF inflated/present DO
			view.findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			view.findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
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
		if (view.findViewById(R.id.tabs) != null) { // IF inflated/present DO
			view.findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			view.findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
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
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
		if (this.adapter != null) {
			PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(this.type.getId()), this.adapter.getAgency(position)
					.getAuthority(), false);
		}
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisisbleAtPosition(position);
				}
			}
		}
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.adapter.setLastVisisbleFragmentPosition(this.lastPageSelected);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			List<Fragment> fragments = getChildFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment instanceof VisibilityAwareFragment) {
						final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						visibilityAwareFragment.setFragmentVisisbleAtPosition(this.lastPageSelected); // resume
					}
				}
			}
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			List<Fragment> fragments2 = getChildFragmentManager().getFragments();
			if (fragments2 != null) {
				for (Fragment fragment : fragments2) {
					if (fragment instanceof VisibilityAwareFragment) {
						final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						visibilityAwareFragment.setFragmentVisisbleAtPosition(-1); // pause
					}
				}
			}
			break;
		}
	}

	@Override
	public CharSequence getABTitle(Context context) {
		if (this.type == null) {
			return context.getString(R.string.ellipsis);
		}
		return context.getString(this.type.getShortNameResId()).toUpperCase(Locale.ENGLISH);
	}


	private static class AgencyPagerAdapter extends FragmentStatePagerAdapter {

		private List<AgencyProperties> agencies;
		private WeakReference<Context> contextWR;
		private Location userLocation;
		private int lastVisisbleFragmentPosition = -1;

		public AgencyPagerAdapter(AgencyTypeFragment agencyTypeFragment, List<AgencyProperties> agencies) {
			super(agencyTypeFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(agencyTypeFragment.getActivity());
			this.agencies = agencies;
		}

		public AgencyProperties getAgency(int position) {
			return this.agencies.size() == 0 ? null : this.agencies.get(position);
		}

		public void setUserLocation(Location userLocation) {
			this.userLocation = userLocation;
		}

		public void setLastVisisbleFragmentPosition(int lastVisisbleFragmentPosition) {
			this.lastVisisbleFragmentPosition = lastVisisbleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.agencies == null ? 0 : this.agencies.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			final Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.agencies == null || position >= this.agencies.size()) {
				return StringUtils.EMPTY;
			}
			return this.agencies.get(position).getShortName();
		}

		@Override
		public Fragment getItem(int position) {
			final AgencyProperties agency = getAgency(position);
			if (agency.isRTS()) {
				final RTSAgencyRoutesFragment f = RTSAgencyRoutesFragment.newInstance(position, this.lastVisisbleFragmentPosition, agency);
				f.setLogTag(agency.getShortName());
				return f;
			}
			return AgencyPOIsFragment.newInstance(position, this.lastVisisbleFragmentPosition, agency, this.userLocation);
		}
	}

}
