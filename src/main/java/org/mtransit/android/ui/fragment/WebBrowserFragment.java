package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.util.LinkUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class WebBrowserFragment extends ABFragment {

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

	private static final String EXTRA_URL_INITIAL = "extra_url_initial";

	private static final String EXTRA_URL_CURRENT = "extra_url_current";

	@NonNull
	public static WebBrowserFragment newInstance(@NonNull String url) {
		WebBrowserFragment f = new WebBrowserFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_URL_INITIAL, url);
		f.initialUrl = url;
		f.setArguments(args);
		return f;
	}

	private String initialUrl;
	private String currentUrl;
	private String pageTitle;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newInitialUrl = BundleUtils.getString(EXTRA_URL_INITIAL, bundles);
		if (newInitialUrl != null && !newInitialUrl.equals(this.initialUrl)) {
			this.initialUrl = newInitialUrl;
		}
		String newCurrentUrl = BundleUtils.getString(EXTRA_URL_CURRENT, bundles);
		if (newCurrentUrl != null && !newCurrentUrl.equals(this.currentUrl)) {
			this.currentUrl = newCurrentUrl;
		}
		View view = getView();
		if (view != null) {
			WebView webView = view.findViewById(R.id.webView);
			if (webView != null && bundles.length > 0) {
				webView.restoreState(bundles[0]);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (!TextUtils.isEmpty(this.initialUrl)) {
			outState.putString(EXTRA_URL_INITIAL, this.initialUrl);
		}
		if (!TextUtils.isEmpty(this.currentUrl)) {
			outState.putString(EXTRA_URL_CURRENT, this.currentUrl);
		}
		View view = getView();
		if (view != null) {
			WebView webView = view.findViewById(R.id.webView);
			if (webView != null) {
				webView.saveState(outState);
			}
		}
		super.onSaveInstanceState(outState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_web_browser, container, false);
		setupView(view);
		return view;
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setupView(View view) {
		if (view == null) {
			return;
		}
		WebView webView = view.findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.setWebChromeClient(new MTWebChromeClient(this));
		webView.setWebViewClient(new MTWebViewClient(this));
	}

	@Override
	public boolean onBackPressed() {
		View view = getView();
		if (view != null) {
			WebView webView = view.findViewById(R.id.webView);
			if (webView != null && webView.canGoBack()) {
				webView.goBack();
				return true; // handled
			}
		}
		return super.onBackPressed();
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (view != null) {
			WebView webView = view.findViewById(R.id.webView);
			if (webView != null) {
				if (TextUtils.isEmpty(this.currentUrl)) {
					webView.loadUrl(this.initialUrl);
				} else if (!this.currentUrl.equals(webView.getUrl())) {
					webView.loadUrl(this.currentUrl);
				}
				webView.onResume();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		View view = getView();
		if (view != null) {
			WebView webView = view.findViewById(R.id.webView);
			if (webView != null) {
				webView.onPause();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		View view = getView();
		if (view != null) {
			WebView webView = view.findViewById(R.id.webView);
			if (webView != null) {
				webView.destroy();
			}
		}
	}

	public void onProgressChanged(int newProgress) {
		View view = getView();
		if (view != null) {
			ProgressBar progressBar = view.findViewById(R.id.progress_bar);
			if (progressBar != null) {
				progressBar.setProgress(newProgress);
				if (newProgress < 100) {
					progressBar.setVisibility(View.VISIBLE);
				} else {
					progressBar.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	public boolean shouldOverrideUrlLoading(@SuppressWarnings("unused") WebView webView,
											String url) {
		if (LinkUtils.intercept(requireActivity(), url)) {
			return true;
		}
		onURLChanged(url);
		return false;
	}

	public void onTitleChanged(String title) {
		this.pageTitle = title;
		ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setABTitle(this, getABTitle(getContext()), true);
		}
	}

	public void onURLChanged(String url) {
		this.currentUrl = url;
		ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setABSubtitle(this, getABSubtitle(getContext()), true);
		}
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		if (!TextUtils.isEmpty(this.currentUrl)) {
			return this.currentUrl;
		}
		return super.getABSubtitle(context);
	}

	@Override
	public CharSequence getABTitle(Context context) {
		if (!TextUtils.isEmpty(this.pageTitle)) {
			return this.pageTitle;
		}
		return context.getString(R.string.web_browser);
	}

	@Override
	public void onModulesUpdated() {
		// do nothing
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_web_browser, menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_open_www:
			LinkUtils.open(getActivity(), this.currentUrl, getString(R.string.web_browser), false);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static class MTWebChromeClient extends WebChromeClient implements MTLog.Loggable {

		private static final String LOG_TAG = WebBrowserFragment.class.getSimpleName() + ">" + MTWebChromeClient.class.getSimpleName();

		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final WeakReference<WebBrowserFragment> webBrowserFragmentWR;

		public MTWebChromeClient(@NonNull WebBrowserFragment webBrowserFragment) {
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
	}

	private static class MTWebViewClient extends WebViewClient implements MTLog.Loggable {

		private static final String LOG_TAG = WebBrowserFragment.class.getSimpleName() + ">" + MTWebViewClient.class.getSimpleName();

		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final WeakReference<WebBrowserFragment> webBrowserFragmentWR;

		public MTWebViewClient(@NonNull WebBrowserFragment webBrowserFragment) {
			this.webBrowserFragmentWR = new WeakReference<>(webBrowserFragment);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView webView, String url) {
			try {
				WebBrowserFragment webBrowserFragment = this.webBrowserFragmentWR.get();
				if (webBrowserFragment != null && webBrowserFragment.shouldOverrideUrlLoading(webView, url)) {
					return true;
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error during should override URL loading!");
			}
			return super.shouldOverrideUrlLoading(webView, url);
		}

		@Override
		public void onPageStarted(WebView webView, String url, Bitmap favicon) {
			super.onPageStarted(webView, url, favicon);
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
			super.onPageFinished(webView, url);
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
