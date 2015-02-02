package org.mtransit.android.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.util.iab.IabHelper;
import org.mtransit.android.util.iab.IabResult;
import org.mtransit.android.util.iab.Inventory;
import org.mtransit.android.util.iab.Purchase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public final class VendingUtils implements MTLog.Loggable {

	private static final String TAG = VendingUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final boolean FORCE_HAS_SUBSCRIPTION = false;
	private static final boolean FORCE_DO_NOT_HAVE_SUBSCRIPTION = false;

	public static final String MONTHLY_SUBSCRIPTION_SKU = "monthly_subscription";

	private static IabHelper mHelper;

	private static WeakReference<Context> contextWR;

	private static WeakHashMap<OnVendingResultListener, Object> listenersWR = new WeakHashMap<VendingUtils.OnVendingResultListener, Object>();

	private static void checkBilling(Context context, OnVendingResultListener listener) {
		addListener(listener);
		setContext(context);
		if (mHelper == null) {
			initBilling(context);
		}
		broadcastNewVendingResult(isHasSubscription(context));
	}

	private static void addListener(OnVendingResultListener listener) {
		listenersWR.put(listener, null);
	}

	private static void removeListener(OnVendingResultListener listener) {
		listenersWR.remove(listener);
	}

	private static void setContext(Context context) {
		contextWR = new WeakReference<Context>(context);
	}

	public static void onPause() {
		if (contextWR != null) {
			contextWR.clear();
			contextWR = null;
		}
	}

	public static void onResume(Context context, OnVendingResultListener listener) {
		checkBilling(context, listener);
	}

	private static void initBilling(Context context) {
		mHelper = new IabHelper(context, context.getString(R.string.google_play_license_key));
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {

			@Override
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess()) {
					MTLog.w(TAG, "Problem setting up in-app billing: " + result);
					return;
				}
				if (mHelper == null) return;
				mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {

					@Override
					public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
						if (mHelper == null) return;
						if (result.isFailure()) {
							MTLog.w(TAG, "Failed to query inventory: " + result);
							return;
						}
						Purchase monthlySubscriptionPurchase = inventory.getPurchase(MONTHLY_SUBSCRIPTION_SKU);
						Boolean mHasSubscription = monthlySubscriptionPurchase != null && verifyDeveloperPayload(monthlySubscriptionPurchase);
						setHasSubscription(mHasSubscription);
					}
				});
			}
		});
	}

	private static boolean verifyDeveloperPayload(Purchase purchase) {
		return true;
	}

	private static Boolean hasSubscription = null;

	private static void setHasSubscription(Boolean newHasSubscription) {
		if (FORCE_HAS_SUBSCRIPTION) {
			newHasSubscription = true;
		} else if (FORCE_DO_NOT_HAVE_SUBSCRIPTION) {
			newHasSubscription = false;
		}
		hasSubscription = newHasSubscription;
		broadcastNewVendingResult(hasSubscription);
		if (hasSubscription != null) {
			Context context = contextWR == null ? null : contextWR.get();
			if (context != null) {
				PreferenceUtils.savePrefLcl(context, PREF_KEY_HAS_SUBSCRIPTION, hasSubscription, false); // async
			}
		}
	}

	public static Boolean isHasSubscription(Context context) {
		if (hasSubscription == null) {
			if (PreferenceUtils.hasPrefLcl(context, PREF_KEY_HAS_SUBSCRIPTION)) {
				boolean newHasSubscription = PreferenceUtils.getPrefLcl(context, PREF_KEY_HAS_SUBSCRIPTION, PREF_KEY_HAS_SUBSCRIPTION_DEFAULT);
				if (FORCE_HAS_SUBSCRIPTION) {
					newHasSubscription = true;
				} else if (FORCE_DO_NOT_HAVE_SUBSCRIPTION) {
					newHasSubscription = false;
				}
				hasSubscription = newHasSubscription;
			}
		}
		return hasSubscription;
	}

	private static void broadcastNewVendingResult(Boolean newHasSubscription) {
		Iterator<OnVendingResultListener> it = listenersWR.keySet().iterator();
		while (it.hasNext()) {
			OnVendingResultListener listener = it.next();
			if (listener != null) {
				listener.onVendingResult(newHasSubscription);
			}
		}
	}

	private static final int RC_REQUEST = 10001;

	public static void purchase(Activity activity, String sku) {
		if (mHelper != null && mHelper.subscriptionsSupported()) {
			String payload = "";
			mHelper.launchPurchaseFlow(activity, sku, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {

				public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
					Context context = contextWR == null ? null : contextWR.get();
					if (mHelper == null) return;
					if (result.isFailure()) {
						MTLog.w(TAG, "onIabPurchaseFinished() > Error purchasing: " + result);
						if (context != null) {
							int resId = R.string.support_subs_default_failure_message;
							if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
								resId = R.string.support_subs_user_canceled_message;
							}
							ToastUtils.makeTextAndShowCentered(context, resId);
						}
						return;
					}
					if (!verifyDeveloperPayload(purchase)) {
						MTLog.w(TAG, "onIabPurchaseFinished() > Error purchasing. Authenticity verification failed.");
						if (context != null) {
							int resId = R.string.support_subs_authenticity_check_fail_message;
							ToastUtils.makeTextAndShowCentered(context, resId, Toast.LENGTH_LONG);
						}
						return;
					}
					String purchasedSku = purchase.getSku();
					if (purchasedSku.equals(MONTHLY_SUBSCRIPTION_SKU)) {
						if (context != null) {
							int resId = R.string.support_subs_purchase_successful_message;
							ToastUtils.makeTextAndShowCentered(context, resId, Toast.LENGTH_LONG);
						}
						setHasSubscription(true);
					}
				}

			}, payload);
		}
	}

	public static boolean onActivityResult(Context context, int requestCode, int resultCode, Intent data) {
		setContext(context);
		if (mHelper != null && mHelper.handleActivityResult(requestCode, resultCode, data)) {
			return true; // handled
		}
		return false; // not handled
	}

	private static final String PREF_KEY_HAS_SUBSCRIPTION = "pHasSubscription";

	private static final boolean PREF_KEY_HAS_SUBSCRIPTION_DEFAULT = false;

	public static void destroyBilling(OnVendingResultListener listener) {
		removeListener(listener);
		if (listenersWR.isEmpty()) {
			if (mHelper != null) {
				mHelper.dispose();
				mHelper = null;
			}
		}
	}

	public static interface OnVendingResultListener {
		public void onVendingResult(Boolean hasSubscription);
	}

}
