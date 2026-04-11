package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.snackbar.Snackbar;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.ad.IAdScreenActivity;
import org.mtransit.android.ad.IAdScreenFragment;
import org.mtransit.android.analytics.AnalyticsManager;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.rate.AppRatingsManager;
import org.mtransit.android.rate.AppRatingsUIManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.EdgeToEdgeKt;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.inappnotification.InAppNotificationUI;
import org.mtransit.commons.FeatureFlags;

import java.util.WeakHashMap;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class ABFragment extends MTFragmentX implements
		AnalyticsManager.Trackable,
		IAdScreenFragment {

	private static final boolean DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT = false;

	public static final boolean DEFAULT_DISPLAY_HOME_AS_UP_ENABLED = true;

	public static final boolean DEFAULT_SHOW_SEARCH_MENU_ITEM = true;

	@Inject
	IAdManager sharedAdManager;
	@Inject
	IAnalyticsManager sharedAnalyticsManager;
	@Inject
	StatusLoader sharedStatusLoader;
	@Inject
	ServiceUpdateLoader sharedServiceUpdateLoader;
	@Inject
	AppRatingsManager sharedAppRatingsManager;

	public ABFragment() {
		super();
	}

	@ContentView
	public ABFragment(@LayoutRes int contentLayoutId) {
		super(contentLayoutId);
	}

	public abstract boolean hasToolbar();

	@Nullable
	public abstract Toolbar getToolbar();

	@Nullable
	public abstract AppBarLayout getAppBarLayout();

	@Nullable
	private Integer initialBackStackEntryCount = null;

	@CallSuper
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		final FragmentActivity activity = getActivity();
		if (activity != null) {
			this.initialBackStackEntryCount = activity.getSupportFragmentManager().getBackStackEntryCount();
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.initialBackStackEntryCount = null;
	}

	public void updateScreenToolbarNavigationIcon() {
		final Toolbar toolbar = getToolbar();
		if (toolbar == null) return;
		if (initialBackStackEntryCount == null) return;
		toolbar.setNavigationIcon(
				initialBackStackEntryCount <= 0 ?
						R.drawable.ic_drawer_menu_24px :
						R.drawable.ic_arrow_back_24
		);
	}

	public void updateScreenToolbarTitle() {
		final Toolbar toolbar = getToolbar();
		if (toolbar == null) return;
		toolbar.setTitle(getABTitle(getContext()));
	}

	public void updateScreenToolbarSubtitle() {
		final Toolbar toolbar = getToolbar();
		if (toolbar == null) return;
		toolbar.setSubtitle(getABSubtitle(getContext()));
	}

	@CallSuper
	public void updateScreenToolbarBgColor() {
		final Integer bgColorInt = getABBgColor(getContext());
		if (bgColorInt != null) {
			getBgDrawable().setFillColor(ColorStateList.valueOf(bgColorInt));
			final MainActivity mainActivity = getMainActivity();
			if (isResumed() && mainActivity != null) {
				EdgeToEdgeKt.setStatusBarBgColorEdgeToEdge(getMainActivity(), bgColorInt);
			}
		}
	}

	public void updateScreenToolbarOverrideGradient() {
		final AppBarLayout appBarLayout = getAppBarLayout();
		if (appBarLayout == null) return;
		final boolean overrideGradient = isABOverrideGradient();
		if (overrideGradient) {
			appBarLayout.setBackground(AppCompatResources.getDrawable(appBarLayout.getContext(), R.drawable.ab_gradient));
		} else {
			setupScreenToolbarBgColor();
		}
	}

	private void setupScreenToolbarBgColor() {
		final AppBarLayout appBarLayout = getAppBarLayout();
		if (appBarLayout == null) return;
		appBarLayout.setBackground(getBgDrawable());
	}

	@Nullable
	private MaterialShapeDrawable bgDrawable = null;

	@NonNull
	private MaterialShapeDrawable getBgDrawable() {
		if (this.bgDrawable == null) {
			this.bgDrawable = new MaterialShapeDrawable();
		}
		return this.bgDrawable;
	}

	public void setupScreenToolbar() {
		final Toolbar toolbar = getToolbar();
		if (toolbar == null) return;
		// setup
		setupScreenToolbarBgColor();
		toolbar.setNavigationOnClickListener(this::onScreenToolbarNavigationClick);
		if (this instanceof MenuProvider) {
			toolbar.addMenuProvider((MenuProvider) this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
		}
		inflateMainMenu();
		toolbar.setOnMenuItemClickListener(this::onScreenToolbarMenuItemClick);
		// initial UI
		updateScreenToolbarNavigationIcon();
		updateScreenToolbarTitle();
		updateScreenToolbarSubtitle();
		updateScreenToolbarOverrideGradient();
		updateScreenToolbarBgColor();
		updateScreenToolbarCustomView();
	}

	public void onScreenToolbarNavigationClick(@NonNull View v) {
		if (getParentFragmentManager().getBackStackEntryCount() == 0) {
			final MainActivity mainActivity = getMainActivity();
			if (mainActivity != null) {
				mainActivity.openDrawer();
			}
			return;
		}
		getParentFragmentManager().popBackStack();
	}

	// R.menu.menu_main
	private void inflateMainMenu() {
		final Toolbar toolbar = getToolbar();
		if (toolbar == null) return;
		final MenuItem searchMenuItem = toolbar.getMenu().add(Menu.NONE, R.id.nav_search, 999, R.string.menu_action_search);
		searchMenuItem.setIcon(R.drawable.ic_search_black_24dp);
		searchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		searchMenuItem.setVisible(isABShowSearchMenuItem());
	}

	private boolean onScreenToolbarMenuItemClick(@NonNull MenuItem item) {
		final ActionBarController abController = getAbController();
		if (abController != null && abController.onOptionsItemSelected(item)) {
			return true; // handled
		}
		//noinspection RedundantIfStatement
		if (this instanceof MenuProvider && ((MenuProvider) this).onMenuItemSelected(item)) {
			return true; // handled
		}
		return false; // not handled
	}

	public boolean isABReady() {
		return true; // default = true = ready
	}

	@Nullable
	public CharSequence getABTitle(@Nullable Context context) {
		return null;
	}

	@Nullable
	public CharSequence getABSubtitle(@Nullable Context context) {
		return null;
	}

	@ColorInt
	@Nullable
	private Integer defaultABBgColor = null;

	@Nullable
	@ColorInt
	public Integer getABBgColor(@Nullable Context context) {
		if (isABStatusBarTransparent()) {
			return Color.TRANSPARENT;
		}
		if (context == null) {
			return null;
		}
		return getDefaultABBgColor(context);
	}

	@ColorInt
	@NonNull
	public Integer getDefaultABBgColor(@NonNull Context context) {
		if (this.defaultABBgColor == null) {
			this.defaultABBgColor = ThemeUtils.resolveColorAttribute(context, android.R.attr.colorPrimary);
		}
		return this.defaultABBgColor;
	}

	public boolean isABOverrideGradient() {
		return false;
	}

	public boolean isABStatusBarTransparent() {
		return false;
	}

	public boolean isNavBarProtected() {
		return true;
	}

	@Nullable
	public View getABCustomView() {
		return null;
	}

	public boolean isABCustomViewFocusable() {
		return false;
	}

	public boolean isABCustomViewRequestFocus() {
		return false;
	}

	@Nullable
	private View currentScreenToolbarCustomView = null;

	public void updateScreenToolbarCustomView() {
		final Toolbar toolbar = getToolbar();
		if (toolbar == null) return;
		final View customView = getABCustomView();
		setScreenToolbarCustomView(customView);
		if (customView == null) {
			return;
		}
		if (isABCustomViewFocusable()) {
			customView.setFocusable(true);
			customView.setFocusableInTouchMode(true);
			if (isABCustomViewRequestFocus()) {
				customView.requestFocus();
				customView.requestFocusFromTouch();
			} else {
				customView.clearFocus();
			}
		}
	}

	private void setScreenToolbarCustomView(@Nullable View customView) {
		final Toolbar toolbar = getToolbar();
		if (toolbar == null) return;
		if (this.currentScreenToolbarCustomView == customView) {
			return; // no change
		}
		if (this.currentScreenToolbarCustomView != null) {
			toolbar.removeView(this.currentScreenToolbarCustomView);
		}
		this.currentScreenToolbarCustomView = customView;
		if (this.currentScreenToolbarCustomView != null) {
			toolbar.addView(this.currentScreenToolbarCustomView);
		}
	}

	public void resetScreenToolbarCustomView() {
		setScreenToolbarCustomView(null);
	}

	public boolean isABDisplayHomeAsUpEnabled() {
		return DEFAULT_DISPLAY_HOME_AS_UP_ENABLED;
	}

	public boolean isABShowSearchMenuItem() {
		return DEFAULT_SHOW_SEARCH_MENU_ITEM;
	}

	public boolean isABThemeDarkInsteadOfThemeLight() {
		return DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT;
	}

	@Nullable
	public ActionBarController getAbController() {
		if (FeatureFlags.F_NAVIGATION) {
			return null;
		}
		final MainActivity mainActivity = getMainActivity();
		if (mainActivity == null) {
			return null;
		}
		return mainActivity.getAbController();
	}

	@Nullable
	MainActivity getMainActivity() {
		final FragmentActivity activity = getActivity();
		if (!(activity instanceof MainActivity)) {
			return null;
		}
		return (MainActivity) activity;
	}

	@CallSuper
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final FragmentActivity activity = requireActivity();
		EdgeToEdgeKt.setStatusBarsThemeEdgeToEdge(activity);
		EdgeToEdgeKt.applyStatusBarsHeightEdgeToEdge(activity.findViewById(R.id.status_bar_bg));
		EdgeToEdgeKt.setNavBarThemeEdgeToEdge(activity);
		EdgeToEdgeKt.setNavBarProtectionEdgeToEdge(activity, isNavBarProtected());
		if (this instanceof MenuProvider && !hasToolbar()) {
			requireActivity().addMenuProvider((MenuProvider) this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
		}
	}

	@Override
	public boolean hasAds() {
		return false; // will show main activity ads
	}

	@CallSuper
	@Override
	public void onResume() {
		super.onResume();
		sharedAdManager.onResumeScreen((IAdScreenActivity) requireActivity());
		sharedAnalyticsManager.trackScreenView(this);
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setAB(this);
			abController.updateAB();
		}
		if (hasToolbar()) {
			updateScreenToolbarBgColor(); // status bar color
			updateScreenToolbarNavigationIcon();
		}
		sharedAppRatingsManager.getShouldShowAppRatingRequest(this).observe(this, shouldShow -> {
			if (!shouldShow) {
				return;
			}
			final MainActivity mainActivity = getMainActivity();
			if (mainActivity == null) {
				return;
			}
			AppRatingsUIManager.showAppRatingsUI(mainActivity, sharedAnalyticsManager, appRatingDisplayed -> {
				if (appRatingDisplayed) {
					this.sharedAppRatingsManager.onAppRequestDisplayed(this, this);
				}
				return kotlin.Unit.INSTANCE;
			});
		});
	}

	@CallSuper
	@Override
	public void onPause() {
		super.onPause();
		this.sharedStatusLoader.clearAllTasks();
		this.sharedServiceUpdateLoader.clearAllTasks();
		hideAllInAppNotifications();
	}

	@CallSuper
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		this.inAppNotifications.clear();
		this.inAppNotificationShown = false;
	}

	@Nullable
	@Override
	public <T extends View> T findViewById(int id) {
		if (getView() == null) {
			return null;
		}
		return getView().findViewById(id);
	}

	// region In-App Notifications

	private boolean inAppNotificationShown = false;

	private final WeakHashMap<String, Pair<PopupWindow, Snackbar>> inAppNotifications = new WeakHashMap<>();

	@SuppressWarnings("unused")
	public boolean showInAppNotification(
			@NonNull String notificationId,
			@Nullable Activity activity,
			@Nullable View view,
			@Nullable View contextView,
			@Nullable View anchorView,
			int additionalBottomMarginInPx,
			@NonNull CharSequence labelText,
			@Nullable CharSequence actionText,
			@Nullable View.OnLongClickListener onActionClick // used instead of OnClickListener because returns boolean
	) {
		if (this.inAppNotificationShown) {
			return false; // SKIP
		}
		if (!isResumed()) {
			return false; // SKIP
		}
		Pair<PopupWindow, Snackbar> inAppNotification = inAppNotifications.get(notificationId);
		if (inAppNotification == null) {
			inAppNotification = InAppNotificationUI.makeInAppNotification(
					activity,
					view,
					contextView,
					anchorView,
					labelText,
					() -> { // on dismiss
						this.inAppNotificationShown = false;
						return true; // handled
					},
					actionText, onActionClick, () -> { // on action clicked
						return hideInAppNotification(notificationId);
					}
			);
			inAppNotifications.put(notificationId, inAppNotification);
		}
		this.inAppNotificationShown = InAppNotificationUI.showInAppNotification(
				activity,
				inAppNotification,
				view,
				contextView,
				anchorView,
				additionalBottomMarginInPx
		);
		return this.inAppNotificationShown;
	}

	@SuppressWarnings("WeakerAccess")
	public boolean hideInAppNotification(@NonNull String notificationId) {
		Pair<PopupWindow, Snackbar> inAppNotification = this.inAppNotifications.get(notificationId);
		final boolean inAppNotificationHidden = InAppNotificationUI.hideInAppNotification(inAppNotification);
		if (inAppNotificationHidden) {
			this.inAppNotificationShown = false;
		}
		return inAppNotificationHidden;
	}

	@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
	public boolean hideAllInAppNotifications() {
		boolean allInAppNotificationsHidden = true;
		for (String inAppNotificationId : inAppNotifications.keySet()) {
			if (inAppNotificationId != null) {
				if (!hideInAppNotification(inAppNotificationId)) {
					allInAppNotificationsHidden = false;
				}
			}
		}
		return allInAppNotificationsHidden;
	}

	// endregion
}
