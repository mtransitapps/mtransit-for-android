package org.mtransit.android.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.WebBrowserFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.widget.TextView;

public final class LinkUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LinkUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static CharSequence linkifyHtml(String originalText, boolean isHTML) {
		try {
			Spanned text = isHTML ? Html.fromHtml(originalText) : new SpannedString(originalText);
			URLSpan[] currentSpans = text.getSpans(0, text.length(), URLSpan.class);
			SpannableString buffer = new SpannableString(text);
			Linkify.addLinks(buffer, isHTML ? Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.MAP_ADDRESSES : Linkify.ALL);
			for (URLSpan span : currentSpans) {
				buffer.setSpan(span, text.getSpanStart(span), text.getSpanEnd(span), 0);
			}
			return buffer;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while linkify-ing '%s'!", originalText);
			return originalText;
		}
	}

	public static boolean open(Activity activity, String url, String label, boolean www) {
		if (TextUtils.isEmpty(url)) {
			return false;
		}
		if (intercept(activity, url)) {
			return true;
		}
		if (www) {
			boolean useInternalWebBrowser = PreferenceUtils.getPrefDefault(activity, //
					PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER, PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT);
			if (useInternalWebBrowser) {
				((MainActivity) activity).addFragmentToStack(WebBrowserFragment.newInstance(url));
				return true;
			}
		}
		return org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), label);
	}

	public static boolean intercept(Activity activity, String url) {
		if (StoreUtils.isStoreIntent(url)) {
			org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), activity.getString(R.string.google_play));
			return true; // intercepted
		}
		if (LinkUtils.isEmailIntent(url)) {
			org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), activity.getString(R.string.email));
			return true; // intercepted
		}
		if (LinkUtils.isPhoneNumberIntent(url)) {
			org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), activity.getString(R.string.tel));
			return true; // intercepted
		}
		if (LinkUtils.isPDFIntent(url)) {
			org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), activity.getString(R.string.file));
			return true; // intercepted
		}
		if (LinkUtils.isYouTubeIntent(url)) {
			org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), activity.getString(R.string.video));
			return true; // intercepted
		}
		return false; // not intercepted
	}

	public static boolean open(@NonNull Activity activity, Uri uri, String label, boolean www) {
		return org.mtransit.android.commons.LinkUtils.open(activity, uri, label);
	}

	public static boolean open(@NonNull Activity activity, Intent intent, String label, boolean www) {
		return org.mtransit.android.commons.LinkUtils.open(activity, intent, label);
	}

	public static void sendEmail(@NonNull Activity activity) {
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		String email = activity.getString(R.string.send_feedback_email);
		intent.setData(Uri.parse(EMAIL_SCHEME + ":" + email)); // only email apps should handle this
		intent.putExtra(Intent.EXTRA_EMAIL, email);
		StringBuilder subjectSb = new StringBuilder();
		subjectSb //
				.append(PackageManagerUtils.getAppName(activity)) //
				.append(" v").append(PackageManagerUtils.getAppVersionName(activity)) //
				.append(" (r").append(PackageManagerUtils.getAppVersionCode(activity)).append(")");
		try {
			if (DataSourceProvider.isSet()) {
				DataSourceProvider dataSourceProvider = DataSourceProvider.get();
				if (dataSourceProvider != null) {
					ArrayList<AgencyProperties> allAgencies = dataSourceProvider.getAllAgencies(activity);
					for (AgencyProperties agencyProperties : allAgencies) {
						if (!agencyProperties.getType().isMapScreen()) {
							continue;
						}
						subjectSb //
								.append(" - ").append(agencyProperties.getShortName()) //
								.append(" ").append(activity.getString(agencyProperties.getType().getShortNameResId()));
						String pkg = dataSourceProvider.getAgencyPkg(agencyProperties.getAuthority());
						if (!TextUtils.isEmpty(pkg)) {
							subjectSb //
									.append(" v").append(PackageManagerUtils.getAppVersionName(activity, pkg)) //
									.append(" (r").append(PackageManagerUtils.getAppVersionCode(activity, pkg)).append(")");
						}
					}
				}
			}
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Error while adding agencies to email subject!");
		}
		intent.putExtra(Intent.EXTRA_SUBJECT, subjectSb.toString());
		open(activity, intent, activity.getString(R.string.email), false);
	}

	private static final String EMAIL_SCHEME = "mailto";

	public static boolean isEmailIntent(String url) {
		return isEmailIntent(Uri.parse(url));
	}

	public static boolean isEmailIntent(Uri uri) {
		if (uri != null) {
			if (EMAIL_SCHEME.equals(uri.getScheme())) {
				return true;
			}
		}
		return false;
	}

	private static final String TEL_SCHEME = "tel";

	public static boolean isPhoneNumberIntent(String url) {
		return isPhoneNumberIntent(Uri.parse(url));
	}

	public static boolean isPhoneNumberIntent(Uri uri) {
		if (uri != null) {
			if (TEL_SCHEME.equals(uri.getScheme())) {
				return true;
			}
		}
		return false;
	}

	private static final String EXT_PDF = ".pdf";

	public static boolean isPDFIntent(String url) {
		if (url != null) {
			if (url.toLowerCase(Locale.ENGLISH).endsWith(EXT_PDF)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPDFIntent(Uri uri) {
		return isPDFIntent(uri == null ? null : uri.toString());
	}

	public static boolean isYouTubeIntent(String url) {
		return isYouTubeIntent(Uri.parse(url));
	}

	private static final String HTTP_SCHEME = "http";
	private static final String HTTPS_SCHEME = "https";
	private static final Pattern YOUTUBE_WWW_AUTHORITY_REGEX = Pattern.compile("(youtube\\.com|youtu\\.be)");

	public static boolean isYouTubeIntent(Uri uri) {
		if (uri != null) {
			if (HTTPS_SCHEME.equals(uri.getScheme()) || HTTP_SCHEME.equals(uri.getScheme())) {
				if (YOUTUBE_WWW_AUTHORITY_REGEX.matcher(uri.getAuthority()).find()) {
					return true;
				}
			}
		}
		return false;
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

	public interface OnUrlClickListener {
		boolean onURLClick(String url);
	}
}
