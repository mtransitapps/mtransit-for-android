package org.mtransit.android.util;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.WebBrowserFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

public final class LinkUtils implements MTLog.Loggable {

	private static final String TAG = LinkUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static boolean open(Activity activity, String url, String label, boolean www) {
		if (TextUtils.isEmpty(url)) {
			return false;
		}
		if (www) {
			boolean useInternalWebBrowser = PreferenceUtils.getPrefDefault(activity, PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER,
					PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT);
			if (useInternalWebBrowser) {
				((MainActivity) activity).addFragmentToStack(WebBrowserFragment.newInstance(url));
				return true;
			}
		}
		return org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), label);
	}

	public static boolean open(Activity activity, Uri uri, String label, boolean www) {
		return org.mtransit.android.commons.LinkUtils.open(activity, uri, label);
	}

	public static boolean open(Activity activity, Intent intent, String label, boolean www) {
		return org.mtransit.android.commons.LinkUtils.open(activity, intent, label);
	}

	public static class LinkMovementMethodInterceptop extends LinkMovementMethod implements MTLog.Loggable {

		private static final String TAG = LinkMovementMethodInterceptop.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static LinkMovementMethodInterceptop sInstance;

		public static LinkMovementMethodInterceptop getInstance(OnUrlClickListener onUrlClickListener) {
			if (sInstance == null) {
				sInstance = new LinkMovementMethodInterceptop(onUrlClickListener);
			} else {
				sInstance.setOnUrlClickListener(onUrlClickListener);
			}
			return sInstance;
		}

		private WeakReference<OnUrlClickListener> onUrlClickListenerWR;

		public LinkMovementMethodInterceptop(OnUrlClickListener onUrlClickListener) {
			setOnUrlClickListener(onUrlClickListener);
		}

		private void setOnUrlClickListener(OnUrlClickListener onUrlClickListener) {
			this.onUrlClickListenerWR = new WeakReference<OnUrlClickListener>(onUrlClickListener);
		}

		@Override
		public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
			int action = event.getAction();
			if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
				int x = (int) event.getX();
				int y = (int) event.getY();
				x -= widget.getTotalPaddingLeft();
				y -= widget.getTotalPaddingTop();
				x += widget.getScrollX();
				y += widget.getScrollY();
				Layout layout = widget.getLayout();
				int line = layout.getLineForVertical(y);
				int off = layout.getOffsetForHorizontal(line, x);
				ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
				if (link.length != 0) {
					if (action == MotionEvent.ACTION_UP) {
						OnUrlClickListener listener = this.onUrlClickListenerWR == null ? null : this.onUrlClickListenerWR.get();
						if (listener != null) {
							if (link[0] instanceof URLSpan) {
								String url = ((URLSpan) link[0]).getURL();
								if (listener.onURLClick(url)) {
									return true;
								}
							}
						}

					}
				}
			}
			return super.onTouchEvent(widget, buffer, event);
		}
	}

	public static interface OnUrlClickListener {
		boolean onURLClick(String url);
	}
}
