package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.snackbar.Snackbar;

import org.mtransit.android.analytics.AnalyticsManager;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.rate.AppRatingsManager;
import org.mtransit.android.rate.AppRatingsUIManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.EdgeToEdgeKt;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.inappnotification.InAppNotificationUI;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.commons.FeatureFlags;

import java.util.WeakHashMap;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

@AndroidEntryPoint
public abstract class ABFragment extends MTFragmentX implements
		AnalyticsManager.Trackable,
		IActivity {

	private static final boolean DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT = false;

	public static final boolean DEFAULT_DISPLAY_HOME_AS_UP_ENABLED = true;

	public static final boolean DEFAULT_SHOW_SEARCH_MENU_ITEM = true;

	@Inject
	IAnalyticsManager analyticsManager;
	@Inject
	StatusLoader statusLoader;
	@Inject
	ServiceUpdateLoader serviceUpdateLoader;
	@Inject
	AppRatingsManager appRatingsManager;

	public ABFragment() {
		super();
	}

	@ContentView
	public ABFragment(@LayoutRes int contentLayoutId) {
		super(contentLayoutId);
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
		if (this.defaultABBgColor == null && context != null) {
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
	public MainActivity getMainActivity() {
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
		EdgeToEdgeKt.setStatusBarColor(requireActivity(), isABStatusBarTransparent());
	}

	@CallSuper
	@Override
	public void onResume() {
		super.onResume();
		analyticsManager.trackScreenView(this, this);
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setAB(this);
			abController.updateAB();
		}
		appRatingsManager.getShouldShowAppRatingRequest(this).observe(this, shouldShow -> {
			if (!Boolean.TRUE.equals(shouldShow)) {
				return;
			}
			final MainActivity mainActivity = getMainActivity();
			if (mainActivity == null) {
				return;
			}
			AppRatingsUIManager.showAppRatingsUI(mainActivity, analyticsManager, appRatingDisplayed -> {
				if (appRatingDisplayed) {
					this.appRatingsManager.onAppRequestDisplayed(this, this);
				}
				return Unit.INSTANCE;
			});
		});
	}

	public boolean onBackPressed() {
		return false; // not processed
	}

	@CallSuper
	@Override
	public void onPause() {
		super.onPause();
		this.statusLoader.clearAllTasks();
		this.serviceUpdateLoader.clearAllTasks();
		hideAllInAppNotifications();
	}

	@CallSuper
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		this.inAppNotifications.clear();
		this.inAppNotificationShown = false;
	}

	@Override
	public void finish() {
		requireActivity().finish();
	}

	@Nullable
	@Override
	public <T extends View> T findViewById(int id) {
		if (getView() == null) {
			return null;
		}
		return getView().findViewById(id);
	}

	@NonNull
	@Override
	public LifecycleOwner getLifecycleOwner() {
		return this;
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
