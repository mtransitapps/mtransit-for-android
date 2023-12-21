package org.mtransit.android.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.IAgencyProperties;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.WebBrowserFragment;
import org.mtransit.android.ui.view.common.NavControllerExtKt;
import org.mtransit.commons.FeatureFlags;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class LinkUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LinkUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static CharSequence linkifyHtml(@NonNull String originalText, boolean isHTML) {
		try {
			Spanned text = isHTML ? Html.fromHtml(originalText) : new SpannedString(originalText);
			URLSpan[] currentSpans = text.getSpans(0, text.length(), URLSpan.class);
			SpannableStringBuilder buffer = new SpannableStringBuilder(text);
			Linkify.addLinks(buffer, isHTML ? Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.MAP_ADDRESSES : Linkify.ALL);
			for (URLSpan span : currentSpans) {
				SpanUtils.set(buffer, text.getSpanStart(span), text.getSpanEnd(span), span);
			}
			return buffer;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while linkify-ing '%s'!", originalText);
			return originalText;
		}
	}

	public static boolean open(@Nullable View view, @NonNull Activity activity, @Nullable String url, @Nullable String label, boolean www) {
		if (url == null || url.isEmpty()) {
			return false;
		}
		if (intercept(activity, url)) {
			return true;
		}
		if (www && view != null) {
			boolean useInternalWebBrowser = PreferenceUtils.getPrefDefault(activity,
					PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER, PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT);
			if (useInternalWebBrowser) {
				if (FeatureFlags.F_NAVIGATION) {
					final NavController navController = Navigation.findNavController(view);
					FragmentNavigator.Extras extras = null;
					if (FeatureFlags.F_TRANSITION) {
						extras = new FragmentNavigator.Extras.Builder()
								// TODO ? .addSharedElement(view, view.getTransitionName())
								.build();
					}
					NavControllerExtKt.navigateF(navController,
							R.id.nav_to_web_screen,
							WebBrowserFragment.newInstanceArgs(url),
							null,
							extras
					);
					return true;
				} else {
					if (activity instanceof MainActivity) {
						((MainActivity) activity).addFragmentToStack(
								WebBrowserFragment.newInstance(url)
						);
						return true;
					}
				}
			}
		}
		return org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), label);
	}

	public static boolean interceptIntent(@NonNull WebView webView, @NonNull String url) {
		try {
			Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
			if (intent != null) {
				if (PackageManagerUtils.isAppInstalledDefault(webView.getContext(), intent)) { // Only works with apps pkg added to AndroidManifest.xml (API Level 30+)
					org.mtransit.android.commons.LinkUtils.open(webView.getContext(), intent, null);
					return true; // INTERCEPTED
				} else {
					final String fallbackUrl = intent.getStringExtra("browser_fallback_url");
					if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
						webView.loadUrl(fallbackUrl);
						return true; // INTERCEPTED
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while processing '%s'!", url);
		}
		return false; // not INTERCEPTED
	}

	public static boolean intercept(@NonNull Activity activity, @NonNull String url) {
		if (StoreUtils.isStoreIntent(url)) {
			org.mtransit.android.commons.LinkUtils.open(activity, Uri.parse(url), activity.getString(org.mtransit.android.commons.R.string.google_play));
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

	public static boolean open(@NonNull Activity activity, @Nullable Uri uri, @Nullable String label, @SuppressWarnings("unused") boolean www) {
		return org.mtransit.android.commons.LinkUtils.open(activity, uri, label);
	}

	public static boolean open(@NonNull Activity activity, @Nullable Intent intent, @Nullable String label, @SuppressWarnings("unused") boolean www) {
		return org.mtransit.android.commons.LinkUtils.open(activity, intent, label);
	}

	@MainThread
	public static void sendEmail(@NonNull Activity activity, @NonNull DataSourcesRepository dataSourcesRepository) {
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
			final List<AgencyProperties> allAgencies = dataSourcesRepository.getAllAgencies();
			for (IAgencyProperties agencyProperties : allAgencies) {
				if (!agencyProperties.getType().isMapScreen()) {
					continue;
				}
				subjectSb //
						.append(" - ").append(agencyProperties.getShortName()) //
						.append(" ").append(activity.getString(agencyProperties.getType().getShortNameResId()));
				final String pkg = agencyProperties.getPkg();
				if (!pkg.isEmpty()) {
					subjectSb //
							.append(" v").append(PackageManagerUtils.getAppVersionName(activity, pkg)) //
							.append(" (r").append(PackageManagerUtils.getAppVersionCode(activity, pkg)).append(")");
				}
			}
		} catch (Exception e) {
			//noinspection deprecation // FXIME
			CrashUtils.w(LOG_TAG, e, "Error while adding agencies to email subject!");
		}
		intent.putExtra(Intent.EXTRA_SUBJECT, subjectSb.toString());
		open(activity, intent, activity.getString(R.string.email), false);
	}

	private static final String EMAIL_SCHEME = "mailto";

	private static boolean isEmailIntent(String url) {
		return isEmailIntent(Uri.parse(url));
	}

	private static boolean isEmailIntent(Uri uri) {
		if (uri != null) {
			//noinspection RedundantIfStatement
			if (EMAIL_SCHEME.equals(uri.getScheme())) {
				return true;
			}
		}
		return false;
	}

	private static final String TEL_SCHEME = "tel";

	private static boolean isPhoneNumberIntent(String url) {
		return isPhoneNumberIntent(Uri.parse(url));
	}

	private static boolean isPhoneNumberIntent(Uri uri) {
		if (uri != null) {
			//noinspection RedundantIfStatement
			if (TEL_SCHEME.equals(uri.getScheme())) {
				return true;
			}
		}
		return false;
	}

	private static final String EXT_PDF = ".pdf";

	private static boolean isPDFIntent(String url) {
		if (url != null) {
			//noinspection RedundantIfStatement
			if (url.toLowerCase(Locale.ENGLISH).endsWith(EXT_PDF)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPDFIntent(@Nullable Uri uri) {
		return isPDFIntent(uri == null ? null : uri.toString());
	}

	// https://developer.chrome.com/multidevice/android/intents
	public static boolean isIntentIntent(@NonNull String url) {
		return isIntentIntent(Uri.parse(url));
	}

	private static final String INTENT_SCHEME = "intent";

	private static boolean isIntentIntent(@Nullable Uri uri) {
		if (uri != null) {
			return INTENT_SCHEME.equals(uri.getScheme());
		}
		return false;
	}

	private static boolean isYouTubeIntent(String url) {
		return isYouTubeIntent(Uri.parse(url));
	}

	private static final String HTTP_SCHEME = "http";
	private static final String HTTPS_SCHEME = "https";
	private static final Pattern YOUTUBE_WWW_AUTHORITY_REGEX = Pattern.compile("(youtube\\.com|youtu\\.be)");

	private static boolean isYouTubeIntent(Uri uri) {
		if (uri != null) {
			if (HTTPS_SCHEME.equals(uri.getScheme())
					|| HTTP_SCHEME.equals(uri.getScheme())) {
				final String authority = uri.getAuthority();
				return authority != null && YOUTUBE_WWW_AUTHORITY_REGEX.matcher(authority).find();
			}
		}
		return false;
	}

	public static class LinkMovementMethodInterceptor extends LinkMovementMethod implements MTLog.Loggable {

		private static final String LOG_TAG = LinkMovementMethodInterceptor.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@Nullable
		private static LinkMovementMethodInterceptor sInstance;

		@NonNull
		public static LinkMovementMethodInterceptor getInstance(@Nullable OnUrlClickListener onUrlClickListener) {
			if (sInstance == null) {
				sInstance = new LinkMovementMethodInterceptor(onUrlClickListener);
			} else {
				sInstance.setOnUrlClickListener(onUrlClickListener);
			}
			return sInstance;
		}

		@Nullable
		private WeakReference<OnUrlClickListener> onUrlClickListenerWR;

		LinkMovementMethodInterceptor(@Nullable OnUrlClickListener onUrlClickListener) {
			setOnUrlClickListener(onUrlClickListener);
		}

		private void setOnUrlClickListener(@Nullable OnUrlClickListener onUrlClickListener) {
			this.onUrlClickListenerWR = new WeakReference<>(onUrlClickListener);
		}

		@Override
		public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
			final int action = event.getAction();
			if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
				int x = (int) event.getX();
				int y = (int) event.getY();
				x -= widget.getTotalPaddingStart();
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
								if (listener.onURLClick(widget, url)) {
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
		boolean onURLClick(@NonNull View view, @NonNull String url);
	}
}
