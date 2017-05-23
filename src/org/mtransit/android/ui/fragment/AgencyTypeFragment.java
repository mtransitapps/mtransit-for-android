package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.NavigationDrawerController;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

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
		java.util.Set<Fragment> fragments = getChildFragments();
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
		java.util.Set<Fragment> fragments = getChildFragments();
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
		View view = inflater.inflate(R.layout.fragment_agency_type, container, false);
		setupView(view);
		return view;
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		if (this.adapter != null) {
			FragmentActivity activity = getActivity();
			if (activity == null) {
				return;
			}
			ArrayList<AgencyProperties> newAvailableAgencies = this.typeId == null ? null : DataSourceProvider.get(activity).getTypeDataSources(activity,
					this.typeId);
			if (CollectionUtils.getSize(newAvailableAgencies) == CollectionUtils.getSize(this.typeAgencies)) {
				this.modulesUpdated = false; // nothing to update
				return;
			}
			MainActivity mainActivity = (MainActivity) activity;
			if (mainActivity.isMTResumed()) {
				NavigationDrawerController navigationController = mainActivity.getNavigationDrawerController();
				if (navigationController != null) {
					navigationController.forceReset();
					this.modulesUpdated = false; // processed
				}
			}
		}
	}

	private void setupAdapter(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		TabLayout tabs = (TabLayout) view.findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);
		notifyTabDataChanged(view);
		showSelectedTab(view);
	}

	private void notifyTabDataChanged(View view) {
	}

	private void showSelectedTab(View view) {
		if (view == null) {
			return;
		}
		if (!hasTypeAgencies()) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			return;
		}
		if (this.lastPageSelected < 0) {
			TaskUtils.execute(new LoadLastPageSelectedFromUserPreference(getActivity(), this, this.typeId, getTypeAgenciesOrNull()));
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(this.lastPageSelected);
		this.selectedPosition = this.lastPageSelected; // set selected position before update tabs color
		updateABColorNow(view);
		switchView(view);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(3);
		viewPager.addOnPageChangeListener(this);
		TabLayout tabs = (TabLayout) view.findViewById(R.id.tabs);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
		tabs.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
		setupAdapter(view);
	}

	private ActionBarController.SimpleActionBarColorizer abColorizer;

	private ActionBarController.ActionBarColorizer getABColorizer(Context context) {
		if (this.abColorizer == null) {
			initABColorizer(context);
		}
		return this.abColorizer;
	}

	private void initABColorizer(Context context) {
		ArrayList<AgencyProperties> agencies = getTypeAgenciesOrNull();
		if (context != null && agencies != null && agencies.size() > 0) {
			int defaultColor = ThemeUtils.resolveColorAttribute(context, R.attr.colorPrimary);
			this.abColorizer = new ActionBarController.SimpleActionBarColorizer();
			int[] agencyColors = new int[agencies.size()];
			for (int i = 0; i < agencies.size(); i++) {
				AgencyProperties agency = agencies.get(i);
				agencyColors[i] = agency.hasColor() ? agency.getColorInt() : defaultColor;
			}
			this.abColorizer.setBgColors(agencyColors);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.typeId != null) {
			outState.putInt(EXTRA_TYPE_ID, this.typeId);
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		Integer typeId = BundleUtils.getInt(EXTRA_TYPE_ID, bundles);
		if (typeId != null && !typeId.equals(this.typeId)) {
			this.typeId = typeId;
			resetType();
			resetTypeAgencies();
		}
	}

	private Integer typeId = null;

	private DataSourceType type;

	private ArrayList<AgencyProperties> typeAgencies = null;

	private void resetType() {
		this.type = null;
	}

	private void resetTypeAgencies() {
		this.typeAgencies = null;
	}

	private boolean hasType() {
		if (this.type == null) {
			initTypeAsync();
			return false;
		}
		return true;
	}

	private boolean hasTypeAgencies() {
		if (this.typeAgencies == null) {
			initTypeAgenciesAsync();
			return false;
		}
		return true;
	}

	private void initTypeAsync() {
		if (this.loadTypeTask != null && this.loadTypeTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (this.typeId == null) {
			return;
		}
		this.loadTypeTask = new LoadTypeTask();
		TaskUtils.execute(this.loadTypeTask);
	}

	private void initTypeAgenciesAsync() {
		if (this.loadTypeAgenciesTask != null && this.loadTypeAgenciesTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (this.typeId == null) {
			return;
		}
		this.loadTypeAgenciesTask = new LoadTypeAgenciesTask();
		TaskUtils.execute(this.loadTypeAgenciesTask);
	}

	private LoadTypeTask loadTypeTask = null;

	private class LoadTypeTask extends MTAsyncTask<Void, Void, Boolean> {

		@Override
		public String getLogTag() {
			return AgencyTypeFragment.this.getLogTag() + ">" + LoadTypeTask.class.getSimpleName();
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
	}

	private LoadTypeAgenciesTask loadTypeAgenciesTask = null;

	private class LoadTypeAgenciesTask extends MTAsyncTask<Void, Void, Boolean> {

		@Override
		public String getLogTag() {
			return AgencyTypeFragment.this.getLogTag() + ">" + LoadTypeAgenciesTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initTypeAgenciesSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewTypeAgencies();
			}
		}
	}

	private void applyNewType() {
		if (this.type == null) {
			return;
		}
		ActionBarController actionBarController = getAbController();
		if (actionBarController != null) {
			actionBarController.setABTitle(this, getABTitle(getActivity()), false);
			actionBarController.setABReady(this, isABReady(), true);
		}
	}

	private void applyNewTypeAgencies() {
		if (this.typeAgencies == null) {
			return;
		}
		if (this.adapter != null) {
			this.adapter.setAgencies(this.typeAgencies);
			this.abBgColor = null; // reset
			this.abColorizer = null; // reset
			View view = getView();
			notifyTabDataChanged(view);
			showSelectedTab(view);
		}
	}

	private boolean initTypeSync() {
		if (this.type != null) {
			return false;
		}
		if (this.typeId != null) {
			this.type = DataSourceType.parseId(this.typeId);
		}
		return this.type != null;
	}

	private boolean initTypeAgenciesSync() {
		if (this.typeAgencies != null) {
			return false;
		}
		if (this.typeId != null) {
			this.typeAgencies = DataSourceProvider.get(getActivity()).getTypeDataSources(getActivity(), this.typeId);
		}
		return this.typeAgencies != null;
	}

	private DataSourceType getTypeOrNull() {
		if (!hasType()) {
			return null;
		}
		return this.type;
	}

	private ArrayList<AgencyProperties> getTypeAgenciesOrNull() {
		if (!hasTypeAgencies()) {
			return null;
		}
		return this.typeAgencies;
	}

	private void initAdapters(Activity activity) {
		this.adapter = new AgencyPagerAdapter(activity, this, null);
	}

	private static class LoadLastPageSelectedFromUserPreference extends MTAsyncTask<Void, Void, Integer> {

		private final String TAG = AgencyTypeFragment.class.getSimpleName() + ">" + LoadLastPageSelectedFromUserPreference.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<Context> contextWR;
		private WeakReference<AgencyTypeFragment> agencyTypeFragmentWR;
		private Integer typeId;
		private ArrayList<AgencyProperties> newAgencies;

		public LoadLastPageSelectedFromUserPreference(Context context, AgencyTypeFragment agencyTypeFragment, Integer typeId,
				ArrayList<AgencyProperties> newAgencies) {
			this.contextWR = new WeakReference<Context>(context);
			this.agencyTypeFragmentWR = new WeakReference<AgencyTypeFragment>(agencyTypeFragment);
			this.typeId = typeId;
			this.newAgencies = newAgencies;
		}

		@Override
		protected Integer doInBackgroundMT(Void... params) {
			try {
				String agencyAuthority;
				Context context = this.contextWR == null ? null : this.contextWR.get();
				if (context != null) {
					String typePref = PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(this.typeId);
					agencyAuthority = PreferenceUtils.getPrefLcl(context, typePref, PreferenceUtils.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT);
				} else {
					agencyAuthority = PreferenceUtils.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT;
				}
				if (this.newAgencies != null) {
					for (int i = 0; i < this.newAgencies.size(); i++) {
						if (this.newAgencies.get(i).getAuthority().equals(agencyAuthority)) {
							return i;
						}
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while determining the select agency tab!");
			}
			return null;
		}

		@Override
		protected void onPostExecute(Integer lastPageSelected) {
			AgencyTypeFragment agencyTypeFragment = this.agencyTypeFragmentWR == null ? null : this.agencyTypeFragmentWR.get();
			if (agencyTypeFragment == null) {
				return; // too late
			}
			if (agencyTypeFragment.lastPageSelected >= 0) {
				return; // user has manually move to another page before, too late
			}
			if (lastPageSelected == null) {
				agencyTypeFragment.lastPageSelected = 0;
			} else {
				agencyTypeFragment.lastPageSelected = lastPageSelected;
			}
			View view = agencyTypeFragment.getView();
			agencyTypeFragment.showSelectedTab(view);
			agencyTypeFragment.onPageSelected(agencyTypeFragment.lastPageSelected); // tell current page it's selected
		}
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
						if (AgencyTypeFragment.this.modulesUpdated) {
							onModulesUpdated();
						}
					}
				});
			}
		}
		if (this.lastPageSelected >= 0) {
			onPageSelected(this.lastPageSelected); // tell current page it's selected
		}
		switchView(view);
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			MTActivityWithLocation.broadcastUserLocationChanged(this, getChildFragments(), newLocation);
		}
	}

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
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
		if (this.typeId == null || this.typeId != DataSourceType.TYPE_MODULE.getId()) {
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
		if (this.typeId != null && this.adapter != null && this.adapter.isInitialized()) {
			String agencyAuthority = this.adapter.getAgencyAuthority(position);
			if (!TextUtils.isEmpty(agencyAuthority)) {
				PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(this.typeId), agencyAuthority, false);
			}
		}
		setFragmentVisibleAtPosition(position);
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
		if (!hasTypeAgencies()) {
			return;
		}
		if (Math.abs(this.lastPageSelected - position) > 1) {
			return;
		}
		this.selectedPosition = position;
		this.selectionOffset = positionOffset;
		restartUpdateABColorLater();
	}

	private Handler handler = new Handler();

	private Runnable updateABColorLater = new Runnable() {
		@Override
		public void run() {
			updateABColorNow(getView());
		}
	};

	private void updateABColorNow(View view) {
		this.abBgColor = getNewABBgColor(getActivity());
		if (this.abBgColor != null) {
			if (view != null) {
				View tabs = view.findViewById(R.id.tabs);
				if (tabs != null) {
					tabs.setBackgroundColor(this.abBgColor);
				}
			}
			getAbController().setABBgColor(this, getABBgColor(getActivity()), false);
			getAbController().updateABBgColor();
			cancelUpdateABColorLater();
		}
	}

	private boolean updateABColorLaterScheduled = false;

	private void restartUpdateABColorLater() {
		if (!this.updateABColorLaterScheduled) {
			this.handler.postDelayed(this.updateABColorLater, TimeUnit.MILLISECONDS.toMillis(50));
			this.updateABColorLaterScheduled = true;
		}
	}

	private void cancelUpdateABColorLater() {
		if (this.updateABColorLater != null) {
			this.handler.removeCallbacks(this.updateABColorLater);
		}
		this.updateABColorLaterScheduled = false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelUpdateABColorLater();
		TaskUtils.cancelQuietly(this.loadTypeAgenciesTask, true);
		TaskUtils.cancelQuietly(this.loadTypeTask, true);
	}

	private Integer abBgColor = null;

	@Override
	public Integer getABBgColor(Context context) {
		if (this.abBgColor == null) {
			this.abBgColor = getNewABBgColor(context);
		}
		if (this.abBgColor == null) {
			return super.getABBgColor(context);
		}
		return this.abBgColor;
	}

	private Integer getNewABBgColor(Context context) {
		if (getABColorizer(context) != null && this.selectedPosition >= 0) {
			int color = getABColorizer(context).getBgColor(this.selectedPosition);
			if (this.selectionOffset > 0f && this.selectedPosition < (this.adapter.getCount() - 1)) {
				int nextColor = getABColorizer(context).getBgColor(this.selectedPosition + 1);
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
			setFragmentVisibleAtPosition(this.lastPageSelected); // resume
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			setFragmentVisibleAtPosition(-1); // pause
			break;
		}
	}

	private void setFragmentVisibleAtPosition(int position) {
		java.util.Set<Fragment> fragments = getChildFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(position);
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
		if (context == null) {
			return super.getABTitle(null);
		}
		DataSourceType type = getTypeOrNull();
		if (type == null) {
			return context.getString(R.string.ellipsis);
		}
		return context.getString(type.getAllStringResId());
	}

	private static class AgencyPagerAdapter extends FragmentStatePagerAdapter implements MTLog.Loggable {

		private static final String TAG = AgencyTypeFragment.class.getSimpleName() + ">" + AgencyPagerAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private ArrayList<AgencyProperties> agencies;
		private WeakReference<Context> contextWR;
		private int lastVisibleFragmentPosition = -1;
		private int saveStateCount = -1;

		public AgencyPagerAdapter(Context context, AgencyTypeFragment agencyTypeFragment, ArrayList<AgencyProperties> agencies) {
			super(agencyTypeFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(context);
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
			notifyDataSetChanged();
		}

		public boolean isInitialized() {
			return this.agencies != null && this.agencies.size() > 0;
		}

		public AgencyProperties getAgency(int position) {
			return this.agencies.size() == 0 ? null : this.agencies.get(position);
		}

		public String getAgencyAuthority(int position) {
			return this.agenciesAuthority == null || this.agenciesAuthority.size() <= position ? null : this.agenciesAuthority.get(position);
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
		public Fragment getItem(int position) {
			AgencyProperties agency = getAgency(position);
			if (agency != null && agency.isRTS()) {
				Integer optColorInt = agency.hasColor() ? agency.getColorInt() : null;
				RTSAgencyRoutesFragment f = RTSAgencyRoutesFragment.newInstance( //
						position, this.lastVisibleFragmentPosition, agency.getAuthority(), optColorInt);
				f.setLogTag(agency.getShortName());
				return f;
			}
			Integer optColorInt = agency == null || !agency.hasColor() ? null : agency.getColorInt();
			return AgencyPOIsFragment.newInstance(position, this.lastVisibleFragmentPosition, getAgencyAuthority(position), optColorInt, null);
		}
	}

	public interface AgencyFragment extends VisibilityAwareFragment {
		String getAgencyAuthority();
	}
}
