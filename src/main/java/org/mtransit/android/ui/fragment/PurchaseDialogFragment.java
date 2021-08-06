package org.mtransit.android.ui.fragment;

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
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.android.billingclient.api.SkuDetails;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.billing.IBillingManager;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.ui.view.common.IActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;

@SuppressWarnings("deprecation")
public class PurchaseDialogFragment extends MTDialogFragment implements IActivity, IAdManager.RewardedAdListener {

	private static final String LOG_TAG = PurchaseDialogFragment.class.getSimpleName();

	private static final String PAID_TASKS_PKG = "com.google.android.apps.paidtasks";

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static PurchaseDialogFragment newInstance() {
		return new PurchaseDialogFragment();
	}

	@Nullable
	private Observer<Map<String, SkuDetails>> newSkuObserver;

	// TODO migrate to 100% Hilt after migrating to AndroidX
	// TODO @InstallIn(FragmentComponent.class) ?
	@EntryPoint
	@InstallIn(SingletonComponent.class)
	interface PurchaseDialogEntryPoint {
		IBillingManager billingManager();

		IAdManager adManager();
	}

	@NonNull
	private PurchaseDialogEntryPoint getEntryPoint(@NonNull Context context) {
		return EntryPointAccessors.fromApplication(context.getApplicationContext(), PurchaseDialogEntryPoint.class);
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
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.newSkuObserver = this::onNewSku;
		getEntryPoint(requireContext()).billingManager().getSkusWithSkuDetails().observeForever(this.newSkuObserver); // NOT ANDROID X
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
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

	@NonNull
	@Override
	public LifecycleOwner getLifecycleOwner() {
		throw new IllegalStateException("Fragment " + this + " is NOT compatible with Lifecycle!"); // NOT ANDROID X
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.newSkuObserver != null) {
			getEntryPoint(requireContext()).billingManager().getSkusWithSkuDetails().removeObserver(this.newSkuObserver);
		}
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
			if (!getEntryPoint(context).adManager().isRewardedAdAvailableToShow()) {
				MTLog.w(this, "onRewardedAdButtonClick() > skip (no ad available)");
				ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_not_ready);
				return;
			}
			getEntryPoint(context).adManager().showRewardedAd(this);
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
			if (periodS == null || periodS.isEmpty()) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected period position: %s)", periodPosition);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			String periodCat = this.periodSToPeriodCat.get(periodS);
			if (periodCat == null || periodCat.isEmpty()) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected period string: %s)", periodS);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			Spinner priceSpinner = view.findViewById(R.id.price);
			int pricePosition = priceSpinner.getSelectedItemPosition();
			String priceS = this.prices.get(pricePosition);
			if (priceS == null || priceS.isEmpty()) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected price position: %s)", pricePosition);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			String priceCat = this.priceSToPriceCat.get(priceS);
			if (priceCat == null || priceCat.isEmpty()) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected price string: %s)", priceS);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			String sku = IBillingManager.SKU_STARTS_WITH_F + periodCat + IBillingManager.SKU_SUBSCRIPTION + priceCat;
			if (!IBillingManager.AVAILABLE_SUBSCRIPTIONS.contains(sku)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected sku: %s)", sku);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			final boolean billingFlowLaunched = getEntryPoint(context).billingManager().launchBillingFlow(this, sku);
			if (!billingFlowLaunched) {
				MTLog.w(this, "onBuyBtnClick() > skip (can not launch billing flow for: %s)", sku);
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
		final PurchaseDialogEntryPoint entryPoint = getEntryPoint(requireContext());
		entryPoint.billingManager().refreshAvailableSubscriptions();
		final IAdManager iAdManager = entryPoint.adManager();
		iAdManager.setRewardedAdListener(this);
		iAdManager.linkRewardedAd(this);
		iAdManager.refreshRewardedAdStatus(this);
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
		getEntryPoint(requireContext()).adManager().setRewardedAdListener(null);
	}

	private void refreshRewardedLayout(@NonNull View view) {
		final IAdManager adManager = getEntryPoint(view.getContext()).adManager();
		final boolean availableToShow = adManager.isRewardedAdAvailableToShow();
		final boolean rewardedNow = adManager.isRewardedNow();
		final long rewardedUntilInMs = adManager.getRewardedUntilInMs();
		final int rewardedAmount = adManager.getRewardedAdAmount();

		final View rewardedDivider = view.findViewById(R.id.paidTasksDivider2);
		final TextView rewardedAdStatusTv = view.findViewById(R.id.rewardedAdText);
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
		final IAdManager adManager = getEntryPoint(requireContext()).adManager();
		if (!adManager.isRewardedNow()) {
			return false; // never skip for non-rewarded users
		}
		final long rewardedUntilInMs = adManager.getRewardedUntilInMs();
		final long skipRewardedAdUntilInMs = TimeUtils.currentTimeMillis()
				- TimeUnit.HOURS.toMillis(1L) // accounts for "recent" rewards
				+ adManager.getRewardedAdAmount() * adManager.getRewardedAdAmountInMs(); // not unlimited
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
		getEntryPoint(requireContext()).adManager().unlinkRewardedAd(this);
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
			view.findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
		}
	}

	private void showNotLoading() {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.loading_layout).setVisibility(View.GONE);
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

	private static final ArrayList<String> SORTED_PERIOD_CAT = ArrayUtils.asArrayList(
			IBillingManager.WEEKLY,
			IBillingManager.MONTHLY,
			IBillingManager.YEARLY
	);

	private static final ArrayMap<String, Integer> PERIOD_RES_ID;

	static {
		ArrayMap<String, Integer> map = new ArrayMap<>();
		map.put(IBillingManager.WEEKLY, R.string.support_every_week);
		map.put(IBillingManager.MONTHLY, R.string.support_every_month);
		map.put(IBillingManager.YEARLY, R.string.support_every_year);
		PERIOD_RES_ID = map;
	}

	private final ArrayList<String> prices = new ArrayList<>();
	private final ArrayMap<String, String> priceSToPriceCat = new ArrayMap<>();
	private final ArrayList<String> periods = new ArrayList<>();
	private final ArrayMap<String, String> periodSToPeriodCat = new ArrayMap<>();

	private void onNewSku(@Nullable Map<String, SkuDetails> skusWithSkuDetails) {
		View view = getView();
		Activity activity = getActivity();
		if (view == null || activity == null) {
			return;
		}
		if (skusWithSkuDetails == null) {
			return;
		}
		this.prices.clear();
		this.periods.clear();
		this.priceSToPriceCat.clear();
		this.periodSToPeriodCat.clear();
		String defaultPriceS = null;
		String defaultPeriodS = null;
		for (String sku : skusWithSkuDetails.keySet()) {
			if (!sku.startsWith(IBillingManager.SKU_STARTS_WITH_F)) {
				continue;
			}
			SkuDetails skuDetails = skusWithSkuDetails.get(sku);
			if (skuDetails == null) {
				continue;
			}
			String periodCat = sku.substring(
					IBillingManager.SKU_STARTS_WITH_F.length(),
					sku.indexOf(IBillingManager.SKU_SUBSCRIPTION, IBillingManager.SKU_STARTS_WITH_F.length())
			);
			final Integer resId = PERIOD_RES_ID.get(periodCat);
			if (resId == null) {
				MTLog.w(this, "Skip sku %s (unknown periodCat: %s)", sku, periodCat);
				continue;
			}
			String priceCat = sku.substring(sku.indexOf(IBillingManager.SKU_SUBSCRIPTION) + IBillingManager.SKU_SUBSCRIPTION.length());
			String priceS = skuDetails.getPrice();
			this.priceSToPriceCat.put(priceS, priceCat);
			if (!this.prices.contains(priceS)) {
				this.prices.add(priceS);
			}
			String periodS = activity.getString(resId);
			if (!this.periods.contains(periodS)) {
				this.periods.add(periodS);
			}
			this.periodSToPeriodCat.put(periodS, periodCat);
			if (IBillingManager.DEFAULT_PRICE_CAT.equals(priceCat)) {
				defaultPriceS = priceS;
			}
			if (IBillingManager.DEFAULT_PERIOD_CAT.equals(periodCat)) {
				defaultPeriodS = periodS;
			}
		}
		Collections.sort(this.periods, (lPeriodS, rPeriodS) -> {
			try {
				String lPriceCat = PurchaseDialogFragment.this.periodSToPeriodCat.get(lPeriodS);
				int lIndexOf = SORTED_PERIOD_CAT.indexOf(lPriceCat);
				String rPriceCat = PurchaseDialogFragment.this.periodSToPeriodCat.get(rPeriodS);
				int rIndexOf = SORTED_PERIOD_CAT.indexOf(rPriceCat);
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
		priceSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, this.prices));
		if (defaultPriceS != null) {
			priceSpinner.setSelection(this.prices.indexOf(defaultPriceS));
		}
		Spinner periodSpinner = view.findViewById(R.id.period);
		periodSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, this.periods));
		if (defaultPeriodS != null) {
			periodSpinner.setSelection(this.periods.indexOf(defaultPeriodS));
		}
		showNotLoading();
	}
}
