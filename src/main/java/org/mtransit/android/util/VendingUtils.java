package org.mtransit.android.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.ui.fragment.PurchaseDialogFragment;
import org.mtransit.android.util.iab.IabHelper;
import org.mtransit.android.util.iab.Purchase;
import org.mtransit.android.util.iab.SkuDetails;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import android.os.Build;
import android.widget.Toast;

public final class VendingUtils implements MTLog.Loggable {

	private static final String LOG_TAG = VendingUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final boolean FORCE_HAS_SUBSCRIPTION = false;

	private static final boolean FORCE_DO_NOT_HAVE_SUBSCRIPTION = false;

	public static final String SKU_SUBSCRIPTION = "_subscription_";

	public static final String SKU_STARTS_WITH_F = "f_";

	private static final String WEEKLY = "weekly";
	private static final String MONTHLY = "monthly";
	private static final String YEARLY = "yearly";

	public static final ArrayList<String> SORTED_PERIOD_CAT = ArrayUtils.asArrayList(WEEKLY, MONTHLY, YEARLY);

	public static final ArrayMap<String, Integer> PERIOD_RES_ID;
	static {
		ArrayMap<String, Integer> map = new ArrayMap<>();
		map.put(WEEKLY, R.string.support_every_week);
		map.put(MONTHLY, R.string.support_every_month);
		map.put(YEARLY, R.string.support_every_year);
		PERIOD_RES_ID = map;
	}

	public static final String DEFAULT_PRICE_CAT = "1";

	public static final String DEFAULT_PERIOD_CAT = MONTHLY;

	public static final ArrayList<String> AVAILABLE_SUBSCRIPTIONS;
	static {
		ArrayList<String> list = new ArrayList<>();
		list.add(SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "1");
		list.add(SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "2");
		list.add(SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "3");
		list.add(SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "4");
		list.add(SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "5");
		list.add(SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "7");
		list.add(SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "10");
		list.add(SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "1");
		list.add(SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "2");
		list.add(SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "3");
		list.add(SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "4");
		list.add(SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "5");
		list.add(SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "7");
		list.add(SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "10");
		list.add(SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "1");
		list.add(SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "2");
		list.add(SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "3");
		list.add(SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "4");
		list.add(SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "5");
		list.add(SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "7");
		list.add(SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "10");
		AVAILABLE_SUBSCRIPTIONS = list;
	}

	public static final ArrayList<String> ALL_VALID_SUBSCRIPTIONS;
	static {
		ArrayList<String> set = new ArrayList<>();
		set.add("weekly_subscription"); // Inactive
		set.add("monthly_subscription"); // Active - offered by default for months
		set.add("yearly_subscription"); // Active - never offered
		set.addAll(AVAILABLE_SUBSCRIPTIONS);
		ALL_VALID_SUBSCRIPTIONS = set;
	}

	private static IabHelper mHelper;

	private static WeakReference<Context> contextWR;

