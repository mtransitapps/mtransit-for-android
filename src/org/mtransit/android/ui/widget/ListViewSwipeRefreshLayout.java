package org.mtransit.android.ui.widget;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.MTLog;

import android.content.Context;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;

public class ListViewSwipeRefreshLayout extends SwipeRefreshLayout implements MTLog.Loggable {

	private static final String TAG = ListViewSwipeRefreshLayout.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private boolean refreshEnabled = true;
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

	public void setRefreshEnabled(boolean refreshEnabled) {
		this.refreshEnabled = refreshEnabled;
	}

	@Override
	public boolean canChildScrollUp() {
		if (!this.refreshEnabled) {
			return true;
		}
		AbsListView listView = this.listViewWR == null ? null : this.listViewWR.get();
		if (listView != null && listView.getVisibility() == View.VISIBLE) {
			return canListViewScrollUp(listView);
		}
		View loadingView = this.loadingViewWR == null ? null : this.loadingViewWR.get();
		if (loadingView != null && loadingView.getVisibility() == View.VISIBLE) {
			return true;
		}
		View emptyView = this.emptyViewWR == null ? null : this.emptyViewWR.get();
		if (emptyView != null && emptyView.getVisibility() == View.VISIBLE) {
			return canViewScrollUp(emptyView);
		}
		return super.canChildScrollUp();
	}

	private static boolean canListViewScrollUp(AbsListView listView) {
		return listView.canScrollVertically(-1);
	}

	public boolean canViewScrollUp(View view) {
		return view.canScrollVertically(-1);
	}
}
