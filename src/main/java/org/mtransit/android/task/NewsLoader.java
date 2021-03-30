package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.provider.NewsProviderContract;
import org.mtransit.android.commons.provider.NewsProviderContract.Filter;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.NewsProviderProperties;
import org.mtransit.android.datasource.DataSourcesRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES;

public class NewsLoader extends MTAsyncTaskLoaderX<ArrayList<News>> {

	private static final String LOG_TAG = NewsLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	private ArrayList<News> news;
	@Nullable
	private final ArrayList<String> targetAuthorities;
	@Nullable
	private final ArrayList<String> filterUUIDs;
	@Nullable
	private final ArrayList<String> filterTargets;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public NewsLoader(@NonNull Context context,
					  @NonNull DataSourcesRepository dataSourcesRepository,
					  @Nullable ArrayList<String> optTargetAuthorities,
					  @Nullable ArrayList<String> optFilterUUIDs,
					  @Nullable ArrayList<String> optFilterTargets) {
		super(context);
		this.dataSourcesRepository = dataSourcesRepository;
		this.targetAuthorities = optTargetAuthorities;
		this.filterUUIDs = optFilterUUIDs;
		this.filterTargets = optFilterTargets;
	}

	@NonNull
	@Override
	public ArrayList<News> loadInBackgroundMT() {
		if (this.news != null) {
			return this.news;
		}
		this.news = new ArrayList<>();
		Collection<NewsProviderProperties> newsProviders;
		if (this.targetAuthorities == null || this.targetAuthorities.isEmpty()) {
			if (F_CACHE_DATA_SOURCES) {
				newsProviders = this.dataSourcesRepository.getAllNewsProviders();
			} else {
				newsProviders = org.mtransit.android.data.DataSourceProvider.get(getContext()).getAllNewsProvider();
			}
		} else {
			newsProviders = new HashSet<>();
			for (String targetAuthority : this.targetAuthorities) {
				final Collection<NewsProviderProperties> targetAuthorityNewsProviders;
				if (F_CACHE_DATA_SOURCES) {
					targetAuthorityNewsProviders = this.dataSourcesRepository.getNewsProviders(targetAuthority);
				} else {
					targetAuthorityNewsProviders = org.mtransit.android.data.DataSourceProvider.get(getContext()).getTargetAuthorityNewsProviders(targetAuthority);
				}
				if (targetAuthorityNewsProviders != null) {
					newsProviders.addAll(targetAuthorityNewsProviders);
				}
			}
		}
		if (CollectionUtils.getSize(newsProviders) == 0) {
			MTLog.w(this, "loadInBackground() > no News provider found");
			return this.news;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(newsProviders.size()));
		ArrayList<Future<ArrayList<News>>> taskList = new ArrayList<>();
		for (NewsProviderProperties newsProvider : newsProviders) {
			taskList.add(executor.submit(new FindNewsTask(getContext(), newsProvider.getAuthority(), this.filterUUIDs, this.filterTargets)));
		}
		HashSet<String> newsUUIDs = new HashSet<>();
		for (Future<ArrayList<News>> future : taskList) {
			try {
				ArrayList<News> agencyNews = future.get();
				if (agencyNews != null) {
					for (News aNews : agencyNews) {
						if (newsUUIDs.contains(aNews.getUUID())) {
							continue;
						}
						this.news.add(aNews);
						newsUUIDs.add(aNews.getUUID());
					}
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
	public void deliverResult(@Nullable ArrayList<News> data) {
		this.news = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	private static class FindNewsTask extends MTCallable<ArrayList<News>> {

		private static final String LOG_TAG = NewsLoader.class.getSimpleName() + ">" + FindNewsTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final Context context;
		@NonNull
		private final String authority;
		@Nullable
		private final ArrayList<String> filterUUIDs;
		@Nullable
		private final ArrayList<String> filterTargets;

		FindNewsTask(@NonNull Context context,
					 @NonNull String authority,
					 @Nullable ArrayList<String> optFilterUUIDs,
					 @Nullable ArrayList<String> optFilterTargets) {
			this.context = context;
			this.authority = authority;
			this.filterUUIDs = optFilterUUIDs;
			this.filterTargets = optFilterTargets;
		}

		@Override
		public ArrayList<News> callMT() {
			final Filter newsFilter;
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
