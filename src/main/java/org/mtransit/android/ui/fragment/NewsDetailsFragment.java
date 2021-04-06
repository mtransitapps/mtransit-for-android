package org.mtransit.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.provider.NewsProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTOnClickListener;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.android.util.UITimeUtils;

public class NewsDetailsFragment extends ABFragment implements UITimeUtils.TimeChangedReceiver.TimeChangedListener, LinkUtils.OnUrlClickListener {

	private static final String TAG = NewsDetailsFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "News";

	@NonNull
	@Override
	public String getScreenName() {
		if (!TextUtils.isEmpty(this.uuid)) {
			return TRACKING_SCREEN_NAME + "/" + this.uuid;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_NEWS_UUID = "extra_news_uuid";

	@NonNull
	public static NewsDetailsFragment newInstance(@NonNull String uuid, @NonNull String authority, @Nullable News optNews) {
		NewsDetailsFragment f = new NewsDetailsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
			f.authority = authority;
		}
		args.putString(EXTRA_NEWS_UUID, uuid);
		if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
			f.uuid = uuid;
			f.news = optNews;
		}
		f.setArguments(args);
		return f;
	}

	@Nullable
	private String authority;
	@Nullable
	private String uuid;
	@Nullable
	private News news;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public NewsDetailsFragment() {
		super();
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
		this.dataSourcesRepository.readingAllNewsProvidersDistinct().observe(this, newNewsProviders -> {
			initNewsAsync(true); // force to check if news article still exists
		});
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (newAuthority != null && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
		}
		String newUUID = BundleUtils.getString(EXTRA_NEWS_UUID, bundles);
		if (newUUID != null && !newUUID.equals(this.uuid)) {
			this.uuid = newUUID;
			resetNews();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (!TextUtils.isEmpty(this.uuid)) {
			outState.putString(EXTRA_NEWS_UUID, this.uuid);
		}
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AUTHORITY, this.authority);
		}
		super.onSaveInstanceState(outState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_news_details, container, false);
		setupView(view);
		return view;
	}

	private void setupView(@SuppressWarnings("unused") View view) {
		// DO NOTHING
	}

	private boolean hasNews() {
		if (this.news == null) {
			initNewsAsync(false);
			return false;
		}
		return true;
	}

	private void initNewsAsync(boolean force) {
		if (this.loadNewsTask != null && this.loadNewsTask.getStatus() == LoadNewsTask.Status.RUNNING) {
			if (!force) {
				MTLog.d(this, "initNewsAsync() > SKIP (already running)");
				return;
			}
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			MTLog.d(this, "initNewsAsync() > SKIP (no UUID or no authority)");
			return;
		}
		this.loadNewsTask = new LoadNewsTask(this, force);
		TaskUtils.execute(this.loadNewsTask);
	}

	@Nullable
	private LoadNewsTask loadNewsTask = null;

	@SuppressWarnings("deprecation")
	private static class LoadNewsTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, NewsDetailsFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return NewsDetailsFragment.class.getSimpleName() + ">" + LoadNewsTask.class.getSimpleName();
		}

		private final boolean force;

		LoadNewsTask(NewsDetailsFragment newsDetailsFragment, boolean force) {
			super(newsDetailsFragment);
			this.force = force;
		}

		@WorkerThread
		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull NewsDetailsFragment newsDetailsFragment, Void... params) {
			return newsDetailsFragment.initNewsSync(this.force);
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull NewsDetailsFragment newsDetailsFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				newsDetailsFragment.applyNewNews();
			}
		}
	}

	private void resetNews() {
		this.news = null;
	}

	@Nullable
	private News getNewsOrNull() {
		if (!hasNews()) {
			return null;
		}
		return this.news;
	}

	@WorkerThread
	private boolean initNewsSync(boolean force) {
		if (!force && this.news != null) {
			MTLog.d(this, "initNewsSync() > SKIP (news already loaded)");
			return false;
		}
		if (this.uuid != null && this.authority != null) {
			final News newNewsArticle = DataSourceManager.findANews(requireContext(), this.authority, NewsProviderContract.Filter.getNewUUIDFilter(this.uuid));
			if (newNewsArticle == null) {
				onDataSourceRemoved();
			}
			this.news = newNewsArticle;
		}
		return this.news != null;
	}

	private void onDataSourceRemoved() {
		final MainActivity activity = (MainActivity) getActivity();
		if (activity == null) {
			return;
		}
		if (activity.isMTResumed()) {
			activity.popFragmentFromStack(this); // close this fragment
		}
	}

	private void applyNewNews() {
		if (this.news == null) {
			MTLog.d(this, "applyNewNews() > SKIP (no news)");
			return;
		}
		updateNewsView();
		if (getAbController() != null) {
			getAbController().setABBgColor(this, getABBgColor(getContext()), false);
			getAbController().setABTitle(this, getABTitle(getContext()), false);
			getAbController().setABSubtitle(this, getABSubtitle(getContext()), false);
			getAbController().setABReady(this, isABReady(), true);
		}
	}

	private void updateNewsView() {
		News news = getNewsOrNull();
		if (news == null) {
			MTLog.d(this, "updateNewsView() > SKIP (no news)");
			return;
		}
		View view = getView();
		if (view == null) {
			MTLog.d(this, "updateNewsView() > SKIP (no view)");
			return;
		}
		TextView newsTv = view.findViewById(R.id.newsText);
		newsTv.setText(LinkUtils.linkifyHtml(news.getTextHTML(), true), TextView.BufferType.SPANNABLE);
		newsTv.setMovementMethod(LinkUtils.LinkMovementMethodInterceptor.getInstance(this));
		if (news.hasColor()) {
			newsTv.setLinkTextColor(ColorUtils.adaptColorToTheme(view.getContext(), news.getColorInt()));
		} else {
			newsTv.setLinkTextColor(ColorUtils.getTextColorPrimary(view.getContext()));
		}
		TextView dateTv = view.findViewById(R.id.date);
		dateTv.setText(UITimeUtils.formatRelativeTime(news.getCreatedAtInMs()), TextView.BufferType.SPANNABLE);
		final String newWebURL = TextUtils.isEmpty(news.getWebURL()) ? news.getAuthorProfileURL() : news.getWebURL();
		dateTv.setOnClickListener(new MTOnClickListener() {
			@Override
			public void onClickMT(@NonNull View view) {
				LinkUtils.open(requireActivity(), newWebURL, getString(R.string.web_browser), true);
			}
		});
	}

	@Override
	public boolean onURLClick(@NonNull String url) {
		return LinkUtils.open(requireActivity(), url, getString(R.string.web_browser), true);
	}

	@Override
	public void onTimeChanged() {
		resetNowToTheMinute();
	}

	private void resetNowToTheMinute() {
		updateNewsView();
	}

	private final UITimeUtils.TimeChangedReceiver timeChangedReceiver = new UITimeUtils.TimeChangedReceiver(this);

	private boolean timeChangedReceiverEnabled = false;

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			if (getActivity() != null) {
				getActivity().registerReceiver(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER);
			}
			this.timeChangedReceiverEnabled = true;
		}
	}

	private void disableTimeChangedReceiver() {
		if (this.timeChangedReceiverEnabled) {
			if (getActivity() != null) {
				getActivity().unregisterReceiver(this.timeChangedReceiver);
			}
			this.timeChangedReceiverEnabled = false;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateNewsView();
		enableTimeChangedReceiver();
	}

	@Override
	public void onPause() {
		super.onPause();
		disableTimeChangedReceiver();
	}

	@Nullable
	@ColorInt
	@Override
	public Integer getABBgColor(@Nullable Context context) {
		News news = getNewsOrNull();
		if (news != null && news.hasColor()) {
			return news.getColorInt();
		}
		return super.getABBgColor(context);
	}

	@Nullable
	@Override
	public CharSequence getABTitle(@Nullable Context context) {
		News news = getNewsOrNull();
		if (news != null) {
			return news.getAuthorOneLine();
		}
		return super.getABTitle(context);
	}

	@Nullable
	@Override
	public CharSequence getABSubtitle(@Nullable Context context) {
		News news = getNewsOrNull();
		if (news != null) {
			return news.getSourceLabel();
		}
		return super.getABSubtitle(context);
	}

	@Override
	public boolean isABReady() {
		return hasNews();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TaskUtils.cancelQuietly(this.loadNewsTask, true);
	}
}
