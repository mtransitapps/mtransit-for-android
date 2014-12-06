package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.NavigationDrawerController;
import org.mtransit.android.ui.view.SlidingTabLayout;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
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
		return TAG + "-" + this.typeId;
	}

	private static final String TRACKING_SCREEN_NAME = "Browse";

	@Override
	public String getScreenName() {
		if (this.typeId != null) {
			return TRACKING_SCREEN_NAME + "/" + this.typeId;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_TYPE_ID = "extra_type_id";

	public static AgencyTypeFragment newInstance(int typeId, DataSourceType optType) {
		AgencyTypeFragment f = new AgencyTypeFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE_ID, typeId);
		f.typeId = typeId;
		f.type = optType;
		f.setArguments(args);
		return f;
	}

	private AgencyPagerAdapter adapter;
	private int lastPageSelected = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true); // child fragments options menus don't get updated when coming back from another activity
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_agency_type, menu);
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
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
		switch (item.getItemId()) {
		case R.id.menu_nearby:
			((MainActivity) getActivity()).addFragmentToStack(NearbyFragment.newNearbyInstance(null, this.typeId));
			return true; // handled
		}
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
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
		restoreInstanceState(savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_agency_type, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		View view = getView();
		switchView(view);
		this.adapter = null; // reset
		initTabsAndViewPager(view);
	}

	@Override
	public void onModulesUpdated() {
		if (this.adapter != null) {
			ArrayList<AgencyProperties> newAvailableAgencies = this.typeId == null ? null : DataSourceProvider.get(getActivity()).getTypeDataSources(
					getActivity(), this.typeId);
			if (CollectionUtils.getSize(newAvailableAgencies) == CollectionUtils.getSize(this.adapter.getAgencies())) {
				return;
			}
			MainActivity mainActivity = (MainActivity) getActivity();
			if (mainActivity != null) {
				NavigationDrawerController navigationController = mainActivity.getNavigationDrawerController();
				if (navigationController != null) {
					navigationController.forceReset();
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.adapter = null;
	}

	private void setupAdapter(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(3);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setCustomTabView(R.layout.layout_tab_indicator, R.id.tab_title);
		tabs.setOnPageChangeListener(this);
		tabs.setSelectedIndicatorColors(Color.WHITE);
	}

	private ActionBarController.SimpleActionBarColorizer abColorizer;

	private ActionBarController.ActionBarColorizer getABColorizer() {
		if (this.abColorizer == null) {
			initABColorizer();
		}
		return this.abColorizer;
	}

	private void initABColorizer() {
		if (this.adapter != null) {
			int defaultColor = ThemeUtils.resolveColorAttribute(getActivity(), R.attr.colorPrimary);
			this.abColorizer = new ActionBarController.SimpleActionBarColorizer();
			if (this.adapter.getCount() == 0) {
				this.abColorizer.setBgColors(defaultColor);
			} else {
				ArrayList<AgencyProperties> agencies = this.adapter.getAgencies();
				int[] agencyColors = new int[agencies.size()];
				for (int i = 0; i < agencies.size(); i++) {
					AgencyProperties agency = agencies.get(i);
					agencyColors[i] = agency.hasColor() ? agency.getColorInt() : defaultColor;
				}
				this.abColorizer.setBgColors(agencyColors);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.typeId != null) {
			outState.putInt(EXTRA_TYPE_ID, this.typeId.intValue());
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		Integer typeId = BundleUtils.getInt(EXTRA_TYPE_ID, bundles);
		if (typeId != null && !typeId.equals(this.typeId)) {
			this.typeId = typeId;
			resetType();
		}
	}

	private Integer typeId = null;

	private DataSourceType type;

	private void resetType() {
		this.type = null;
	}

	private boolean hasType() {
		if (this.type == null) {
			initTypeAsync();
			return false;
		}
		return true;
	}

	private void initTypeAsync() {
		if (this.loadTypeTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (this.typeId == null) {
			return;
		}
		this.loadTypeTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadTypeTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadTypeTask";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initTypeSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewType();
			}
		}
	};

	private void applyNewType() {
		if (this.type == null) {
			return;
		}
		getAbController().setABTitle(this, getABTitle(getActivity()), false);
		getAbController().setABReady(this, isABReady(), true);
	}

	private boolean initTypeSync() {
		if (this.type != null) {
			return false;
		}
		if (this.typeId != null) {
			this.type = DataSourceType.parseId(this.typeId.intValue());
		}
		return this.type != null;
	}

	private DataSourceType getTypeOrNull() {
		if (!hasType()) {
			return null;
		}
		return this.type;
	}

	private void initTabsAndViewPager(final View view) {
		if (view == null) {
			return;
		}
		final ArrayList<AgencyProperties> newAgencies = this.typeId == null ? null : DataSourceProvider.get(getActivity()).getTypeDataSources(getActivity(),
				this.typeId);
		if (CollectionUtils.getSize(newAgencies) == 0) {
			return;
		}
		if (this.adapter == null) {
			this.adapter = new AgencyPagerAdapter(this, newAgencies);
		} else if (CollectionUtils.getSize(newAgencies) != CollectionUtils.getSize(this.adapter.getAgencies())) {
			this.adapter.setAgencies(newAgencies);
			this.adapter.notifyDataSetChanged();
		}
		setupAdapter(view);
		this.lastPageSelected = -1;
		new MTAsyncTask<Void, Void, Integer>() {

			private final String TAG = AgencyTypeFragment.class.getSimpleName() + ">LoadLastPageSelectedFromUserPreferences";

			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Integer doInBackgroundMT(Void... params) {
				try {
					String agencyAuthority = PreferenceUtils.getPrefLcl(getActivity(),
							PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(AgencyTypeFragment.this.typeId),
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
				if (AgencyTypeFragment.this.lastPageSelected >= 0) {
					return; // user has manually move to another page before, too late
				}
				if (lastPageSelected == null) {
					AgencyTypeFragment.this.lastPageSelected = 0;
				} else {
					AgencyTypeFragment.this.lastPageSelected = lastPageSelected.intValue();
					ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
					viewPager.setCurrentItem(AgencyTypeFragment.this.lastPageSelected);
				}
				onPageSelected(AgencyTypeFragment.this.lastPageSelected); // tell current page it's selected
				switchView(view);

			}
		}.execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			MTActivityWithLocation.broadcastUserLocationChanged(this, getChildFragmentManager(), newLocation);
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
		if (this.typeId == null || this.typeId.intValue() != DataSourceType.TYPE_MODULE.getId()) {
			view.findViewById(R.id.tabs).setVisibility(View.VISIBLE); // show
		}
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
		ServiceUpdateLoader.get().clearAllTasks();
		if (this.adapter != null) {
			String agencyAuthority = this.adapter.getAgencyAuthority(position);
			if (!TextUtils.isEmpty(agencyAuthority)) {
				PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(this.typeId), agencyAuthority, false);
			}
		}
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
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
		onViewPagerPageChanged(position, positionOffset);
	}

	private int selectedPosition = -1;
	private float selectionOffset = 0f;

	private void onViewPagerPageChanged(int position, float positionOffset) {
		this.selectedPosition = position;
		this.selectionOffset = positionOffset;
		this.abBgColor = getNewABBgColor();
		if (this.abBgColor != null) {
			getView().findViewById(R.id.tabs).setBackgroundColor(this.abBgColor);
			getAbController().setABBgColor(this, getABBgColor(getActivity()), false);
			getAbController().updateABBgColor();
		}
	}

	private Integer abBgColor = null;

	@Override
	public Integer getABBgColor(Context context) {
		if (this.abBgColor == null) {
			this.abBgColor = getNewABBgColor();
		}
		return this.abBgColor;
	}

	private Integer getNewABBgColor() {
		if (getABColorizer() != null && this.selectedPosition >= 0) {
			int color = getABColorizer().getBgColor(this.selectedPosition);
			if (this.selectionOffset > 0f && this.selectedPosition < (this.adapter.getCount() - 1)) {
				int nextColor = getABColorizer().getBgColor(this.selectedPosition + 1);
				if (color != nextColor) {
					return ColorUtils.blendColors(nextColor, color, this.selectionOffset);
				}
			}
			return color;
		}
		return null;
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
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(this.lastPageSelected); // resume
				}
			}
		}
	}

	private void pauseAllVisibleAwareChildFragments() {
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(-1); // pause
				}
			}
		}
	}

	@Override
	public boolean isABReady() {
		return hasType();
	}

	@Override
	public CharSequence getABTitle(Context context) {
		DataSourceType type = getTypeOrNull();
		if (type == null) {
			return context.getString(R.string.ellipsis);
		}
		return context.getString(type.getAllStringResId());
	}


	private static class AgencyPagerAdapter extends FragmentStatePagerAdapter implements SlidingTabLayout.TabColorizer, MTLog.Loggable {

		private static final String TAG = AgencyTypeFragment.class.getSimpleName() + ">" + AgencyPagerAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private ArrayList<AgencyProperties> agencies;
		private WeakReference<Context> contextWR;
		private Location userLocation;
		private int lastVisibleFragmentPosition = -1;
		private int saveStateCount = -1;

		public AgencyPagerAdapter(AgencyTypeFragment agencyTypeFragment, ArrayList<AgencyProperties> agencies) {
			super(agencyTypeFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(agencyTypeFragment.getActivity());
			setAgencies(agencies);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
			if (this.saveStateCount >= 0 && this.saveStateCount != getCount()) {
				return;
			}
			try {
				super.restoreState(state, loader);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while restoring state!");
			}
		}

		@Override
		public Parcelable saveState() {
			try {
				this.saveStateCount = getCount();
				return super.saveState();
			} catch (Exception e) {
				MTLog.w(this, e, "Error while saving fragment state!");
				return null;
			}
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			try {
				super.destroyItem(container, position, object);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while destroying fragment at position '%s'!", position);
			}
		}


		private ArrayList<String> agenciesAuthority = new ArrayList<String>();

		public void setAgencies(ArrayList<AgencyProperties> agencies) {
			this.agencies = agencies;
			this.agenciesAuthority.clear();
			if (this.agencies != null) {
				for (AgencyProperties agency : this.agencies) {
					this.agenciesAuthority.add(agency.getAuthority());
				}
			}

		}

		public ArrayList<AgencyProperties> getAgencies() {
			return agencies;
		}

		public AgencyProperties getAgency(int position) {
			return this.agencies.size() == 0 ? null : this.agencies.get(position);
		}

		public String getAgencyAuthority(int position) {
			return this.agenciesAuthority == null ? null : this.agenciesAuthority.get(position);
		}

		@Override
		public int getItemPosition(Object object) {
			if (object instanceof AgencyFragment) {
				return getAgencyItemPosition(((AgencyFragment) object).getAgencyAuthority());
			} else {
				return POSITION_NONE;
			}
		}

		private int getAgencyItemPosition(String agencyAuthority) {
			int indexOf = this.agenciesAuthority == null ? -1 : this.agenciesAuthority.indexOf(agencyAuthority);
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
			return this.agenciesAuthority == null ? 0 : this.agenciesAuthority.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.agencies == null || position >= this.agencies.size()) {
				return StringUtils.EMPTY;
			}
			return this.agencies.get(position).getShortName();
		}

		@Override
		public int getIndicatorColor(int position) {
			return Color.WHITE;
		}

		@Override
		public Fragment getItem(int position) {
			AgencyProperties agency = getAgency(position);
			if (agency != null && agency.isRTS()) {
				RTSAgencyRoutesFragment f = RTSAgencyRoutesFragment.newInstance(position, this.lastVisibleFragmentPosition, agency.getAuthority());
				f.setLogTag(agency.getShortName());
				return f;
			}
			return AgencyPOIsFragment.newInstance(position, this.lastVisibleFragmentPosition, agency.getAuthority(), this.userLocation, null, agency);
		}
	}

	public static interface AgencyFragment extends VisibilityAwareFragment {
		public String getAgencyAuthority();
	}

}
