package org.mtransit.android.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.mtransit.android.commons.MTLog;

import java.lang.ref.WeakReference;

@Deprecated // use SwipeRefreshLayout w/ RecyclerView
public class ListViewSwipeRefreshLayout extends SwipeRefreshLayout implements MTLog.Loggable {

	private static final String LOG_TAG = ListViewSwipeRefreshLayout.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private boolean refreshEnabled = true;
	@Nullable
	private WeakReference<AbsListView> listViewWR;
	@Nullable
	private WeakReference<View> loadingViewWR;
	@Nullable
	private WeakReference<View> emptyViewWR;

	public ListViewSwipeRefreshLayout(@NonNull Context context) {
		super(context);
	}

	public ListViewSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public void setListViewWR(@Nullable AbsListView listView) {
		this.listViewWR = new WeakReference<>(listView);
	}

	public void setLoadingViewWR(@Nullable View loadingView) {
		this.loadingViewWR = new WeakReference<>(loadingView);
	}

	public void setEmptyViewWR(@Nullable View emptyView) {
		this.emptyViewWR = new WeakReference<>(emptyView);
	}

	public void setRefreshEnabled(boolean refreshEnabled) {
		this.refreshEnabled = refreshEnabled;
	}

	@Override
	public boolean canChildScrollUp() {
		if (!this.refreshEnabled) {
			return true;
		}
		final AbsListView listView = this.listViewWR == null ? null : this.listViewWR.get();
		if (listView != null && listView.getVisibility() == View.VISIBLE) {
			return canListViewScrollUp(listView);
		}
		final View loadingView = this.loadingViewWR == null ? null : this.loadingViewWR.get();
		if (loadingView != null && loadingView.getVisibility() == View.VISIBLE) {
			return true;
		}
		final View emptyView = this.emptyViewWR == null ? null : this.emptyViewWR.get();
		if (emptyView != null && emptyView.getVisibility() == View.VISIBLE) {
			return canViewScrollUp(emptyView);
		}
		return super.canChildScrollUp();
	}

	private static boolean canListViewScrollUp(AbsListView listView) {
		return listView.canScrollVertically(-1);
	}

	public boolean canViewScrollUp(@NonNull View view) {
		return view.canScrollVertically(-1);
	}
}
