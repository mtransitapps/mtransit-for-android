package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.mtransit.android.R;
import org.mtransit.android.billing.IBillingManager;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.di.Injection;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;


public class PurchaseDialogFragment extends MTDialogFragment implements IBillingManager.OnBillingInventoryListener {

	private static final String LOG_TAG = PurchaseDialogFragment.class.getSimpleName();

	public static final String PAID_TASKS_PKG = "com.google.android.apps.paidtasks";

	public static final ArrayList<String> SORTED_PERIOD_CAT = ArrayUtils.asArrayList(
			IBillingManager.WEEKLY,
			IBillingManager.MONTHLY,
			IBillingManager.YEARLY
	);

	public static final ArrayMap<String, Integer> PERIOD_RES_ID;
	static {
		ArrayMap<String, Integer> map = new ArrayMap<>();
		map.put(IBillingManager.WEEKLY, R.string.support_every_week);
		map.put(IBillingManager.MONTHLY, R.string.support_every_month);
		map.put(IBillingManager.YEARLY, R.string.support_every_year);
		PERIOD_RES_ID = map;
	}

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static PurchaseDialogFragment newInstance() {
		return new PurchaseDialogFragment();
	}

	@NonNull
	private final IBillingManager billingManager;

	public PurchaseDialogFragment() {
		super();
		this.billingManager = Injection.providesBillingManager();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_dialog_purchase, container, false);
		setupView(view);
		return view;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_user_canceled_message);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		view.findViewById(R.id.buyBtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBuyBtnClick(v.getContext());
			}

		});
		view.findViewById(R.id.downloadOrOpenPaidTasksBtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onDownloadOrOpenPaidTasksBtnClick(v.getContext());
			}
		});
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
			String sku = this.billingManager.getSku(periodCat, priceCat);
			if (!this.billingManager.isSkuAvailableForPurchase(sku)) {
				MTLog.w(this, "onClick() > skip (unexpected sku: %s)", sku);
				ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
				return;
			}
			this.billingManager.launchPurchase(activity, sku);
			Dialog dialog = getDialog();
			if (dialog != null) {
				dialog.dismiss();
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while handling buy button!");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_subs_default_failure_message);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		this.billingManager.queryInventoryAsync(this);
		showLoading();
		View view = getView();
		if (view != null) {
			((Button) view.findViewById(R.id.downloadOrOpenPaidTasksBtn)).setText( //
					PackageManagerUtils.isAppInstalled(view.getContext(), PAID_TASKS_PKG) ? //
							R.string.support_paid_tasks_incentive_open_btn
							: R.string.support_paid_tasks_incentive_download_btn);
		}
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

	private List<String> prices = new ArrayList<>();
	private Map<String, String> priceSToPriceCat = new ArrayMap<>();
	private List<String> periods = new ArrayList<>();
	private Map<String, String> periodSToPeriodCat = new ArrayMap<>();

	@Override
	public void onBillingInventoryResult(@NonNull List<Pair<String, String>> skuAndPriceList) {
		View view = getView();
		Activity activity = getActivity();
		if (view == null || activity == null) {
			return;
		}
		if (skuAndPriceList.isEmpty()) {
			MTLog.w(this, "Inventory empty!");
			ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
			return;
		}
		this.prices.clear();
		this.periods.clear();
		this.priceSToPriceCat.clear();
		this.periodSToPeriodCat.clear();
		String defaultPriceS = null;
		String defaultPeriodS = null;
		for (Pair<String, String> skuAndPrice : skuAndPriceList) {
			String sku = skuAndPrice.first;
			String priceS = skuAndPrice.second;
			if (sku == null
					|| priceS == null
					|| !this.billingManager.isSkuAvailableForPurchase(sku)) {
				continue;
			}
			String periodCat = sku.substring(IBillingManager.SKU_STARTS_WITH_F.length(),
					sku.indexOf(IBillingManager.SKU_SUBSCRIPTION, IBillingManager.SKU_STARTS_WITH_F.length()));
			Integer periodStringResId = PERIOD_RES_ID.get(periodCat);
			if (periodStringResId == null) {
				MTLog.w(this, "Skip sku %s (unknown period category: %s)", sku, periodCat);
				continue;
			}
			String priceCat = sku.substring(sku.indexOf(IBillingManager.SKU_SUBSCRIPTION) + IBillingManager.SKU_SUBSCRIPTION.length());
			this.priceSToPriceCat.put(priceS, priceCat);
			if (!this.prices.contains(priceS)) {
				this.prices.add(priceS);
			}
			String periodS = getString(periodStringResId);
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
		Collections.sort(this.periods, new Comparator<String>() {
			@Override
			public int compare(String lPeriodS, String rPeriodS) {
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
			}
		});
		Collections.sort(this.prices, new Comparator<String>() {
			@Override
			public int compare(String lPriceS, String rPeriods) {
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

	@Override
	public void onUserCancelled() {
		MTLog.w(this, "Failed to query inventory (user cancelled).");
		ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
	}

	@Override
	public void onUnexpectedError() {
		MTLog.w(this, "Failed to query inventory (unexpected error).");
		ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
	}
}
