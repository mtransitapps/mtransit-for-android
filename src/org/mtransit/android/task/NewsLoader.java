package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.provider.NewsProviderContract;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.NewsProviderProperties;

import android.content.Context;
import android.text.TextUtils;

public class NewsLoader extends MTAsyncTaskLoaderV4<ArrayList<News>> {

	private static final String TAG = NewsLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<News> news;
	private String targetUUID;

	public NewsLoader(Context context, String optTargetUUID) {
		super(context);
		this.targetUUID = optTargetUUID;
	}

	@Override
	public ArrayList<News> loadInBackgroundMT() {
		if (this.news != null) {
			return this.news;
		}
		this.news = new ArrayList<News>();
		Collection<NewsProviderProperties> newsProviders;
		if (TextUtils.isEmpty(this.targetUUID)) {
			newsProviders = DataSourceProvider.get(getContext()).getAllNewsProvider();
		} else {
			newsProviders = DataSourceProvider.get(getContext()).getTargetAuthorityNewsProviders(this.targetUUID);
		}
		if (CollectionUtils.getSize(newsProviders) == 0) {
			return this.news;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(newsProviders.size()));
		ArrayList<Future<ArrayList<News>>> taskList = new ArrayList<Future<ArrayList<News>>>();
		for (NewsProviderProperties newsProvider : newsProviders) {
			taskList.add(executor.submit(new FindNewsTask(getContext(), newsProvider.getAuthority())));
		}
		HashSet<String> newsUUIDs = new HashSet<String>();
		for (Future<ArrayList<News>> future : taskList) {
			try {
				ArrayList<News> agencyNews = future.get();
				for (News aNews : agencyNews) {
					if (newsUUIDs.contains(aNews.getUUID())) {
						continue;
					}
					this.news.add(aNews);
					newsUUIDs.add(aNews.getUUID());
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while loading in background!");
			}
		}
		executor.shutdown();
		CollectionUtils.sort(this.news, News.NEWS_COMPARATOR);
		return this.news;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (this.news != null) {
			deliverResult(this.news);
		}
		if (this.news == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(ArrayList<News> data) {
		this.news = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	private static class FindNewsTask extends MTCallable<ArrayList<News>> {

		private static final String TAG = NewsLoader.class.getSimpleName() + ">" + FindNewsTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private Context context;
		private String authority;

		public FindNewsTask(Context context, String authority) {
			this.context = context;
			this.authority = authority;
		}

		@Override
		public ArrayList<News> callMT() throws Exception {
			NewsProviderContract.Filter newsFilter = new NewsProviderContract.Filter();
			return DataSourceManager.findNews(this.context, this.authority, newsFilter);
		}
	}
}
