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
import android.widget.ArrayAdapter;
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
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.di.Injection;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.android.util.VendingUtils;
import org.mtransit.android.util.iab.IabHelper;
import org.mtransit.android.util.iab.IabResult;
import org.mtransit.android.util.iab.Inventory;
import org.mtransit.android.util.iab.SkuDetails;

import java.util.ArrayList;
import java.util.Collections;

public class PurchaseDialogFragment extends MTDialogFragment implements IActivity, IabHelper.QueryInventoryFinishedListener, IAdManager.RewardedAdListener {

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
			String sku = VendingUtils.SKU_STARTS_WITH_F + periodCat + VendingUtils.SKU_SUBSCRIPTION + priceCat;
			if (!VendingUtils.AVAILABLE_SUBSCRIPTIONS.contains(sku)) {
				MTLog.w(this, "onClick() > skip (unexpected sku: %s)", sku);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			VendingUtils.purchase(activity, sku);
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
		VendingUtils.getInventory(this);
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

	private ArrayList<String> prices = new ArrayList<>();
	private ArrayMap<String, String> priceSToPriceCat = new ArrayMap<>();
	private ArrayList<String> periods = new ArrayList<>();
	private ArrayMap<String, String> periodSToPeriodCat = new ArrayMap<>();

	@Override
	public void onQueryInventoryFinished(@Nullable IabResult result, @Nullable Inventory inventory) {
		View view = getView();
		Activity activity = getActivity();
		if (view == null || activity == null) {
			return;
		}
		if (result == null || result.isFailure() || inventory == null) {
			MTLog.w(this, "Failed to query inventory: %s (%s)", result, inventory);
			ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
			return;
		}
		this.prices.clear();
		this.periods.clear();
		this.priceSToPriceCat.clear();
		this.periodSToPeriodCat.clear();
		String defaultPriceS = null;
		String defaultPeriodS = null;
		for (String sku : inventory.getAllSkus()) {
			if (!sku.startsWith(VendingUtils.SKU_STARTS_WITH_F)) {
				continue;
			}
			if (!inventory.hasDetails(sku)) {
				continue;
			}
			SkuDetails skuDetails = inventory.getSkuDetails(sku);
			String periodCat = sku.substring(VendingUtils.SKU_STARTS_WITH_F.length(),
					sku.indexOf(VendingUtils.SKU_SUBSCRIPTION, VendingUtils.SKU_STARTS_WITH_F.length()));
			if (!VendingUtils.PERIOD_RES_ID.containsKey(periodCat)) {
				MTLog.w(this, "Skip sku %s (unknown periodCat: %s)", sku, periodCat);
				continue;
			}
			String priceCat = sku.substring(sku.indexOf(VendingUtils.SKU_SUBSCRIPTION) + VendingUtils.SKU_SUBSCRIPTION.length());
			String priceS = skuDetails.getPrice();
			this.priceSToPriceCat.put(priceS, priceCat);
			if (!this.prices.contains(priceS)) {
				this.prices.add(priceS);
			}
			String periodS = activity.getString(VendingUtils.PERIOD_RES_ID.get(periodCat));
			if (!this.periods.contains(periodS)) {
				this.periods.add(periodS);
			}
			this.periodSToPeriodCat.put(periodS, periodCat);
			if (VendingUtils.DEFAULT_PRICE_CAT.equals(priceCat)) {
				defaultPriceS = priceS;
			}
			if (VendingUtils.DEFAULT_PERIOD_CAT.equals(periodCat)) {
				defaultPeriodS = periodS;
			}
		}
		Collections.sort(this.periods, (lPeriodS, rPeriodS) -> {
			try {
				String lPriceCat = PurchaseDialogFragment.this.periodSToPeriodCat.get(lPeriodS);
				int lIndexOf = VendingUtils.SORTED_PERIOD_CAT.indexOf(lPriceCat);
				String rPriceCat = PurchaseDialogFragment.this.periodSToPeriodCat.get(rPeriodS);
				int rIndexOf = VendingUtils.SORTED_PERIOD_CAT.indexOf(rPriceCat);
				return lIndexOf - rIndexOf;
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while sorting periods!");
				return 0;
			}
		});
		Collections.sort(this.prices, (lPriceS, rPeriods) -> {
			try {
				String lPriceCat = PurchaseDialogFragment.this.priceSToPriceCat.get(lPriceS);
				int lIndexOf = lPriceCat == null || !TextUtils.isDigitsOnly(lPriceCat) ? -1 : Integer.parseInt(lPriceCat);
				String rPriceCat = PurchaseDialogFragment.this.priceSToPriceCat.get(rPeriods);
				int rIndexOf = rPriceCat == null || !TextUtils.isDigitsOnly(rPriceCat) ? -1 : Integer.parseInt(rPriceCat);
				return lIndexOf - rIndexOf;
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while sorting prices!");
				return 0;
			}
		});
		Spinner priceSpinner = view.findViewById(R.id.price);
		priceSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, this.prices));
		if (defaultPriceS != null) {
			priceSpinner.setSelection(this.prices.indexOf(defaultPriceS));
		}
		Spinner periodSpinner = view.findViewById(R.id.period);
		periodSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, this.periods));
		if (defaultPeriodS != null) {
			periodSpinner.setSelection(this.periods.indexOf(defaultPeriodS));
		}
		showNotLoading();
	}
}
