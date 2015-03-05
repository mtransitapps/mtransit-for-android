package org.mtransit.android.task;

import java.util.ArrayList;
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
import org.mtransit.android.commons.provider.NewsProviderContract.Filter;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.NewsProviderProperties;

import android.content.Context;

public class NewsLoader extends MTAsyncTaskLoaderV4<ArrayList<News>> {

	private static final String TAG = NewsLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<News> news;
	private ArrayList<String> targetAuthorities;
	private ArrayList<String> filterUUIDs;
	private ArrayList<String> filterTargets;

	public NewsLoader(Context context, ArrayList<String> optTargetAuthorities, ArrayList<String> optFilterUUIDs, ArrayList<String> optFilterTargets) {
		super(context);
		this.targetAuthorities = optTargetAuthorities;
		this.filterUUIDs = optFilterUUIDs;
		this.filterTargets = optFilterTargets;
	}

	@Override
	public ArrayList<News> loadInBackgroundMT() {
		if (this.news != null) {
			return this.news;
		}
		this.news = new ArrayList<News>();
		ArrayList<NewsProviderProperties> newsProviders;
		if (CollectionUtils.getSize(this.targetAuthorities) == 0) {
			newsProviders = DataSourceProvider.get(getContext()).getAllNewsProvider();
		} else {
			newsProviders = new ArrayList<NewsProviderProperties>();
			for (String targetAuthority : this.targetAuthorities) {
				newsProviders.addAll(DataSourceProvider.get(getContext()).getTargetAuthorityNewsProviders(targetAuthority));
			}
		}
		if (CollectionUtils.getSize(newsProviders) == 0) {
			MTLog.w(this, "loadInBackground() > no News provider found");
			return this.news;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(newsProviders.size()));
		ArrayList<Future<ArrayList<News>>> taskList = new ArrayList<Future<ArrayList<News>>>();
		for (NewsProviderProperties newsProvider : newsProviders) {
			taskList.add(executor.submit(new FindNewsTask(getContext(), newsProvider.getAuthority(), this.filterUUIDs, this.filterTargets)));
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
		private ArrayList<String> filterUUIDs;
		private ArrayList<String> filterTargets;

		public FindNewsTask(Context context, String authority, ArrayList<String> optFilterUUIDs, ArrayList<String> optFilterTargets) {
			this.context = context;
			this.authority = authority;
			this.filterUUIDs = optFilterUUIDs;
			this.filterTargets = optFilterTargets;
		}

		@Override
		public ArrayList<News> callMT() throws Exception {
			Filter newsFilter;
			if (CollectionUtils.getSize(this.filterUUIDs) > 0) {
				newsFilter = NewsProviderContract.Filter.getNewUUIDsFilter(this.filterUUIDs);
			} else if (CollectionUtils.getSize(this.filterTargets) > 0) {
				newsFilter = NewsProviderContract.Filter.getNewTargetsFilter(this.filterTargets);
			} else {
				newsFilter = NewsProviderContract.Filter.getNewEmptyFilter();
			}
			return DataSourceManager.findNews(this.context, this.authority, newsFilter);
		}
	}
}
