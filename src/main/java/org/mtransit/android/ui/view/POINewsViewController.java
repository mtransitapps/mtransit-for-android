package org.mtransit.android.ui.view;

import android.content.Context;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import org.mtransit.android.R;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.databinding.LayoutPoiNewsBinding;
import org.mtransit.android.util.UITimeUtils;

import java.util.List;

public class POINewsViewController implements MTLog.Loggable {

	private static final String LOG_TAG = POINewsViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static ViewBinding getLayoutViewBinding(@NonNull ViewStub viewStub) {
		viewStub.setLayoutResource(getLayoutResId());
		return LayoutPoiNewsBinding.bind(viewStub.inflate());
	}

	@LayoutRes
	public static int getLayoutResId() {
		return R.layout.layout_poi_news;
	}

	private static void initViewHolder(@NonNull View convertView) {
		NewsViewHolder holder = new NewsViewHolder();
		holder.layout = convertView;
		holder.newsTv = convertView.findViewById(R.id.newsText);
		holder.authorTv = convertView.findViewById(R.id.author);
		holder.dateTv = convertView.findViewById(R.id.date);
		convertView.setTag(holder);
	}

	public static void updateView(@Nullable View view, @Nullable List<News> news) {
		updateView(view,
				news == null || news.size() == 0 ? null : news.get(0)
		);
	}

	public static void updateView(@Nullable View view, @Nullable News newsArticle) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof NewsViewHolder)) {
			initViewHolder(view);
		}
		NewsViewHolder newsViewHolder = (NewsViewHolder) view.getTag();
		updateView(view.getContext(), newsViewHolder, newsArticle);
	}

	private static void updateView(@NonNull Context context, @Nullable NewsViewHolder newsViewHolder, @Nullable News newsArticle) {
		if (newsArticle == null || newsViewHolder == null) {
			if (newsViewHolder != null) {
				newsViewHolder.layout.setVisibility(View.GONE);
			}
			MTLog.d(LOG_TAG, "updateView() > SKIP (no news article or no view holder)");
			return;
		}
		updateNewsView(context, newsViewHolder, newsArticle);
	}

	private static void updateNewsView(@NonNull Context context, @Nullable NewsViewHolder holder, @Nullable News newsArticle) {
		if (holder == null) {
			MTLog.d(LOG_TAG, "updateNewsView() > SKIP (no view holder)");
			return;
		}
		if (newsArticle == null) {
			MTLog.d(LOG_TAG, "updateNewsView() > SKIP (no news article)");
			holder.layout.setVisibility(View.GONE);
			return;
		}
		holder.authorTv.setText(context.getString(R.string.news_shared_on_and_author_and_source, newsArticle.getAuthorOneLine(), newsArticle.getSourceLabel()));
		if (newsArticle.hasColor()) {
			holder.authorTv.setTextColor(ColorUtils.adaptColorToTheme(context, newsArticle.getColorInt()));
		} else {
			holder.authorTv.setTextColor(ColorUtils.getTextColorSecondary(context));
		}
		holder.dateTv.setText(UITimeUtils.formatRelativeTime(newsArticle.getCreatedAtInMs()), TextView.BufferType.SPANNABLE);
		holder.newsTv.setText(newsArticle.getText());
		if (newsArticle.hasColor()) {
			holder.newsTv.setLinkTextColor(newsArticle.getColorInt());
		} else {
			holder.newsTv.setLinkTextColor(ColorUtils.getTextColorPrimary(context));
		}
		holder.layout.setVisibility(View.VISIBLE);
	}

	private static final class NewsViewHolder {
		View layout;
		TextView newsTv;
		TextView authorTv;
		TextView dateTv;
	}
}
