package org.mtransit.android.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.webkit.WebViewClientCompat;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.databinding.FragmentWebBrowserBinding;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.EdgeToEdgeKt;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.android.util.UIFeatureFlags;

import java.lang.ref.WeakReference;

@SuppressWarnings("DeprecatedCall")
public class WebBrowserFragment extends ABFragment implements MenuProvider {

	private static final String LOG_TAG = WebBrowserFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Web";

	@NonNull
	@Override
	public String getScreenName() {
		if (!TextUtils.isEmpty(this.initialUrl)) {
			return TRACKING_SCREEN_NAME + "/" + this.initialUrl;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_TITLE_STATIC = "extra_title_static";

	private static final String EXTRA_URL_INITIAL = "extra_url_initial";

	private static final String EXTRA_URL_CURRENT = "extra_url_current";

	@NonNull
	public static WebBrowserFragment newInstance(@NonNull String url) {
		return newInstance(url, null);
	}

	@NonNull
	public static WebBrowserFragment newInstance(@NonNull String url, @Nullable String titleStatic) {
		WebBrowserFragment f = new WebBrowserFragment();
		if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
			f.initialUrl = url;
		}
		f.setArguments(newInstanceArgs(url, titleStatic));
		return f;
	}

	@NonNull
	public static Bundle newInstanceArgs(@NonNull String url) {
		return newInstanceArgs(url, null);
	}

	@NonNull
	public static Bundle newInstanceArgs(@NonNull String url, @Nullable String titleStatic) {
		Bundle args = new Bundle();
		args.putString(EXTRA_URL_INITIAL, url);
		args.putString(EXTRA_TITLE_STATIC, titleStatic);
		return args;
	}

	public WebBrowserFragment() {
		super(R.layout.fragment_web_browser);
	}

	private String initialUrl;
	private String currentUrl;
	@Nullable
	private String pageTitleStatic = null;
	private String pageTitle;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	private void restoreInstanceState(Bundle... bundles) {
		final String newInitialUrl = BundleUtils.getString(EXTRA_URL_INITIAL, bundles);
		if (newInitialUrl != null && !newInitialUrl.equals(this.initialUrl)) {
			this.initialUrl = newInitialUrl;
		}
		final String newTitleStatic = BundleUtils.getString(EXTRA_TITLE_STATIC, bundles);
		if (newTitleStatic != null && !newTitleStatic.equals(this.pageTitleStatic)) {
			this.pageTitleStatic = newTitleStatic;
		}
		final String newCurrentUrl = BundleUtils.getString(EXTRA_URL_CURRENT, bundles);
		if (newCurrentUrl != null && !newCurrentUrl.equals(this.currentUrl)) {
			this.currentUrl = newCurrentUrl;
		}
		final WebView webView = binding == null ? null : binding.webView;
		if (webView != null && bundles.length > 0) {
			webView.restoreState(bundles[0]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (!TextUtils.isEmpty(this.initialUrl)) {
			outState.putString(EXTRA_URL_INITIAL, this.initialUrl);
		}
		if (!TextUtils.isEmpty(this.pageTitleStatic)) {
			outState.putString(EXTRA_TITLE_STATIC, this.pageTitleStatic);
		}
		if (!TextUtils.isEmpty(this.currentUrl)) {
			outState.putString(EXTRA_URL_CURRENT, this.currentUrl);
		}
		final WebView webView = binding == null ? null : binding.webView;
		if (webView != null) {
			webView.saveState(outState);
		}
		super.onSaveInstanceState(outState);
	}

	private @Nullable FragmentWebBrowserBinding binding = null;

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
		this.binding = FragmentWebBrowserBinding.bind(view);
		setupView(binding);
		if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
			final OnBackPressedCallback callback = new OnBackPressedCallback(false) {
				@Override
				public void handleOnBackPressed() {
					final WebView webView = binding == null ? null : binding.webView;
					if (webView != null && webView.canGoBack()) {
						webView.goBack();
					}
				}
			};
			onBackPressedCallback = callback;
			requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setupView(@Nullable FragmentWebBrowserBinding binding) {
		if (binding == null) {
			return;
		}
		final View view = binding.getRoot();
		EdgeToEdgeKt.applyStatusBarsInsetsEdgeToEdge(view);
		setupScreenToolbar(binding.screenToolbarLayout);
		updateScreenToolbarBgColor(binding.screenToolbarLayout);
		updateScreenToolbarOverrideGradient(binding.screenToolbarLayout);
		if (FileUtils.isImageURL(this.initialUrl)) {
			final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) binding.screenContent.getLayoutParams();
			params.setBehavior(null);
			binding.screenContent.setLayoutParams(params);
			binding.screenToolbarLayout.screenToolbarLayout.setLiftOnScroll(false);
			binding.screenToolbarLayout.screenToolbarLayout.setOutlineProvider(null);
			final MainActivity mainActivity = getMainActivity();
			if (mainActivity != null) {
				EdgeToEdgeKt.setStatusBarBgColorEdgeToEdge(getMainActivity(), getDefaultABBgColor(view.getContext()));
			}
		}
		final WebView webView = binding.webView;
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.getSettings().setGeolocationEnabled(true);
		if (FileUtils.isImageURL(this.initialUrl)) {
			webView.getSettings().setUseWideViewPort(true);
			webView.getSettings().setLoadWithOverviewMode(true);
			view.setPadding(0, 0, 0, 0);
		}
		webView.setWebChromeClient(new MTWebChromeClient(this));
		webView.setWebViewClient(new MTWebViewClient(this));
	}

	@Nullable
	private OnBackPressedCallback onBackPressedCallback = null;

	@Override
	public boolean onBackPressed() {
		if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
			return super.onBackPressed();
		}
		final WebView webView = binding == null ? null : binding.webView;
		if (webView != null && webView.canGoBack()) {
			webView.goBack();
			return true; // handled
		}
		return super.onBackPressed();
	}

	@Override
	public void onResume() {
		super.onResume();
		final WebView webView = binding == null ? null : binding.webView;
		if (webView != null) {
			if (TextUtils.isEmpty(this.currentUrl)) {
				webView.loadUrl(this.initialUrl);
			} else if (!this.currentUrl.equals(webView.getUrl())) {
				webView.loadUrl(this.currentUrl);
			}
			webView.onResume();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		final WebView webView = binding == null ? null : binding.webView;
		if (webView != null) {
			webView.onPause();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		this.binding = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		final WebView webView = binding == null ? null : binding.webView;
		if (webView != null) {
			webView.destroy();
		}
	}

	private void onProgressChanged(int newProgress) {
		final ProgressBar progressBar = binding == null ? null : binding.progressBar;
		if (progressBar != null) {
			progressBar.setProgress(newProgress);
			if (newProgress < 100) {
				progressBar.setVisibility(View.VISIBLE);
			} else {
				progressBar.setVisibility(View.INVISIBLE);
			}
		}
	}

	private boolean shouldOverrideUrlLoading(WebView webView,
											 @NonNull String url) {
		if (LinkUtils.isIntentIntent(url)) {
			if (LinkUtils.interceptIntent(webView, url)) {
				return true; // INTERCEPTED
			}
		} else if (LinkUtils.intercept(requireActivity(), url)) {
			return true; // INTERCEPTED
		}
		onURLChanged(url);
		return false; // NOT intercepted
	}

	private void onTitleChanged(String title) {
		this.pageTitle = title;
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setABTitle(this, getABTitle(getContext()), true);
		}
		final Toolbar screenToolbar = binding == null ? null : binding.screenToolbarLayout.screenToolbar;
		if (screenToolbar != null) {
			updateScreenToolbarTitle(screenToolbar);
		}
	}

	private void onURLChanged(String url) {
		this.currentUrl = url;
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setABSubtitle(this, getABSubtitle(getContext()), true);
		}
		final Toolbar screenToolbar = binding == null ? null : binding.screenToolbarLayout.screenToolbar;
		if (screenToolbar != null) {
			updateScreenToolbarSubtitle(screenToolbar);
		}
	}

	// TODO later view model
	// @Nullable
	// private POIViewModel getAddedViewModel() {
	// return isAttached() ? this.viewModel : null;
	// }
	//
	//
	@Override
	public boolean hasToolbar() {
		return true;
	}

	@Nullable
	@Override
	public Integer getABBgColor(@Nullable Context context) {
		if (FileUtils.isImageURL(this.initialUrl)) {
			return Color.TRANSPARENT;
		}
		return super.getABBgColor(context);
	}

	@Override
	public boolean isABStatusBarTransparent() {
		if (FileUtils.isImageURL(this.initialUrl)) {
			return true;
		}
		return super.isABStatusBarTransparent();
	}

	@Override
	public boolean isABOverrideGradient() {
		if (FileUtils.isImageURL(this.initialUrl)) {
			return true;
		}
		return super.isABOverrideGradient();
	}

	@Nullable
	@Override
	public CharSequence getABSubtitle(@Nullable Context context) {
		if (!TextUtils.isEmpty(this.currentUrl)) {
			return this.currentUrl;
		}
		return super.getABSubtitle(context);
	}

	@Nullable
	@Override
	public CharSequence getABTitle(@Nullable Context context) {
		if (!TextUtils.isEmpty(this.pageTitleStatic)) {
			return this.pageTitleStatic;
		}
		if (!TextUtils.isEmpty(this.pageTitle)) {
			return this.pageTitle;
		}
		if (context == null) {
			return super.getABTitle(null);
		}
		return context.getString(org.mtransit.android.commons.R.string.web_browser);
	}

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.menu_web_browser, menu);
	}

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		if (menuItem.getItemId() == R.id.menu_open_www) {
			LinkUtils.open(null, requireActivity(), this.currentUrl, getString(org.mtransit.android.commons.R.string.web_browser), false);
			return true; // handled
		}
		return false; // not handled
	}

	private static class MTWebChromeClient extends WebChromeClient implements MTLog.Loggable {

		private static final String LOG_TAG = WebBrowserFragment.class.getSimpleName() + ">" + MTWebChromeClient.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final WeakReference<WebBrowserFragment> webBrowserFragmentWR;

		MTWebChromeClient(@NonNull WebBrowserFragment webBrowserFragment) {
			this.webBrowserFragmentWR = new WeakReference<>(webBrowserFragment);
		}

		@Override
		public void onProgressChanged(WebView webView, int newProgress) {
			super.onProgressChanged(webView, newProgress);
			try {
				WebBrowserFragment webBrowserFragment = this.webBrowserFragmentWR.get();
				if (webBrowserFragment != null) {
					webBrowserFragment.onProgressChanged(newProgress);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error during on progress changed!");
			}
		}

		@Override
		public void onReceivedTitle(WebView webView, String title) {
			super.onReceivedTitle(webView, title);
			try {
				WebBrowserFragment webBrowserFragment = this.webBrowserFragmentWR.get();
				if (webBrowserFragment != null) {
					webBrowserFragment.onTitleChanged(title);
					webBrowserFragment.onURLChanged(webView.getUrl());
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error during on received title!");
			}
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
			boolean allow;
			switch (origin) {
			case "https://www.google.com":
			case "https://www.google.com/":
			case "https://www.google.com/maps":
			case "https://www.google.com/maps/":
			case "https://maps.google.com":
			case "https://maps.google.com/":
			case "https://maps.google.com/maps":
			case "https://maps.google.com/maps/":
				allow = true;
				break;
			default:
				allow = false;
			}
			callback.invoke(origin, allow, false);
		}
	}

	private static class MTWebViewClient extends WebViewClientCompat implements MTLog.Loggable {

		private static final String LOG_TAG = WebBrowserFragment.class.getSimpleName() + ">" + MTWebViewClient.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final WeakReference<WebBrowserFragment> webBrowserFragmentWR;

		MTWebViewClient(@NonNull WebBrowserFragment webBrowserFragment) {
			this.webBrowserFragmentWR = new WeakReference<>(webBrowserFragment);
		}

		@Override
		public boolean shouldOverrideUrlLoading(@NonNull WebView webView, @NonNull WebResourceRequest request) {
			try {
				WebBrowserFragment webBrowserFragment = this.webBrowserFragmentWR.get();
				if (webBrowserFragment != null
						&& webBrowserFragment.shouldOverrideUrlLoading(webView, request.getUrl().toString())) {
					return true; // handled
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error during should override URL loading!");
			}
			return super.shouldOverrideUrlLoading(webView, request);
		}

		@Override
		public void doUpdateVisitedHistory(WebView webView, String url, boolean isReload) {
			if (!UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
				return;
			}
			final WebBrowserFragment webBrowserFragment = this.webBrowserFragmentWR.get();
			final OnBackPressedCallback onBackPressedCallback = webBrowserFragment == null ? null : webBrowserFragment.onBackPressedCallback;
			if (onBackPressedCallback != null) {
				onBackPressedCallback.setEnabled(webView.canGoBack());
			}
		}

		@Override
		public void onPageStarted(WebView webView, String url, Bitmap favicon) {
			try {
				WebBrowserFragment webBrowserFragment = this.webBrowserFragmentWR.get();
				if (webBrowserFragment != null) {
					webBrowserFragment.onTitleChanged(webView.getTitle());
					webBrowserFragment.onURLChanged(url);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error during on page started!");
			}
		}

		@Override
		public void onPageFinished(WebView webView, String url) {
			try {
				WebBrowserFragment webBrowserFragment = this.webBrowserFragmentWR.get();
				if (webBrowserFragment != null) {
					webBrowserFragment.onTitleChanged(webView.getTitle());
					webBrowserFragment.onURLChanged(url);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error during on page finished!");
			}
		}
	}
}
