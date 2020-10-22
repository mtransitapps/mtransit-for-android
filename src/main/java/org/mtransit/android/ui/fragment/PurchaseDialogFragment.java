package org.mtransit.android.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.di.Injection;
import org.mtransit.android.ui.view.common.IActivity;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class PurchaseDialogFragment extends MTDialogFragment implements IActivity, IAdManager.RewardedAdListener {

	private static final String LOG_TAG = PurchaseDialogFragment.class.getSimpleName();

	public static final String PAID_TASKS_PKG = "com.google.android.apps.paidtasks";

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static PurchaseDialogFragment newInstance() {
		return new PurchaseDialogFragment();
	}

	@NonNull
	private final IAdManager adManager;

	public PurchaseDialogFragment() {
		super();
		this.adManager = Injection.providesAdManager();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_dialog_purchase, container, false);
		setupView(view);
		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}

	@Override
	public void onCancel(@SuppressLint("UnknownNullness") DialogInterface dialog) {
		super.onCancel(dialog);
		ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_user_canceled_message);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		view.findViewById(R.id.buyBtn).setOnClickListener(v ->
				onBuyBtnClick(v.getContext())
		);
		view.findViewById(R.id.downloadOrOpenPaidTasksBtn).setOnClickListener(v ->
				onDownloadOrOpenPaidTasksBtnClick(v.getContext())
		);
		view.findViewById(R.id.rewardedAdsBtn).setOnClickListener(v ->
				onRewardedAdButtonClick(v.getContext())
		);
	}

	@NonNull
	@Override
	public Context requireContext() throws IllegalStateException {
		Context context = getContext();
		if (context == null) {
			throw new IllegalStateException("Fragment " + this + " not attached to a context.");
		}
		return context;
	}

	@NonNull
	@Override
	public Activity requireActivity() throws IllegalStateException {
		Activity activity = getActivity();
		if (activity == null) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return activity;
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

	private void onRewardedAdButtonClick(@NonNull Context context) {
		Activity activity = getActivity();
		try {
			if (activity == null) {
				MTLog.w(this, "onRewardedAdButtonClick() > skip (no view or no activity)");
				ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_default_failure_message);
				return;
			}
			if (!this.adManager.isRewardedAdAvailableToShow()) {
				MTLog.w(this, "onRewardedAdButtonClick() > skip (no ad available)");
				ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_not_ready);
				return;
			}
			this.adManager.showRewardedAd(this);
			final View view = getView();
			if (view != null) {
				view.findViewById(R.id.rewardedAdsBtn).setEnabled(false);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while handling download or open paid tasks button!");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_default_failure_message);
		}
	}

	private void onDownloadOrOpenPaidTasksBtnClick(@NonNull Context context) {
		Activity activity = getActivity();
		try {
			if (activity == null) {
				MTLog.w(this, "onDownloadOrOpenPaidTasksBtnClick() > skip (no view or no activity)");
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			if (PackageManagerUtils.isAppInstalled(context, PAID_TASKS_PKG)) {
				ToastUtils.makeTextAndShowCentered(context, //
						context.getString(R.string.opening_and_label, //
								context.getString(R.string.support_paid_tasks_incentive_app_label)));
				PackageManagerUtils.openApp(context, PAID_TASKS_PKG);
			} else {
				StoreUtils.viewAppPage(activity, PAID_TASKS_PKG, context.getString(R.string.google_play));
			}
			Dialog dialog = getDialog();
			if (dialog != null) {
				dialog.dismiss();
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while handling download or open paid tasks button!");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
		}
	}

	private void onBuyBtnClick(@NonNull Context context) {
		Activity activity = getActivity();
		try {
			View view = getView();
			if (view == null || activity == null) {
				MTLog.w(this, "onBuyBtnClick() > skip (no view or no activity)");
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			Spinner periodSpinner = view.findViewById(R.id.period);
			int periodPosition = periodSpinner.getSelectedItemPosition();
			String periodS = this.periods.get(periodPosition);
			if (TextUtils.isEmpty(periodS)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected period position: %s)", periodPosition);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			String periodCat = this.periodSToPeriodCat.get(periodS);
			if (TextUtils.isEmpty(periodCat)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected period string: %s)", periodS);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			Spinner priceSpinner = view.findViewById(R.id.price);
			int pricePosition = priceSpinner.getSelectedItemPosition();
			String priceS = this.prices.get(pricePosition);
			if (TextUtils.isEmpty(priceS)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected price position: %s)", pricePosition);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			String priceCat = this.priceSToPriceCat.get(priceS);
			if (TextUtils.isEmpty(priceCat)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected price string: %s)", priceS);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			Dialog dialog = getDialog();
			if (dialog != null) {
				dialog.dismiss();
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while handling buy button!");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
		}
	}

	@NonNull
	private final ThreadSafeDateFormatter dateFormatter = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	@Override
	public void onResume() {
		super.onResume();
		this.adManager.setRewardedAdListener(this);
		this.adManager.linkRewardedAd(this);
		this.adManager.refreshRewardedAdStatus(this);
		showLoading();
		View view = getView();
		if (view != null) {
			((Button) view.findViewById(R.id.downloadOrOpenPaidTasksBtn)).setText( //
					PackageManagerUtils.isAppInstalled(view.getContext(), PAID_TASKS_PKG) ? //
							R.string.support_paid_tasks_incentive_open_btn
							: R.string.support_paid_tasks_incentive_download_btn);
			refreshRewardedLayout(view);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		this.adManager.setRewardedAdListener(null);
	}

	private void refreshRewardedLayout(@NonNull View view) {
		final boolean availableToShow = this.adManager.isRewardedAdAvailableToShow();
		final boolean rewardedNow = this.adManager.isRewardedNow();
		final long rewardedUntilInMs = this.adManager.getRewardedUntilInMs();
		final int rewardedAmount = this.adManager.getRewardedAdAmount();

		final View rewardedDivider = view.findViewById(R.id.paidTasksDivider2);
		final TextView rewardedAdStatusTv = (TextView) view.findViewById(R.id.rewardedAdText);
		final Button rewardedAdsBtn = view.findViewById(R.id.rewardedAdsBtn);

		rewardedDivider.setVisibility(availableToShow || rewardedNow ? View.VISIBLE : View.GONE);

		if (rewardedNow) {
			rewardedAdStatusTv.setText(getString(
					R.string.support_watch_rewarded_ad_status_until_and_date,
					this.dateFormatter.formatThreadSafe(rewardedUntilInMs)
			));
			rewardedAdStatusTv.setVisibility(View.VISIBLE);
		} else {
			rewardedAdStatusTv.setVisibility(View.GONE);
			rewardedAdStatusTv.setText(null);
		}

		rewardedAdsBtn.setText(getString(
				rewardedNow ?
						R.string.support_watch_rewarded_ad_btn_more_and_days :
						R.string.support_watch_rewarded_ad_btn_and_days,
				rewardedAmount
		));
		if (availableToShow) { // only if NOT paying user
			rewardedAdsBtn.setEnabled(true);
			rewardedAdsBtn.setVisibility(View.VISIBLE);
		} else {
			rewardedAdsBtn.setEnabled(false); // keep but disable
		}
	}

	@Override
	public boolean skipRewardedAd() {
		if (!this.adManager.isRewardedNow()) {
			return false; // never skip for non-rewarded users
		}
		final long rewardedUntilInMs = this.adManager.getRewardedUntilInMs();
		final long skipRewardedAdUntilInMs = TimeUtils.currentTimeMillis()
				- TimeUnit.HOURS.toMillis(1L) // accounts for "recent" rewards
				+ this.adManager.getRewardedAdAmount() * this.adManager.getRewardedAdAmountInMs(); // not unlimited
		return rewardedUntilInMs > skipRewardedAdUntilInMs;
	}

	@Override
	public void onRewardedAdStatusChanged() {
		View view = getView();
		if (view != null) {
			refreshRewardedLayout(view);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.adManager.unlinkRewardedAd(this);
	}

	private void showLoading() {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.title).setVisibility(View.GONE);
			view.findViewById(R.id.subTitle).setVisibility(View.GONE);
			view.findViewById(R.id.beforeText).setVisibility(View.GONE);
			view.findViewById(R.id.priceSelection).setVisibility(View.GONE);
			view.findViewById(R.id.afterText).setVisibility(View.GONE);
			view.findViewById(R.id.buyBtn).setVisibility(View.GONE);
			view.findViewById(R.id.paidTasksDivider).setVisibility(View.GONE);
			view.findViewById(R.id.paidTasksIncentive).setVisibility(View.GONE);
			view.findViewById(R.id.downloadOrOpenPaidTasksBtn).setVisibility(View.GONE);
			view.findViewById(R.id.loading).setVisibility(View.VISIBLE);
		}
	}

	private void showNotLoading() {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.loading).setVisibility(View.GONE);
			view.findViewById(R.id.title).setVisibility(View.VISIBLE);
			view.findViewById(R.id.subTitle).setVisibility(View.VISIBLE);
			view.findViewById(R.id.beforeText).setVisibility(View.VISIBLE);
			view.findViewById(R.id.priceSelection).setVisibility(View.VISIBLE);
			view.findViewById(R.id.afterText).setVisibility(View.VISIBLE);
			view.findViewById(R.id.buyBtn).setVisibility(View.VISIBLE);
			view.findViewById(R.id.paidTasksDivider).setVisibility(View.VISIBLE);
			view.findViewById(R.id.paidTasksIncentive).setVisibility(View.VISIBLE);
			view.findViewById(R.id.downloadOrOpenPaidTasksBtn).setVisibility(View.VISIBLE);
		}
	}

	private final ArrayList<String> prices = new ArrayList<>();
	private final ArrayMap<String, String> priceSToPriceCat = new ArrayMap<>();
	private final ArrayList<String> periods = new ArrayList<>();
	private final ArrayMap<String, String> periodSToPeriodCat = new ArrayMap<>();

}
