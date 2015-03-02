package org.mtransit.android.ui.fragment;

import java.util.Arrays;

import org.mtransit.android.R;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.provider.NewsProvider;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.ui.MainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NewsDetailsFragment extends ABFragment implements TimeUtils.TimeChangedReceiver.TimeChangedListener {

	private static final String TAG = NewsDetailsFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "News";

	@Override
	public String getScreenName() {
		if (!TextUtils.isEmpty(this.uuid)) {
			return TRACKING_SCREEN_NAME + "/" + this.uuid;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";

	public static NewsDetailsFragment newInstance(String uuid, String authority, News optNews) {
		NewsDetailsFragment f = new NewsDetailsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		args.putString(EXTRA_POI_UUID, uuid);
		f.uuid = uuid;
		f.news = optNews;
		f.setArguments(args);
		return f;
	}

	private String authority;
	private String uuid;
	private News news;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_news_details, container, false);
		setupView(view);
		return view;
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
	}

	private boolean hasNews() {
		if (this.news == null) {
			initNewsAsync();
			return false;
		}
		return true;
	}

	private void initNewsAsync() {
		if (this.loadNewsTask != null && this.loadNewsTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadNewsTask = new LoadNewsTask();
		this.loadNewsTask.execute();
	}

	private LoadNewsTask loadNewsTask = null;

	private class LoadNewsTask extends MTAsyncTask<Void, Void, Boolean> {

		@Override
		public String getLogTag() {
			return NewsDetailsFragment.this.getLogTag() + ">" + LoadNewsTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initNewsSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewNews();
			}
		}
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
			this.news = DataSourceManager.findANews(getActivity(), this.authority, new NewsProvider.NewsFilter(Arrays.asList(new String[] { this.uuid })));
		}
		return this.news != null;
	}

	private void applyNewNews() {
		if (this.news == null) {
			return;
		}
		updateNewsView();
		getAbController().setABBgColor(this, getABBgColor(getActivity()), false);
		getAbController().setABTitle(this, getABTitle(getActivity()), false);
		getAbController().setABSubtitle(this, getABSubtitle(getActivity()), false);
		getAbController().setABReady(this, isABReady(), true);
	}

	private void updateNewsView() {
		News news = getNewsOrNull();
		if (news != null) {
			View view = getView();
			if (view != null) {
				TextView newsTv = (TextView) view.findViewById(R.id.newsText);
				newsTv.setText(Html.fromHtml(this.news.getTextHTML()));
				newsTv.setMovementMethod(LinkMovementMethod.getInstance());
				if (news.hasColor()) {
					newsTv.setLinkTextColor(news.getColorInt());
				}
				TextView dateTv = (TextView) view.findViewById(R.id.date);
				dateTv.setText(TimeUtils.formatRelativeTime(getActivity(), news.getCreatedAtInMs()));
			}
		}
	}

	@Override
	public void onTimeChanged() {
		resetNowToTheMinute();
	}

	private void resetNowToTheMinute() {
		updateNewsView();
	}

	private final BroadcastReceiver timeChangedReceiver = new TimeUtils.TimeChangedReceiver(this);

	private boolean timeChangedReceiverEnabled = false;

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			getActivity().registerReceiver(timeChangedReceiver, TimeUtils.TIME_CHANGED_INTENT_FILTER);
			this.timeChangedReceiverEnabled = true;
		}
	}

	private void disableTimeChangeddReceiver() {
		if (this.timeChangedReceiverEnabled) {
			getActivity().unregisterReceiver(this.timeChangedReceiver);
			this.timeChangedReceiverEnabled = false;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			view.post(new Runnable() {
				@Override
				public void run() {
					if (NewsDetailsFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
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
		News newNews = DataSourceManager.findANews(getActivity(), this.authority, new NewsProvider.NewsFilter(Arrays.asList(new String[] { this.uuid })));
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
}