	private static WeakHashMap<OnVendingResultListener, Object> listenersWR = new WeakHashMap<>();

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
		contextWR = new WeakReference<>(context);
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
		mHelper.startSetup(result -> {
			if (!result.isSuccess()) {
				MTLog.w(LOG_TAG, "Problem setting up in-app billing: " + result);
				if (Build.DEVICE.startsWith("generic")) { // EMULATOR
					setHasSubscription(false);
				}
				return;
			}
			if (mHelper == null) {
				return;
			}
			mHelper.queryInventoryAsync((result1, inventory) -> {
				if (mHelper == null) {
					return;
				}
				if (result1.isFailure()) {
					MTLog.w(LOG_TAG, "Failed to query inventory: %s", result1);
					return;
				}
				List<String> allOwnedSkus = inventory.getAllOwnedSkus(IabHelper.ITEM_TYPE_SUBS);
				Boolean mHasSubscription = Boolean.FALSE;
				if (allOwnedSkus != null) {
					for (String ownedSku : allOwnedSkus) {
						if (ALL_VALID_SUBSCRIPTIONS.contains(ownedSku)) {
							Purchase subscriptionPurchase = inventory.getPurchase(ownedSku);
							if (subscriptionPurchase != null && verifyDeveloperPayload(subscriptionPurchase)) {
								mHasSubscription = Boolean.TRUE;
								break;
							}
						}
					}
				}
				setHasSubscription(mHasSubscription);
			});
		});
	}

	private static boolean verifyDeveloperPayload(Purchase purchase) {
		return true;
	}

	@Nullable
	private static Boolean hasSubscription = null;

	private static void setHasSubscription(@Nullable Boolean newHasSubscription) {
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

	@Nullable
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
		for (OnVendingResultListener listener : listenersWR.keySet()) {
			if (listener != null) {
				listener.onVendingResult(newHasSubscription);
			}
		}
	}

	public static void purchase(Activity activity) {
		FragmentUtils.replaceDialogFragment(activity, FragmentUtils.DIALOG_TAG, PurchaseDialogFragment.newInstance(), null);
	}


	public static void getInventory(IabHelper.QueryInventoryFinishedListener listener) {
		try {
			if (mHelper != null && mHelper.subscriptionsSupported()) {
				mHelper.queryInventoryAsync(true, null, AVAILABLE_SUBSCRIPTIONS, listener);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while gettting inventory!");
		}
	}

	@Deprecated
	public static void logInventory(Activity activity) {
		MTLog.i(LOG_TAG, "logInventory(%s)", activity);
		try {
			MTLog.i(LOG_TAG, "logInventory() > mHelper: %s", mHelper);
			if (mHelper != null && mHelper.subscriptionsSupported()) {
				MTLog.i(LOG_TAG, "logInventory() > Query inventory...");
				mHelper.queryInventoryAsync(true, null, AVAILABLE_SUBSCRIPTIONS, (result, inventory) -> {
					MTLog.i(LOG_TAG, "onQueryInventoryFinished(%s,%s)", result, inventory);
					MTLog.i(LOG_TAG, "Query inventory finished.");
					if (result == null || result.isFailure() || inventory == null) {
						MTLog.w(LOG_TAG, "Failed to query inventory: %s (%s)", result, inventory);
						return;
					}
					MTLog.i(LOG_TAG, "Query inventory was successful.");
					MTLog.i(LOG_TAG, "all sku...");
					for (String sku : inventory.getAllSkus()) {
						if (!inventory.hasDetails(sku)) {
							MTLog.i(LOG_TAG, "Skip sku %s (no details)", sku);
							continue;
						}
						SkuDetails skuDetails = inventory.getSkuDetails(sku);
						MTLog.i(LOG_TAG, "sku: %s: %s", sku, skuDetails);
					}
					MTLog.i(LOG_TAG, "all sku... DONE");
					MTLog.i(LOG_TAG, "all owned sku...");
					for (String sku : inventory.getAllOwnedSkus()) {
						if (!inventory.hasDetails(sku)) {
							MTLog.i(LOG_TAG, "Skip sku %s (no details)", sku);
							continue;
						}
						SkuDetails skuDetails = inventory.getSkuDetails(sku);
						MTLog.i(LOG_TAG, "sku: %s: %s", sku, skuDetails);
					}
					MTLog.i(LOG_TAG, "all owned sku... DONE");
					MTLog.i(LOG_TAG, "all purchase...");
					for (Purchase purchase : inventory.getAllPurchases()) {
						MTLog.i(LOG_TAG, "purchase: %s", purchase);
					}
					MTLog.i(LOG_TAG, "all purchase... DONE");
				});
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while logging inventory!");
		}
	}

	private static final int RC_REQUEST = 10001;

	public static void purchase(@NonNull Activity activity, @NonNull String sku) {
		if (mHelper != null && mHelper.subscriptionsSupported()) {
			String payload = "";
			mHelper.launchPurchaseFlow(activity, sku, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST, (result, purchase) -> {
				Context context = contextWR == null ? null : contextWR.get();
				if (mHelper == null) {
					return;
				}
				if (result.isFailure()) {
					MTLog.w(LOG_TAG, "onIabPurchaseFinished() > Error purchasing: %s", result);
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
					MTLog.w(LOG_TAG, "onIabPurchaseFinished() > Error purchasing. Authenticity verification failed.");
					if (context != null) {
						int resId = R.string.support_subs_authenticity_check_fail_message;
						ToastUtils.makeTextAndShowCentered(context, resId, Toast.LENGTH_LONG);
					}
					return;
				}
				String purchasedSku = purchase.getSku();
				if (ALL_VALID_SUBSCRIPTIONS.contains(purchasedSku)) {
					if (context != null) {
						int resId = R.string.support_subs_purchase_successful_message;
						ToastUtils.makeTextAndShowCentered(context, resId, Toast.LENGTH_LONG);
					}
					setHasSubscription(true);
				}
			}, payload);
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean onActivityResult(@NonNull Context context, int requestCode, int resultCode, @Nullable Intent data) {
		setContext(context);
		//noinspection RedundantIfStatement
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

	public interface OnVendingResultListener {
		void onVendingResult(Boolean hasSubscription);
	}

}
