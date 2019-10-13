package org.mtransit.android.ui.view;

import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.News;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

public class POINewsViewController implements MTLog.Loggable {

	private static final String TAG = POINewsViewController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@LayoutRes
	@Nullable
	public static int getLayoutResId() {
		return R.layout.layout_poi_news;
	}

	public static void initViewHolder(View convertView) {
		NewsViewHolder holder = new NewsViewHolder();
		holder.layout = convertView;
		holder.newsTv = convertView.findViewById(R.id.newsText);
		holder.authorTv = convertView.findViewById(R.id.author);
		holder.dateTv = convertView.findViewById(R.id.date);
		convertView.setTag(holder);
	}

	public static void updateView(Context context, View view, ArrayList<News> news) {
		updateView(context, view, news == null || news.size() == 0 ? null : news.get(0));
	}

	public static void updateView(Context context, View view, News news) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof NewsViewHolder)) {
			initViewHolder(view);
		}
		NewsViewHolder newsViewHolder = (NewsViewHolder) view.getTag();
		updateView(context, newsViewHolder, news);
	}

	private static void updateView(Context context, NewsViewHolder newsViewHolder, News news) {
		if (news == null || newsViewHolder == null) {
			if (newsViewHolder != null) {
				newsViewHolder.layout.setVisibility(View.GONE);
			}
			return;
		}
		updateNewsView(context, newsViewHolder, news);
	}

	private static void updateNewsView(Context context, NewsViewHolder holder, News news) {
		if (holder != null) {
			if (news != null) {
				holder.authorTv.setText(context.getString(R.string.news_shared_on_and_author_and_source, news.getAuthorOneLine(), news.getSourceLabel()));
				if (news.hasColor()) {
					holder.authorTv.setTextColor(news.getColorInt());
				} else {
					holder.authorTv.setTextColor(ColorUtils.getTextColorSecondary(context));
				}
				holder.dateTv.setText(TimeUtils.formatRelativeTime(context, news.getCreatedAtInMs()));
				holder.newsTv.setText(news.getText());
				if (news.hasColor()) {
					holder.newsTv.setLinkTextColor(news.getColorInt());
				} else {
					holder.newsTv.setLinkTextColor(ColorUtils.getTextColorPrimary(context));
				}
				holder.layout.setVisibility(View.VISIBLE);
			} else {
				holder.layout.setVisibility(View.GONE);
			}
		}
	}

	private static final class NewsViewHolder {
		View layout;
		TextView newsTv;
		TextView authorTv;
		TextView dateTv;
	}
}
