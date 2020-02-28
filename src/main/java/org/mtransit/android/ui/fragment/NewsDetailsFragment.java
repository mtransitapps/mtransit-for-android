package org.mtransit.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.provider.NewsProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTOnClickListener;
import org.mtransit.android.util.LinkUtils;

public class NewsDetailsFragment extends ABFragment implements TimeUtils.TimeChangedReceiver.TimeChangedListener, LinkUtils.OnUrlClickListener {

	private static final String TAG = NewsDetailsFragment.class.getSimpleName();

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

	public static NewsDetailsFragment newInstance(String uuid, String authority, @Nullable News optNews) {
		NewsDetailsFragment f = new NewsDetailsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		args.putString(EXTRA_NEWS_UUID, uuid);
		f.uuid = uuid;
		f.news = optNews;
		f.setArguments(args);
		return f;
	}

	private String authority;
	private String uuid;
	private News news;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
		}
		String newUUID = BundleUtils.getString(EXTRA_NEWS_UUID, bundles);
		if (!TextUtils.isEmpty(newUUID) && !newUUID.equals(this.uuid)) {
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

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_news_details, container, false);
		setupView(view);
		return view;
	}

	private void setupView(View view) {
	}

	private boolean hasNews() {
		if (this.news == null) {
			initNewsAsync();
			return false;
		}
		return true;
	}

	private void initNewsAsync() {
		if (this.loadNewsTask != null && this.loadNewsTask.getStatus() == LoadNewsTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadNewsTask = new LoadNewsTask(this);
		TaskUtils.execute(this.loadNewsTask);
	}

	private LoadNewsTask loadNewsTask = null;

	private static class LoadNewsTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, NewsDetailsFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return NewsDetailsFragment.class.getSimpleName() + ">" + LoadNewsTask.class.getSimpleName();
		}

		LoadNewsTask(NewsDetailsFragment newsDetailsFragment) {
			super(newsDetailsFragment);
		}

		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull NewsDetailsFragment newsDetailsFragment, Void... params) {
			return newsDetailsFragment.initNewsSync();
		}

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

	private News getNewsOrNull() {
		if (!hasNews()) {
			return null;
		}
		return this.news;
	}

	private boolean initNewsSync() {
		if (this.news != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.uuid) && !TextUtils.isEmpty(this.authority)) {
			this.news = DataSourceManager.findANews(requireContext(), this.authority, NewsProviderContract.Filter.getNewUUIDFilter(this.uuid));
		}
		return this.news != null;
	}

	private void applyNewNews() {
		if (this.news == null) {
			return;
		}
		updateNewsView();
		getAbController().setABBgColor(this, getABBgColor(getContext()), false);
		getAbController().setABTitle(this, getABTitle(getContext()), false);
		getAbController().setABSubtitle(this, getABSubtitle(getContext()), false);
		getAbController().setABReady(this, isABReady(), true);
	}

	private void updateNewsView() {
		News news = getNewsOrNull();
		if (news == null) {
			return;
		}
		View view = getView();
		if (view == null) {
			return;
		}
		TextView newsTv = view.findViewById(R.id.newsText);
		newsTv.setText(LinkUtils.linkifyHtml(news.getTextHTML(), true));
		newsTv.setMovementMethod(LinkUtils.LinkMovementMethodInterceptop.getInstance(this));
		if (news.hasColor()) {
			newsTv.setLinkTextColor(ColorUtils.adaptColorToTheme(view.getContext(), news.getColorInt()));
		} else {
			newsTv.setLinkTextColor(ColorUtils.getTextColorPrimary(view.getContext()));
		}
		TextView dateTv = view.findViewById(R.id.date);
		dateTv.setText(TimeUtils.formatRelativeTime(view.getContext(), news.getCreatedAtInMs()));
		final String newWebURL = TextUtils.isEmpty(news.getWebURL()) ? news.getAuthorProfileURL() : news.getWebURL();
		dateTv.setOnClickListener(new MTOnClickListener() {
			@Override
			public void onClickMT(View view) {
				LinkUtils.open(getActivity(), newWebURL, getString(R.string.web_browser), true);
			}
		});
	}

	@Override
	public boolean onURLClick(String url) {
		return LinkUtils.open(getActivity(), url, getString(R.string.web_browser), true);
	}

	@Override
	public void onTimeChanged() {
		resetNowToTheMinute();
	}

	private void resetNowToTheMinute() {
		updateNewsView();
	}

	private final TimeUtils.TimeChangedReceiver timeChangedReceiver = new TimeUtils.TimeChangedReceiver(this);

	private boolean timeChangedReceiverEnabled = false;

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			if (getActivity() != null) {
				getActivity().registerReceiver(timeChangedReceiver, TimeUtils.TIME_CHANGED_INTENT_FILTER);
			}
			this.timeChangedReceiverEnabled = true;
		}
	}

	private void disableTimeChangeddReceiver() {
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
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(() -> {
					if (NewsDetailsFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
		}
		updateNewsView();
		enableTimeChangedReceiver();
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		MainActivity activity = (MainActivity) getActivity();
		if (activity == null) {
			return;
		}
		News newNews = DataSourceManager.findANews(requireContext(), this.authority, NewsProviderContract.Filter.getNewUUIDFilter(this.uuid));
		if (newNews == null) {
			if (activity.isMTResumed()) {
				activity.popFragmentFromStack(this); // close this fragment
				this.modulesUpdated = false; // processed
			}
		} else {
			this.modulesUpdated = false; // nothing to do
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		disableTimeChangeddReceiver();
	}

	@ColorInt
	@Override
	public Integer getABBgColor(Context context) {
		News news = getNewsOrNull();
		if (news != null && news.hasColor()) {
			return news.getColorInt();
		}
		return super.getABBgColor(context);
	}

	@Override
	public CharSequence getABTitle(Context context) {
		News news = getNewsOrNull();
		if (news != null) {
			return news.getAuthorOneLine();
		}
		return super.getABTitle(context);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
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
