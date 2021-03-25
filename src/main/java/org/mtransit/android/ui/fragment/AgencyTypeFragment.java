package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import androidx.annotation.ColorInt;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.NavigationDrawerController;
import org.mtransit.android.util.CrashUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES;

public class AgencyTypeFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener {

	private static final String TAG = AgencyTypeFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG + "-" + getTypeId();
	}

	private static final String TRACKING_SCREEN_NAME = "Browse";

	@NonNull
	@Override
	public String getScreenName() {
		if (this.type != null) {
			return TRACKING_SCREEN_NAME + "/" + this.type.getId();
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_TYPE_ID = "extra_type_id";

	@NonNull
	public static AgencyTypeFragment newInstance(@NonNull DataSourceType dst) {
		AgencyTypeFragment f = new AgencyTypeFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE_ID, dst.getId());
		f.type = dst;
		f.setArguments(args);
		return f;
	}

	private AgencyPagerAdapter adapter;
	private int lastPageSelected = -1;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	@NonNull
	private final LiveData<List<AgencyProperties>> allAgenciesLD;

	@NonNull
	private final MutableLiveData<DataSourceType> typeLD;

	@NonNull
	private final MediatorLiveData<List<AgencyProperties>> typeAgenciesLD = new MediatorLiveData<>();

	public AgencyTypeFragment() {
		super();
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
		if (F_CACHE_DATA_SOURCES) {
			this.typeLD = new MutableLiveData<>(this.type);
			this.allAgenciesLD = this.dataSourcesRepository.readingAllAgencies();
		}
	}

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true); // child fragments options menus don't get updated when coming back from another activity
		restoreInstanceState(savedInstanceState, getArguments());
		if (F_CACHE_DATA_SOURCES) {
			this.typeAgenciesLD.addSource(this.typeLD, newType -> {
						this.typeAgenciesLD.setValue(
								combine(newType, this.allAgenciesLD.getValue())
						);
					}
			);
			this.typeAgenciesLD.addSource(this.allAgenciesLD, newAgencies -> {
						this.typeAgenciesLD.setValue(
								combine(this.typeLD.getValue(), newAgencies)
						);
					}
			);
			this.typeAgenciesLD.observe(this, newTypeAgencies -> {
						if (this.typeAgencies != null && this.typeAgencies.equals(newTypeAgencies)) {
							return;
						}
						this.typeAgencies = newTypeAgencies;
						applyNewTypeAgencies();
					}
			);
			this.typeLD.postValue(this.type); // trigger once DataSourcesReader
		}
	}

	@Nullable
	private List<AgencyProperties> combine(@Nullable DataSourceType dst, @Nullable List<AgencyProperties> allAgencies) {
		if (dst == null || allAgencies == null) {
			return null;
		}
		List<AgencyProperties> filteredAgencyType = new ArrayList<>();
		for (AgencyProperties agency : allAgencies) { // already sorted
			if (agency.getType() != dst) {
				continue;
			}
			filteredAgencyType.add(agency);
		}
		return filteredAgencyType;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
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
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_nearby) {
			if (getActivity() != null) {
				((MainActivity) getActivity()).addFragmentToStack(NearbyFragment.newNearbyInstance(null, getTypeId()), this);
			}
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

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
		if (!F_CACHE_DATA_SOURCES) {
			if (this.adapter != null) {
				FragmentActivity activity = getActivity();
				if (activity == null) {
					return;
				}
				List<AgencyProperties> newAvailableAgencies;
				if (F_CACHE_DATA_SOURCES) {
					newAvailableAgencies = this.type == null ? null : this.dataSourcesRepository.getTypeDataSources(this.type);
				} else {
					newAvailableAgencies = this.type == null ? null : org.mtransit.android.data.DataSourceProvider.get(activity).getTypeDataSources(activity, this.type);
				}
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
		} else {
			this.modulesUpdated = false; // processed
		}
	}

	private void setupAdapter(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		TabLayout tabs = view.findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);
		notifyTabDataChanged(view);
		showSelectedTab(view);
	}

	private void notifyTabDataChanged(@SuppressWarnings("unused") View view) {
		// DO NOTHING
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
			TaskUtils.execute(new LoadLastPageSelectedFromUserPreference(this, getTypeId(), getTypeAgenciesOrNull()));
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(this.lastPageSelected);
		this.selectedPosition = this.lastPageSelected; // set selected position before update tabs color
		updateABColorNow(view);
		switchView(view);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(3);
		viewPager.addOnPageChangeListener(this);
		TabLayout tabs = view.findViewById(R.id.tabs);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
		tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
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
		List<AgencyProperties> agencies = getTypeAgenciesOrNull();
		if (context != null && agencies != null && agencies.size() > 0) {
			int defaultColor = ThemeUtils.resolveColorAttribute(context, R.attr.colorPrimary);
			this.abColorizer = new ActionBarController.SimpleActionBarColorizer();
			int[] agencyColors = new int[agencies.size()];
			for (int i = 0; i < agencies.size(); i++) {
				AgencyProperties agency = agencies.get(i);
				agencyColors[i] = agency.getColorInt() != null ? agency.getColorInt() : defaultColor;
			}
			this.abColorizer.setBgColors(agencyColors);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (this.type != null) {
			outState.putInt(EXTRA_TYPE_ID, this.type.getId());
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		final Integer newTypeId = BundleUtils.getInt(EXTRA_TYPE_ID, bundles);
		if (newTypeId != null && !newTypeId.equals(getTypeId())) {
			this.type = DataSourceType.parseId(newTypeId);
			this.typeLD.postValue(this.type);
			applyNewType();
			resetTypeAgencies();
		}
	}

	@Nullable
	private DataSourceType type = null;

	@Nullable
	private List<AgencyProperties> typeAgencies = null;

	@Nullable
	private Integer getTypeId() {
		return this.type == null ? null : this.type.getId();
	}

	private void resetTypeAgencies() {
		this.typeAgencies = null;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean hasTypeAgencies() {
		if (this.typeAgencies == null) {
			initTypeAgenciesAsync();
			return false;
		}
		return true;
	}

	private void initTypeAgenciesAsync() {
		if (this.loadTypeAgenciesTask != null && this.loadTypeAgenciesTask.getStatus() == LoadTypeAgenciesTask.Status.RUNNING) {
			return;
		}
		if (this.type == null) {
			return;
		}
		this.loadTypeAgenciesTask = new LoadTypeAgenciesTask(this);
		TaskUtils.execute(this.loadTypeAgenciesTask);
	}

	@Nullable
	private LoadTypeAgenciesTask loadTypeAgenciesTask = null;

	@SuppressWarnings("deprecation")
	private static class LoadTypeAgenciesTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, AgencyTypeFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return AgencyTypeFragment.class.getSimpleName() + ">" + LoadTypeAgenciesTask.class.getSimpleName();
		}

		LoadTypeAgenciesTask(AgencyTypeFragment agencyTypeFragment) {
			super(agencyTypeFragment);
		}

		@WorkerThread
		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull AgencyTypeFragment agencyTypeFragment, Void... params) {
			return agencyTypeFragment.initTypeAgenciesSync();
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull AgencyTypeFragment agencyTypeFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				agencyTypeFragment.applyNewTypeAgencies();
			}
		}
	}

	private void applyNewType() {
		if (this.type == null) {
			return;
		}
		ActionBarController actionBarController = getAbController();
		if (actionBarController != null) {
			actionBarController.setABTitle(this, getABTitle(getContext()), false);
			actionBarController.setABReady(this, isABReady(), true);
		}
	}

	@MainThread
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

	@WorkerThread
	private boolean initTypeAgenciesSync() {
		if (this.typeAgencies != null) {
			return false;
		}
		if (this.type != null) {
			if (F_CACHE_DATA_SOURCES) {
				this.typeAgencies = this.dataSourcesRepository.getTypeDataSources(this.type);
			} else {
				this.typeAgencies = org.mtransit.android.data.DataSourceProvider.get(getContext()).getTypeDataSources(getContext(), this.type);
			}
		}
		return this.typeAgencies != null;
	}

	@Nullable
	private DataSourceType getTypeOrNull() {
		return this.type;
	}

	@Nullable
	private List<AgencyProperties> getTypeAgenciesOrNull() {
		if (!hasTypeAgencies()) {
			return null;
		}
		return this.typeAgencies;
	}

	private void initAdapters(Activity activity) {
		this.adapter = new AgencyPagerAdapter(activity, this, null);
	}

	@SuppressWarnings("deprecation")
	private static class LoadLastPageSelectedFromUserPreference extends MTCancellableFragmentAsyncTask<Void, Void, Integer, AgencyTypeFragment> {

		private static final String LOG_TAG = AgencyTypeFragment.class.getSimpleName() + ">" + LoadLastPageSelectedFromUserPreference.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@Nullable
		private final Integer typeId;
		@Nullable
		private final List<AgencyProperties> newAgencies;

		LoadLastPageSelectedFromUserPreference(@NonNull AgencyTypeFragment agencyTypeFragment, @Nullable Integer typeId, @Nullable List<AgencyProperties> newAgencies) {
			super(agencyTypeFragment);
			this.typeId = typeId;
			this.newAgencies = newAgencies;
		}

		@Nullable
		@Override
		protected Integer doInBackgroundNotCancelledWithFragmentMT(@NonNull AgencyTypeFragment agencyTypeFragment, Void... params) {
			try {
				String agencyAuthority;
				Context context = agencyTypeFragment.getContext();
				if (context != null && this.typeId != null) {
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
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull AgencyTypeFragment agencyTypeFragment, @Nullable Integer lastPageSelected) {
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
				view.post(() -> {
					if (AgencyTypeFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
		}
		if (this.lastPageSelected >= 0) {
			onPageSelected(this.lastPageSelected); // tell current page it's selected
		}
		switchView(view);
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
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
		if (this.type == null || this.type != DataSourceType.TYPE_MODULE) {
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
		final Integer typeId = getTypeId();
		if (typeId != null && this.adapter != null && this.adapter.isInitialized()) {
			String agencyAuthority = this.adapter.getAgencyAuthority(position);
			if (!TextUtils.isEmpty(agencyAuthority)) {
				PreferenceUtils.savePrefLcl(getContext(), PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(typeId), agencyAuthority, false);
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

	private final Handler handler = new Handler();

	private final Runnable updateABColorLater = () ->
			updateABColorNow(getView());

	private void updateABColorNow(View view) {
		this.abBgColor = getNewABBgColor(getContext());
		if (this.abBgColor != null) {
			if (view != null) {
				View tabs = view.findViewById(R.id.tabs);
				if (tabs != null) {
					tabs.setBackgroundColor(this.abBgColor);
				}
			}
			final ActionBarController abController = getAbController();
			if (abController != null) {
				abController.setABBgColor(this, getABBgColor(getContext()), false);
				abController.updateABBgColor();
			}
			cancelUpdateABColorLater();
		}
	}

	private boolean updateABColorLaterScheduled = false;

	private void restartUpdateABColorLater() {
		if (!this.updateABColorLaterScheduled) {
			this.handler.postDelayed(this.updateABColorLater, TimeUnit.MILLISECONDS.toMillis(50L));
			this.updateABColorLaterScheduled = true;
		}
	}

	private void cancelUpdateABColorLater() {
		this.handler.removeCallbacks(this.updateABColorLater);
		this.updateABColorLaterScheduled = false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelUpdateABColorLater();
		TaskUtils.cancelQuietly(this.loadTypeAgenciesTask, true);
	}

	@Nullable
	private Integer abBgColor = null;

	@Nullable
	@ColorInt
	@Override
	public Integer getABBgColor(@Nullable Context context) {
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
		return this.type != null;
	}

	@Nullable
	@Override
	public CharSequence getABTitle(@Nullable Context context) {
		if (context == null) {
			return super.getABTitle(null);
		}
		DataSourceType type = getTypeOrNull();
		if (type == null) {
			return context.getString(R.string.ellipsis);
		}
		return context.getString(type.getAllStringResId());
	}

	@SuppressWarnings("deprecation")
	private static class AgencyPagerAdapter extends FragmentStatePagerAdapter implements MTLog.Loggable {

		private static final String TAG = AgencyTypeFragment.class.getSimpleName() + ">" + AgencyPagerAdapter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		private List<AgencyProperties> agencies;
		private final WeakReference<Context> contextWR;
		private int lastVisibleFragmentPosition = -1;
		private int saveStateCount = -1;

		AgencyPagerAdapter(Context context, AgencyTypeFragment agencyTypeFragment, ArrayList<AgencyProperties> agencies) {
			super(agencyTypeFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<>(context);
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
		public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
			try {
				super.destroyItem(container, position, object);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while destroying fragment at position '%s'!", position);
			}
		}

		@NonNull
		private final ArrayList<String> agenciesAuthority = new ArrayList<>();

		public void setAgencies(List<AgencyProperties> agencies) {
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

		@Nullable
		String getAgencyAuthority(int position) {
			return this.agenciesAuthority.size() <= position ? null : this.agenciesAuthority.get(position);
		}

		@Override
		public int getItemPosition(@NonNull Object object) {
			if (object instanceof AgencyFragment) {
				return getAgencyItemPosition(((AgencyFragment) object).getAgencyAuthority());
			} else {
				return POSITION_NONE;
			}
		}

		private int getAgencyItemPosition(@Nullable String agencyAuthority) {
			int indexOf = this.agenciesAuthority.indexOf(agencyAuthority);
			if (indexOf < 0) {
				return POSITION_NONE;
			}
			return indexOf;
		}

		void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.agenciesAuthority.size();
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

		@NonNull
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
			String agencyAuthority = getAgencyAuthority(position);
			if (agencyAuthority == null || agencyAuthority.isEmpty()) {
				CrashUtils.w(this, "No agency authority at position %d!", position);
				throw new RuntimeException("No agency authority at position " + position + "!");
			}
			return AgencyPOIsFragment.newInstance(position, this.lastVisibleFragmentPosition, agencyAuthority, optColorInt, null);
		}
	}

	public interface AgencyFragment extends VisibilityAwareFragment {
		@Nullable
		String getAgencyAuthority();
	}
}
