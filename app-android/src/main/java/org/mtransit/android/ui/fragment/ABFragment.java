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
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.databinding.LayoutScreenToolbarBinding;
import org.mtransit.android.rate.AppRatingsManager;
import org.mtransit.android.rate.AppRatingsUIManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.EdgeToEdgeKt;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.inappnotification.InAppNotificationUI;
import org.mtransit.android.util.UIFeatureFlags;
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

	public boolean hasToolbar() {
		return false; // WIP: will show main activity Action Bar
	}

	public void updateScreenToolbarNavigationIcon(@NonNull Toolbar toolbar) {
		updateScreenToolbarNavigationIcon(toolbar, getParentFragmentManager().getBackStackEntryCount());
	}

	private void updateScreenToolbarNavigationIcon(@NonNull Toolbar toolbar, int backStackEntryCount) {
		toolbar.setNavigationIcon(
				backStackEntryCount <= 0 ?
						R.drawable.ic_drawer_menu_24px :
						R.drawable.ic_arrow_back_24
		);
	}

	public void updateScreenToolbarTitle(@NonNull Toolbar toolbar) {
		toolbar.setTitle(getABTitle(getContext()));
	}

	public void updateScreenToolbarSubtitle(@NonNull Toolbar toolbar) {
		toolbar.setSubtitle(getABSubtitle(getContext()));
	}

	public void updateScreenToolbarBgColor(@NonNull LayoutScreenToolbarBinding screenToolbarLayout) {
		updateScreenToolbarBgColor(screenToolbarLayout.screenToolbarLayout, screenToolbarLayout.screenToolbar);
	}

	private void updateScreenToolbarBgColor(
			@SuppressWarnings("unused") @NonNull AppBarLayout appBarLayout,
			@SuppressWarnings("unused") @NonNull Toolbar toolbar) {
		final Integer bgColorInt = getABBgColor(getContext());
		if (bgColorInt != null) {
			getBgDrawable().setFillColor(ColorStateList.valueOf(bgColorInt));
			final MainActivity mainActivity = getMainActivity();
			if (mainActivity != null) {
				EdgeToEdgeKt.setStatusBarBgColorEdgeToEdge(getMainActivity(), bgColorInt);
			}
		}
	}

	void updateScreenToolbarOverrideGradient(@NonNull LayoutScreenToolbarBinding screenToolbarLayout) {
		updateScreenToolbarOverrideGradient(screenToolbarLayout.screenToolbarLayout, screenToolbarLayout.screenToolbar);
	}

	private void updateScreenToolbarOverrideGradient(@NonNull AppBarLayout appBarLayout, @NonNull Toolbar toolbar) {
		final boolean overrideGradient = isABOverrideGradient();
		if (overrideGradient) {
			appBarLayout.setBackground(AppCompatResources.getDrawable(appBarLayout.getContext(), R.drawable.ab_gradient));
		} else {
			setupScreenToolbarBgColor(appBarLayout, toolbar);
		}
	}

	private void setupScreenToolbarBgColor(@NonNull AppBarLayout appBarLayout, @SuppressWarnings("unused") @NonNull Toolbar toolbar) {
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

	public void setupScreenToolbar(@NonNull LayoutScreenToolbarBinding screenToolbarLayout) {
		setupScreenToolbar(screenToolbarLayout.screenToolbarLayout, screenToolbarLayout.screenToolbar);
	}

	private void setupScreenToolbar(@NonNull AppBarLayout appBarLayout, @NonNull Toolbar toolbar) {
		// setup
		setupScreenToolbarBgColor(appBarLayout, toolbar);
		toolbar.setNavigationOnClickListener(v -> {
			if (getParentFragmentManager().getBackStackEntryCount() == 0) {
				final MainActivity mainActivity = getMainActivity();
				if (mainActivity != null) {
					mainActivity.openDrawer();
				}
				return;
			}
			getParentFragmentManager().popBackStack();
		});
		if (this instanceof MenuProvider) {
			toolbar.addMenuProvider((MenuProvider) this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
		}
		inflateMainMenu(toolbar);
		toolbar.setOnMenuItemClickListener(this::onScreenToolbarMenuItemClick);
		// initial UI
		updateScreenToolbarNavigationIcon(toolbar);
		updateScreenToolbarTitle(toolbar);
		updateScreenToolbarSubtitle(toolbar);
		updateScreenToolbarOverrideGradient(appBarLayout, toolbar);
		updateScreenToolbarBgColor(appBarLayout, toolbar);
		updateScreenToolbarCustomView(toolbar);
	}

	// R.menu.menu_main
	private void inflateMainMenu(@NonNull Toolbar toolbar) {
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

	public void updateScreenToolbarCustomView(@NonNull Toolbar toolbar) {
		final View customView = getABCustomView();
		setScreenToolbarCustomView(toolbar, customView);
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

	private void setScreenToolbarCustomView(@NonNull Toolbar toolbar, @Nullable View customView) {
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

	public void resetScreenToolbarCustomView(@NonNull Toolbar toolbar) {
		setScreenToolbarCustomView(toolbar, null);
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

	public boolean onBackPressed() {
		//noinspection IfStatementWithIdenticalBranches
		if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
			return false; // processed
		}
		return false; // not processed
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
