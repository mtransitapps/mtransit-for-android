package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.task.NewsLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTOnItemClickListener;
import org.mtransit.android.ui.widget.ListViewSwipeRefreshLayout;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

public class NewsFragment extends ABFragment implements LoaderManager.LoaderCallbacks<ArrayList<News>>, SwipeRefreshLayout.OnRefreshListener {

	private static final String LOG_TAG = NewsFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "News";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_COLOR_INT = "extra_color_int";
	private static final String EXTRA_SUB_TITLE = "extra_subtitle";
	private static final String EXTRA_FILTER_TARGET_AUTHORITIES = "extra_filter_target_authorities";
	private static final String EXTRA_FILTER_TARGETS = "extra_filter_targets";
	private static final String EXTRA_FILTER_UUIDS = "extra_filter_uuids";

	@NonNull
	public static NewsFragment newInstance(@Nullable Integer optColorInt, @Nullable String optSubtitle, @Nullable ArrayList<String> optTargetAuthorities,
										   @Nullable ArrayList<String> optFilterUUIDs, @Nullable ArrayList<String> optFilterTargets) {
		NewsFragment f = new NewsFragment();
		Bundle args = new Bundle();
		if (optColorInt != null) {
			args.putInt(EXTRA_COLOR_INT, optColorInt);
			f.colorInt = optColorInt;
		}
		if (!TextUtils.isEmpty(optSubtitle)) {
			args.putString(EXTRA_SUB_TITLE, optSubtitle);
			f.subTitle = optSubtitle;
		}
		if (CollectionUtils.getSize(optTargetAuthorities) > 0) {
			args.putStringArrayList(EXTRA_FILTER_TARGET_AUTHORITIES, optTargetAuthorities);
			f.targetAuthorities = optTargetAuthorities;
		}
		if (CollectionUtils.getSize(optFilterUUIDs) > 0) {
			args.putStringArrayList(EXTRA_FILTER_UUIDS, optFilterUUIDs);
			f.filterUUIDs = optFilterUUIDs;
		}
		if (CollectionUtils.getSize(optFilterTargets) > 0) {
			args.putStringArrayList(EXTRA_FILTER_TARGETS, optFilterTargets);
			f.filterTargets = optFilterTargets;
		}
		f.setArguments(args);
		return f;
	}

