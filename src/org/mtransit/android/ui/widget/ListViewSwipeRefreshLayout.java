package org.mtransit.android.ui.widget;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.MTLog;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;

public class ListViewSwipeRefreshLayout extends SwipeRefreshLayout implements MTLog.Loggable {

	private static final String TAG = ListViewSwipeRefreshLayout.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private WeakReference<AbsListView> listViewWR;
	private WeakReference<View> loadingViewWR;
	private WeakReference<View> emptyViewWR;

	public ListViewSwipeRefreshLayout(Context context) {
		super(context);
	}

	public ListViewSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setListViewWR(AbsListView listView) {
		this.listViewWR = new WeakReference<AbsListView>(listView);
	}

	public void setLoadingViewWR(View loadingView) {
		this.loadingViewWR = new WeakReference<View>(loadingView);
	}

	public void setEmptyViewWR(View emptyView) {
		this.emptyViewWR = new WeakReference<View>(emptyView);
	}

	@Override
	public boolean canChildScrollUp() {
		final AbsListView listView = this.listViewWR == null ? null : this.listViewWR.get();
		if (listView != null && listView.getVisibility() == View.VISIBLE) {
			final boolean canListViewScrollUp = canListViewScrollUp(listView);
			return canListViewScrollUp;
		}
		final View loadingView = this.loadingViewWR == null ? null : this.loadingViewWR.get();
		if (loadingView != null && loadingView.getVisibility() == View.VISIBLE) {
			final boolean canLoadingViewScrollUp = true;
			return canLoadingViewScrollUp;
		}
		final View emptyView = this.emptyViewWR == null ? null : this.emptyViewWR.get();
		if (emptyView != null && emptyView.getVisibility() == View.VISIBLE) {
			final boolean canEmptyViewScrollUp = canViewScrollUp(emptyView);
			return canEmptyViewScrollUp;
		}
		final boolean canChildScrollUp = super.canChildScrollUp();
		return canChildScrollUp;
	}

	private static boolean canListViewScrollUp(AbsListView listView) {
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			return ViewCompat.canScrollVertically(listView, -1);
		} else {
			return listView.getChildCount() > 0 && (listView.getFirstVisiblePosition() > 0 || listView.getChildAt(0).getTop() < listView.getPaddingTop());
		}
	}

	public boolean canViewScrollUp(View view) {
		if (android.os.Build.VERSION.SDK_INT < 14) {
			if (view instanceof AbsListView) {
				final AbsListView absListView = (AbsListView) view;
				return absListView.getChildCount() > 0
						&& (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
			} else {
				return view.getScrollY() > 0;
			}
		} else {
			return ViewCompat.canScrollVertically(view, -1);
		}
	}
}
