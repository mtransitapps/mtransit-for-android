package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.util.VendingUtils;
import org.mtransit.android.util.iab.IabHelper;
import org.mtransit.android.util.iab.IabResult;
import org.mtransit.android.util.iab.Inventory;
import org.mtransit.android.util.iab.SkuDetails;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class PurchaseDialogFragment extends MTDialogFragment implements IabHelper.QueryInventoryFinishedListener {

	private static final String TAG = PurchaseDialogFragment.class.getSimpleName();

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
				onBuyBtnClick();
			}

		});
	}

	private void onBuyBtnClick() {
		try {
			View view = getView();
			if (view == null) {
				MTLog.w(this, "onBuyBtnClick() > skip (no view)");
				ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
				return;
			}
			Spinner periodSpinner = (Spinner) view.findViewById(R.id.period);
			int periodPosition = periodSpinner.getSelectedItemPosition();
			String periodS = this.periods.get(periodPosition);
			if (TextUtils.isEmpty(periodS)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected period position: %s)", periodPosition);
				ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
				return;
			}
			String periodCat = this.periodSToPeriodCat.get(periodS);
			if (TextUtils.isEmpty(periodCat)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected period string: %s)", periodS);
				ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
				return;
			}
			Spinner priceSpinner = (Spinner) view.findViewById(R.id.price);
			int pricePosition = priceSpinner.getSelectedItemPosition();
			String priceS = this.prices.get(pricePosition);
			if (TextUtils.isEmpty(priceS)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected price position: %s)", pricePosition);
				ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
				return;
			}
			String priceCat = this.priceSToPriceCat.get(priceS);
			if (TextUtils.isEmpty(priceCat)) {
				MTLog.w(this, "onBuyBtnClick() > skip (unexpected price string: %s)", priceS);
				ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
				return;
			}
			String sku = "f_" + periodCat + "_subscription_" + priceCat;
			if (!VendingUtils.AVAILABLE_SUBSCRIPTIONS.contains(sku)) {
				MTLog.w(this, "onClick() > skip (unexpected sku: %s)", sku);
				ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
				return;
			}
			VendingUtils.purchase(getActivity(), sku);
			Dialog dialog = getDialog();
			if (dialog != null) {
				getDialog().dismiss();
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while handling buy button!");
			ToastUtils.makeTextAndShowCentered(getActivity(), R.string.support_subs_default_failure_message);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		VendingUtils.getInventory(this);
		showLoading();
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
		}
	}

	private static final String WEEKLY = "weekly";
	private static final String MONTHLY = "monthly";
	private static final String YEARLY = "yearly";

	private static final ArrayList<String> SORTED_PERIOD_CAT = ArrayUtils.asArrayList(new String[] { WEEKLY, MONTHLY, YEARLY });

	private static final HashMap<String, Integer> PERIOD_RES_ID;
	static {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put(WEEKLY, R.string.support_every_week);
		map.put(MONTHLY, R.string.support_every_month);
		map.put(YEARLY, R.string.support_every_year);
		PERIOD_RES_ID = map;
	}

	private static final String DEFAULT_PRICE_CAT = "1";

	private static final String DEFAULT_PERIOD_CAT = MONTHLY;

	private ArrayList<String> prices = new ArrayList<String>();
	private HashMap<String, String> priceSToPriceCat = new HashMap<String, String>();
	private ArrayList<String> periods = new ArrayList<String>();
	private HashMap<String, String> periodSToPeriodCat = new HashMap<String, String>();

	@Override
	public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
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
			if (!sku.startsWith("f_")) {
				continue;
			}
			if (!inventory.hasDetails(sku)) {
				continue;
			}
			SkuDetails skuDetails = inventory.getSkuDetails(sku);
			String periodCat = sku.substring("f_".length(), sku.indexOf("_subscription_", "f_".length()));
			if (!PERIOD_RES_ID.containsKey(periodCat)) {
				MTLog.w(this, "Skip sku %s (unknown periodCat: %s)", sku, periodCat);
				continue;
			}
			String priceCat = sku.substring(sku.indexOf("_subscription_") + "_subscription_".length());
			String priceS = skuDetails.getPrice();
			this.priceSToPriceCat.put(priceS, priceCat);
			if (!this.prices.contains(priceS)) {
				this.prices.add(priceS);
			}
			String periodS = getString(PERIOD_RES_ID.get(periodCat));
			if (!this.periods.contains(periodS)) {
				this.periods.add(periodS);
			}
			this.periodSToPeriodCat.put(periodS, periodCat);
			if (DEFAULT_PRICE_CAT.equals(priceCat)) {
				defaultPriceS = priceS;
			}
			if (DEFAULT_PERIOD_CAT.equals(periodCat)) {
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
		View view = getView();
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