	private Integer colorInt;
	private String subTitle;
	private ArrayList<String> targetAuthorities;
	private ArrayList<String> filterUUIDs;
	private ArrayList<String> filterTargets;
	private CharSequence emptyText = null;
	private NewsAdapter adapter;
	private ListViewSwipeRefreshLayout swipeRefreshLayout;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	private void initAdapters(Activity activity) {
		this.adapter = new NewsAdapter(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	private void restoreInstanceState(Bundle... bundles) {
		Integer newColorInt = BundleUtils.getInt(EXTRA_COLOR_INT, bundles);
		if (newColorInt != null) {
			this.colorInt = newColorInt;
		}
		String newSubtitle = BundleUtils.getString(EXTRA_SUB_TITLE, bundles);
		if (!TextUtils.isEmpty(newSubtitle)) {
			this.subTitle = newSubtitle;
		}
		ArrayList<String> newTargetAuthorities = BundleUtils.getStringArrayList(EXTRA_FILTER_TARGET_AUTHORITIES, bundles);
		if (CollectionUtils.getSize(newTargetAuthorities) > 0) {
			this.targetAuthorities = newTargetAuthorities;
		}
		ArrayList<String> newFilterUUIDs = BundleUtils.getStringArrayList(EXTRA_FILTER_UUIDS, bundles);
		if (CollectionUtils.getSize(newFilterUUIDs) > 0) {
			this.filterUUIDs = newFilterUUIDs;
		}
		ArrayList<String> newFilterTargets = BundleUtils.getStringArrayList(EXTRA_FILTER_TARGETS, bundles);
		if (CollectionUtils.getSize(newFilterTargets) > 0) {
			this.filterTargets = newFilterTargets;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.colorInt != null) {
			outState.putInt(EXTRA_COLOR_INT, this.colorInt);
		}
		if (!TextUtils.isEmpty(this.subTitle)) {
			outState.putString(EXTRA_SUB_TITLE, this.subTitle);
		}
		if (CollectionUtils.getSize(this.targetAuthorities) > 0) {
			outState.putStringArrayList(EXTRA_FILTER_TARGET_AUTHORITIES, this.targetAuthorities);
		}
		if (CollectionUtils.getSize(this.filterUUIDs) > 0) {
			outState.putStringArrayList(EXTRA_FILTER_UUIDS, this.filterUUIDs);
		}
		if (CollectionUtils.getSize(this.filterTargets) > 0) {
			outState.putStringArrayList(EXTRA_FILTER_TARGETS, this.filterTargets);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_news, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(() -> {
					if (NewsFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
		}
		if (!this.adapter.isInitialized()) {
			LoaderUtils.restartLoader(this, NEWS_LOADER, null, this);
		}
		this.adapter.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		this.adapter.onPause();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (this.swipeRefreshLayout != null) {
			this.swipeRefreshLayout.setOnRefreshListener(null);
			this.swipeRefreshLayout = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.adapter.onDestroy();
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		LoaderUtils.restartLoader(this, NEWS_LOADER, null, this);
		this.modulesUpdated = false; // processed
	}

	private static final int NEWS_LOADER = 0;

	@NonNull
	@Override
	public Loader<ArrayList<News>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case NEWS_LOADER:
			return new NewsLoader(getContext(), this.targetAuthorities, this.filterUUIDs, this.filterTargets);
		default:
			CrashUtils.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<ArrayList<News>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@Override
	public void onLoadFinished(@NonNull Loader<ArrayList<News>> loader, ArrayList<News> data) {
		this.emptyText = getString(R.string.no_news);
		this.adapter.setNews(data);
		switchView(getView());
	}

	@Override
	public void onRefresh() {
		initiateRefresh();
	}

	private boolean initiateRefresh() {
		if (this.adapter != null) {
			this.adapter.clear();
		}
		switchView(getView());
		LoaderUtils.restartLoader(this, NEWS_LOADER, null, this);
		setSwipeRefreshLayoutRefreshing(false);
		return true;
	}

	public void setSwipeRefreshLayoutRefreshing(boolean refreshing) {
		if (this.swipeRefreshLayout != null) {
			if (refreshing) {
				if (!this.swipeRefreshLayout.isRefreshing()) {
					this.swipeRefreshLayout.setRefreshing(true);
				}
			} else {
				this.swipeRefreshLayout.setRefreshing(false);
			}
		}
	}

	private void setupView(@NonNull View view) {
		this.swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
		this.swipeRefreshLayout.setColorSchemeColors(ThemeUtils.resolveColorAttribute(view.getContext(), R.attr.colorAccent));
		this.swipeRefreshLayout.setOnRefreshListener(this);
		inflateList(view);
		switchView(view);
		linkAdapterWithListView(view);
	}

	private void linkAdapterWithListView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		View listView = view.findViewById(R.id.list);
		if (listView != null) {
			((AbsListView) listView).setAdapter(this.adapter);
			((AbsListView) listView).setOnItemClickListener(this.adapter);
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
		inflateList(view);
		view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
			if (this.swipeRefreshLayout != null) {
				this.swipeRefreshLayout.setListViewWR(view.findViewById(R.id.list));
			}
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		if (this.swipeRefreshLayout != null) {
			this.swipeRefreshLayout.setLoadingViewWR(view.findViewById(R.id.loading));
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
			if (this.swipeRefreshLayout != null) {
				this.swipeRefreshLayout.setEmptyViewWR(view.findViewById(R.id.empty));
			}
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.news);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		if (!TextUtils.isEmpty(this.subTitle)) {
			return this.subTitle;
		}
		return super.getABSubtitle(context);
	}

	@Override
	public Integer getABBgColor(Context context) {
		if (this.colorInt != null) {
			return this.colorInt;
		}
		return super.getABBgColor(context);
	}

	private static class NewsAdapter extends MTArrayAdapter<News> implements TimeUtils.TimeChangedReceiver.TimeChangedListener,
			AdapterView.OnItemClickListener {

		private static final String LOG_TAG = NewsAdapter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private LayoutInflater layoutInflater;
		private WeakReference<Activity> activityWR;

		private ArrayList<News> news;

		private NewsAdapter(Activity activity) {
			super(activity, -1);
			setActivity(activity);
			this.layoutInflater = LayoutInflater.from(getContext());
		}

		public void setActivity(Activity activity) {
			this.activityWR = new WeakReference<>(activity);
		}

		private Activity getActivityOrNull() {
			return this.activityWR == null ? null : this.activityWR.get();
		}

		public boolean isInitialized() {
			return this.news != null;
		}

		private void resetNowToTheMinute() {
			notifyDataSetChanged();
		}

		@Override
		public void onTimeChanged() {
			resetNowToTheMinute();
		}

		private final TimeUtils.TimeChangedReceiver timeChangedReceiver = new TimeUtils.TimeChangedReceiver(this);

		private boolean timeChangedReceiverEnabled = false;

		private void enableTimeChangedReceiver() {
			if (!this.timeChangedReceiverEnabled) {
				getContext().registerReceiver(timeChangedReceiver, TimeUtils.TIME_CHANGED_INTENT_FILTER);
				this.timeChangedReceiverEnabled = true;
			}
		}

		private void disableTimeChangeddReceiver() {
			if (this.timeChangedReceiverEnabled) {
				getContext().unregisterReceiver(this.timeChangedReceiver);
				this.timeChangedReceiverEnabled = false;
			}
		}

		public void setNews(ArrayList<News> news) {
			this.news = news;
			notifyDataSetChanged();
		}

		@Override
		public void clear() {
			if (this.news != null) {
				this.news.clear();
				this.news = null; // not initialized
			}
			super.clear();
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
			MTOnItemClickListener.onItemClickS(parent, view, position, id, new MTOnItemClickListener() {
				@Override
				public void onItemClickMT(AdapterView<?> parent, View view, int position, long id) {
					News news = getItem(position);
					if (news == null) {
						return;
					}
					Activity activity = getActivityOrNull();
					if (activity == null) {
						return;
					}
					((MainActivity) activity).addFragmentToStack(NewsDetailsFragment.newInstance(news.getUUID(), news.getAuthority(), news));
				}
			});
		}

		@Override
		public int getCount() {
			return CollectionUtils.getSize(this.news);
		}

		public void onPause() {
			disableTimeChangeddReceiver();
		}

		public void onResume() {
			enableTimeChangedReceiver();
		}

		public void onDestroy() {
			disableTimeChangeddReceiver();
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_news_base, parent, false);
				NewsViewHolder holder = new NewsViewHolder();
				holder.newsTv = convertView.findViewById(R.id.newsText);
				holder.authorTv = convertView.findViewById(R.id.author);
				holder.dateTv = convertView.findViewById(R.id.date);
				convertView.setTag(holder);
			}
			NewsViewHolder holder = (NewsViewHolder) convertView.getTag();
			News news = getItem(position);
			holder.authorTv.setText(getContext().getString(R.string.news_shared_on_and_author_and_source, news.getAuthorOneLine(), news.getSourceLabel()));
			if (news.hasColor()
					&& (!ColorUtils.isDarkTheme(getContext())
					|| !ColorUtils.isTooDarkForDarkTheme(news.getColorInt()))) {
				holder.authorTv.setTextColor(news.getColorInt());
			} else {
				holder.authorTv.setTextColor(ColorUtils.getTextColorSecondary(getContext()));
			}
			holder.dateTv.setText(TimeUtils.formatRelativeTime(getContext(), news.getCreatedAtInMs()));
			holder.newsTv.setText(news.getText());
			if (news.hasColor()) {
				holder.newsTv.setLinkTextColor(news.getColorInt());
			} else {
				holder.newsTv.setLinkTextColor(ColorUtils.getTextColorPrimary(getContext()));
			}
			return convertView;
		}

		@Override
		public News getItem(int position) {
			return this.news == null ? null : this.news.get(position);
		}

		private static final class NewsViewHolder {
			private TextView newsTv;
			private TextView authorTv;
			private TextView dateTv;
		}
	}
}
