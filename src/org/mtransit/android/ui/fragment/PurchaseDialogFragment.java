package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.util.VendingUtils;
import org.mtransit.android.util.iab.IabHelper;
import org.mtransit.android.util.iab.IabResult;
import org.mtransit.android.util.iab.Inventory;
import org.mtransit.android.util.iab.SkuDetails;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class PurchaseDialogFragment extends MTDialogFragment implements IabHelper.QueryInventoryFinishedListener {

	private static final String TAG = PurchaseDialogFragment.class.getSimpleName();

	public static final String PAID_TASKS_PKG = "com.google.android.apps.paidtasks";

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static PurchaseDialogFragment newInstance() {
		PurchaseDialogFragment f = new PurchaseDialogFragment();
		return f;
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
			Spinner periodSpinner = (Spinner) view.findViewById(R.id.period);
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
			Spinner priceSpinner = (Spinner) view.findViewById(R.id.price);
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

	@Override
	public void onResume() {
		super.onResume();
		VendingUtils.getInventory(this);
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

	private ArrayList<String> prices = new ArrayList<String>();
	private ArrayMap<String, String> priceSToPriceCat = new ArrayMap<String, String>();
	private ArrayList<String> periods = new ArrayList<String>();
	private ArrayMap<String, String> periodSToPeriodCat = new ArrayMap<String, String>();

	@Override
	public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
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
		Collections.sort(this.periods, new Comparator<String>() {
			@Override
			public int compare(String lPeriodS, String rPeriodS) {
				try {
					String lPriceCat = PurchaseDialogFragment.this.periodSToPeriodCat.get(lPeriodS);
					int lIndexOf = VendingUtils.SORTED_PERIOD_CAT.indexOf(lPriceCat);
					String rPriceCat = PurchaseDialogFragment.this.periodSToPeriodCat.get(rPeriodS);
					int rIndexOf = VendingUtils.SORTED_PERIOD_CAT.indexOf(rPriceCat);
					return lIndexOf - rIndexOf;
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while sorting periods!");
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
					MTLog.w(TAG, e, "Error while sorting prices!");
					return 0;
				}
			}
		});
		Spinner priceSpinner = (Spinner) view.findViewById(R.id.price);
		priceSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, this.prices));
		if (defaultPriceS != null) {
			priceSpinner.setSelection(this.prices.indexOf(defaultPriceS));
		}
		Spinner periodSpinner = (Spinner) view.findViewById(R.id.period);
		periodSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, this.periods));
		if (defaultPeriodS != null) {
			periodSpinner.setSelection(this.periods.indexOf(defaultPeriodS));
		}
		showNotLoading();
	}
}
