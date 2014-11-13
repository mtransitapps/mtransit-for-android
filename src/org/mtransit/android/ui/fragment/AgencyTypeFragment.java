package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
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
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

public class AgencyTypeFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener {

	private static final String TAG = AgencyTypeFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.type;
	}

	private static final String TRACKING_SCREEN_NAME = "Browse";

	@Override
	public String getScreenName() {
		if (this.type != null) {
			return TRACKING_SCREEN_NAME + "/" + this.type.name();
		}
		return TRACKING_SCREEN_NAME;
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true); // child fragments options menus don't get updated when coming back from another activity
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null) {
					fragment.onCreateOptionsMenu(menu, inflater);
				}
			}
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null) {
					if (fragment.onOptionsItemSelected(item)) {
						return true; // handled
					}
				}
			}
		}
		return super.onOptionsItemSelected(item);
	}

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
		final View view = getView();
		initTabsAndViewPager(view);
		switchView(view);
	}

	@Override
	public void onModulesUpdated() {
		if (this.adapter != null) {
			final List<AgencyProperties> newAvailableAgencies = this.type == null ? null : DataSourceProvider.get(getActivity()).getTypeDataSources(
					this.type.getId());
			if (CollectionUtils.getSize(newAvailableAgencies) == CollectionUtils.getSize(this.adapter.getAgencies())) {
				return;
			}
			this.lastPageSelected = -1;
			this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
			pauseAllVisibleAwareChildFragments();
			final View view = getView();
			initTabsAndViewPager(view);
		}
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
		final Integer typeId = BundleUtils.getInt(EXTRA_TYPE_ID, savedInstanceState, getArguments());
		if (typeId != null) {
			this.type = DataSourceType.parseId(typeId);
			((MainActivity) getActivity()).getAbController().setABTitle(this, getABTitle(getActivity()), false);
			((MainActivity) getActivity()).getAbController().setABIcon(this, getABIconDrawableResId(), true);
		}
	}

	private void initTabsAndViewPager(final View view) {
		if (view == null) {
			return;
		}
		final List<AgencyProperties> newAgencies = this.type == null ? null : DataSourceProvider.get(getActivity()).getTypeDataSources(this.type.getId());
		if (CollectionUtils.getSize(newAgencies) == 0) {
			return;
		}
		if (this.adapter == null) {
			this.adapter = new AgencyPagerAdapter(this, newAgencies);
		} else if (CollectionUtils.getSize(newAgencies) != CollectionUtils.getSize(this.adapter.getAgencies())) {
			this.adapter.setAgencies(newAgencies);
			this.adapter.notifyDataSetChanged();
		}
		setupView(view);
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
					for (int i = 0; i < newAgencies.size(); i++) {
						if (newAgencies.get(i).getAuthority().equals(agencyAuthority)) {
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
					final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
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
		if (view == null) {
			return;
		}
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
			final AgencyProperties agency = this.adapter.getAgency(position);
			if (agency != null) {
				PreferenceUtils
						.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(this.type.getId()), agency.getAuthority(), false);
			}
		}
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(position);
				}
			}
		}
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			resumeAllVisibleAwareChildFragment();
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			pauseAllVisibleAwareChildFragments();
			break;
		}
	}

	private void resumeAllVisibleAwareChildFragment() {
		List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(this.lastPageSelected); // resume
				}
			}
		}
	}

	private void pauseAllVisibleAwareChildFragments() {
		List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(-1); // pause
				}
			}
		}
	}

	@Override
	public CharSequence getABTitle(Context context) {
		if (this.type == null) {
			return context.getString(R.string.ellipsis);
		}
		return context.getString(this.type.getShortNameResId()).toUpperCase(Locale.ENGLISH);
	}

	@Override
	public int getABIconDrawableResId() {
		if (this.type != null && this.type.getAbIconResId() != -1) {
			return this.type.getAbIconResId();
		}
		return super.getABIconDrawableResId();
	}


	private static class AgencyPagerAdapter extends FragmentStatePagerAdapter implements MTLog.Loggable {

		private static final String TAG = AgencyTypeFragment.class.getSimpleName() + ">" + AgencyPagerAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private List<AgencyProperties> agencies;
		private WeakReference<Context> contextWR;
		private Location userLocation;
		private int lastVisibleFragmentPosition = -1;
		private int saveStateCount = -1;

		public AgencyPagerAdapter(AgencyTypeFragment agencyTypeFragment, List<AgencyProperties> agencies) {
			super(agencyTypeFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(agencyTypeFragment.getActivity());
			this.agencies = agencies;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
			if (this.saveStateCount >= 0 && this.saveStateCount != getCount()) {
				return;
			}
			try {
				super.restoreState(state, loader);
			} catch (IllegalStateException ise) {
				MTLog.w(this, ise, "Error while restoring state!");
			}
		}

		@Override
		public Parcelable saveState() {
			try {
				this.saveStateCount = getCount();
				return super.saveState();
			} catch (IllegalStateException ise) {
				MTLog.w(this, ise, "Error while saving fragment state!");
				return null;
			}
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			try {
				super.destroyItem(container, position, object);
			} catch (IllegalStateException ise) {
				MTLog.w(this, ise, "Error while destroying fragment at position '%s'!", position);
			}
		}

		public void setAgencies(List<AgencyProperties> agencies) {
			this.agencies = agencies;
		}

		public List<AgencyProperties> getAgencies() {
			return agencies;
		}

		public AgencyProperties getAgency(int position) {
			return this.agencies.size() == 0 ? null : this.agencies.get(position);
		}

		@Override
		public int getItemPosition(Object object) {
			if (object instanceof AgencyFragment) {
				return getAgencyItemPosition(((AgencyFragment) object).getAgency());
			} else {
				return POSITION_NONE;
			}
		}

		public int getAgencyItemPosition(AgencyProperties agency) {
			final int indexOf = this.agencies == null ? -1 : this.agencies.indexOf(agency);
			if (indexOf < 0) {
				return POSITION_NONE;
			}
			return indexOf;
		}

		public void setUserLocation(Location userLocation) {
			this.userLocation = userLocation;
		}

		public void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
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
				final RTSAgencyRoutesFragment f = RTSAgencyRoutesFragment.newInstance(position, this.lastVisibleFragmentPosition, agency);
				f.setLogTag(agency.getShortName());
				return f;
			}
			return AgencyPOIsFragment.newInstance(position, this.lastVisibleFragmentPosition, agency, this.userLocation);
		}

	}

	public static interface AgencyFragment extends VisibilityAwareFragment {
		public AgencyProperties getAgency();
	}

}
