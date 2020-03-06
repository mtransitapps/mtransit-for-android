package org.mtransit.android.ui.view;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.News;

import java.util.ArrayList;

public class POINewsViewController implements MTLog.Loggable {

	private static final String TAG = POINewsViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@LayoutRes
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

	private static void updateNewsView(@NonNull Context context, NewsViewHolder holder, @Nullable News news) {
		if (holder == null) {
			return;
		}
		if (news == null) {
			holder.layout.setVisibility(View.GONE);
			return;
		}
		holder.authorTv.setText(context.getString(R.string.news_shared_on_and_author_and_source, news.getAuthorOneLine(), news.getSourceLabel()));
		if (news.hasColor()) {
			holder.authorTv.setTextColor(ColorUtils.adaptColorToTheme(context, news.getColorInt()));
		} else {
			holder.authorTv.setTextColor(ColorUtils.getTextColorSecondary(context));
		}
		holder.dateTv.setText(TimeUtils.formatRelativeTime(context, news.getCreatedAtInMs()), TextView.BufferType.SPANNABLE);
		holder.newsTv.setText(news.getText());
		if (news.hasColor()) {
			holder.newsTv.setLinkTextColor(news.getColorInt());
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
