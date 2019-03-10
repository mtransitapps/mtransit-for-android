package org.mtransit.android.billing;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.common.IApplication;
import org.mtransit.android.common.repository.IKeyValueRepository;
import org.mtransit.android.commons.MTLog;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

// DEBUG: adb shell setprop log.tag.BillingClient VERBOSE
public class BillingManager implements MTLog.Loggable,
		IBillingManager,
		PurchasesUpdatedListener, BillingClientStateListener, SkuDetailsResponseListener, PurchaseHistoryResponseListener {

	private static final String LOG_TAG = BillingManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

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

	private static final boolean FORCE_HAS_SUBSCRIPTION = false;
	private static final boolean FORCE_DO_NOT_HAVE_SUBSCRIPTION = false;

	private static final String PREF_KEY_HAS_SUBSCRIPTION = "pHasSubscription";
	private static final boolean PREF_KEY_HAS_SUBSCRIPTION_DEFAULT = false;

	@NonNull
	private final IApplication appContext;
	@NonNull
	private final IKeyValueRepository cacheRepository;

	@NonNull
	private final List<Purchase> purchases = new ArrayList<>();

	@Nullable
	private BillingClient billingClient;
	private int billingClientResponseCode = -1;
	private boolean isServiceConnected = false;

	@Nullable
	private static Boolean hasSubscription;

	@NonNull
	private WeakHashMap<OnBillingResultListener, Void> listenersWR = new WeakHashMap<>();

	public BillingManager(@NonNull IApplication appContext,
						  @NonNull IKeyValueRepository cacheRepository) {
		this.appContext = appContext;
		this.cacheRepository = cacheRepository;
		init();
	}

	private void init() {
		billingClient = BillingClient
				.newBuilder(appContext.requireContext())
				.setListener(this)
				.build();
		startServiceConnection();
	}

	@Override
	public void addListener(@NonNull OnBillingResultListener listener) {
		this.listenersWR.put(listener, null);
	}

	@Override
	public void removeListener(@NonNull OnBillingResultListener listener) {
		this.listenersWR.remove(listener);
	}

	private void setHasSubscription(Boolean newHasSubscription) {
		if (BuildConfig.DEBUG) {
			if (FORCE_HAS_SUBSCRIPTION) {
				newHasSubscription = true;
			} else if (FORCE_DO_NOT_HAVE_SUBSCRIPTION) {
				newHasSubscription = false;
			}
		}
		hasSubscription = newHasSubscription;
		// TODO broadcastNewVendingResult(hasSubscription);
		if (hasSubscription != null) {
			this.cacheRepository.saveAsync(PREF_KEY_HAS_SUBSCRIPTION, hasSubscription);
		}
	}

	@Nullable
	@Override
	public Boolean hasSubscription() {
		if (hasSubscription == null) {
			if (this.cacheRepository.hasKey(PREF_KEY_HAS_SUBSCRIPTION)) {
				boolean newHasSubscription = this.cacheRepository.getValue(PREF_KEY_HAS_SUBSCRIPTION, PREF_KEY_HAS_SUBSCRIPTION_DEFAULT);
				if (BuildConfig.DEBUG) {
					if (FORCE_HAS_SUBSCRIPTION) {
						newHasSubscription = true;
					} else if (FORCE_DO_NOT_HAVE_SUBSCRIPTION) {
						newHasSubscription = false;
					}
				}
				hasSubscription = newHasSubscription;
			}
		}
		return hasSubscription;
	}

	public void refreshPurchases() {
		if (this.billingClientResponseCode == BillingResponse.OK) {
			queryPurchaseHistoryAsync();
		}
	}

	@Override
	public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchaseList) {
		if (responseCode == BillingResponse.OK) {
			if (purchaseList != null) {
				for (Purchase purchase : purchaseList) {
					handlePurchase(purchase);
				}
			}
			// TODO billingUpdatesListener.onPurchasesUpdated(this.purchaseList);
		} else if (responseCode == BillingResponse.USER_CANCELED) {
			MTLog.d(this, "onPurchasesUpdated() - user cancelled the purchase flow - skipping");
		} else {
			MTLog.w(this, "onPurchasesUpdated() got unknown resultCode: %s.", responseCode);
		}
	}

	private void handlePurchase(Purchase purchase) {
		if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
			MTLog.i(this, "Got a purchase: %s; but signature is bad. Skipping...", purchase);
			return;
		}
		MTLog.d(this, "Got a verified purchase: " + purchase);
		this.purchases.add(purchase);
	}

	private boolean verifyValidSignature(String originalJson, String signature) {
		// TODO
		return false;
	}

	private void startServiceConnection() {
		billingClient.startConnection(this);
	}

	@Override
	public void onBillingSetupFinished(int responseCode) {
		MTLog.d(this, "Setup finished. Response code: %s.", responseCode);
		if (responseCode == BillingResponse.OK) {
			this.isServiceConnected = true;
			// TODO	executeOnSuccess.run();
		}
		this.billingClientResponseCode = responseCode;
	}

	@Override
	public void onBillingServiceDisconnected() {
		this.isServiceConnected = false;
	}

	private boolean areSubscriptionsSupported() {
		int responseCode = billingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
		if (responseCode != BillingResponse.OK) {
			MTLog.w(this, "areSubscriptionsSupported() got an error response: %s", responseCode);
		}
		return responseCode == BillingResponse.OK;
	}

	private void queryPurchaseHistoryAsync() {
		billingClient.queryPurchaseHistoryAsync(SkuType.SUBS, this);
	}

	@Override
	public void onPurchaseHistoryResponse(int responseCode, @Nullable List<Purchase> purchasesList) {
		onPurchasesUpdated(responseCode, purchasesList);
	}

	private void queryPurchasesFromCache() {
		long time = System.currentTimeMillis();
		if (areSubscriptionsSupported()) {
			PurchasesResult subscriptionResult = billingClient.queryPurchases(SkuType.SUBS);
			MTLog.d(this, "Querying purchases and subscriptions elapsed time: %s ms", (System.currentTimeMillis() - time));
			MTLog.d(this, "Querying subscriptions result code: %s res: %s.", subscriptionResult.getResponseCode(), subscriptionResult.getPurchasesList().size());
			if (subscriptionResult.getResponseCode() == BillingResponse.OK) {
				onQueryPurchasesFinished(subscriptionResult);
			} else {
				MTLog.e(this, "Got an error response trying to query subscription purchases");
			}
		}
	}

	private void onQueryPurchasesFinished(PurchasesResult result) {
		if (this.billingClient == null || result.getResponseCode() != BillingResponse.OK) {
			MTLog.w(this, "Billing client was null or result code (%s) was bad - quitting", result.getResponseCode());
			return;
		}
		MTLog.d(this, "Query inventory was successful.");
		this.purchases.clear();
		onPurchasesUpdated(BillingResponse.OK, result.getPurchasesList());
	}

	@Override
	public void queryInventoryAsync(@NonNull final OnBillingInventoryListener listener) {
		billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
				.setSkusList(ALL_VALID_SUBSCRIPTIONS)
				.setType(SkuType.SUBS)
				.build(), new SkuDetailsResponseListener() {
			@Override
			public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
				if (responseCode == BillingResponse.OK) {
					List<Pair<String, String>> skuAndPriceList = new ArrayList<>();
					if (skuDetailsList != null) {
						for (SkuDetails skuDetails : skuDetailsList) {
							skuAndPriceList.add(new Pair<>(skuDetails.getSku(), skuDetails.getPrice()));
						}
					}
					listener.onBillingInventoryResult(skuAndPriceList);
				} else if (responseCode == BillingResponse.USER_CANCELED) {
					MTLog.d(BillingManager.this, "onSkuDetailsResponse() - user cancelled the purchase flow - skipping");
					listener.onUserCancelled();
				} else {
					MTLog.w(BillingManager.this, "onSkuDetailsResponse() got unknown resultCode: %s.", responseCode);
					listener.onUnexpectedError();
				}
			}
		});
	}

	@Override
	public void launchPurchase(Activity activity, String sku) {
		// TODO
	}

	private void querySkuDetailsAsync() {
		billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
				.setSkusList(ALL_VALID_SUBSCRIPTIONS)
				.setType(SkuType.SUBS)
				.build(), this);

	}

	@Override
	public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
		if (responseCode == BillingResponse.OK) {
			if (skuDetailsList != null) {
				for (SkuDetails skuDetail : skuDetailsList) {
					// TODO handl????(skuDetail);
				}
			}
		} else if (responseCode == BillingResponse.USER_CANCELED) {
			MTLog.d(this, "onSkuDetailsResponse() - user cancelled the purchase flow - skipping");
		} else {
			MTLog.w(this, "onSkuDetailsResponse() got unknown resultCode: %s.", responseCode);
		}
	}

	@Override
	public String getSku(String periodCat, String priceCat) {
		return SKU_STARTS_WITH_F + periodCat + SKU_SUBSCRIPTION + priceCat;
	}

	@Override
	public boolean isSkuAvailableForPurchase(@NonNull String sku) {
		return AVAILABLE_SUBSCRIPTIONS.contains(sku);
	}

	@Override
	public void destroy() {
		MTLog.d(this, "Destroying the manager.");
		if (this.billingClient != null) {
			if (this.billingClient.isReady()) {
				this.billingClient.endConnection();
			}
			this.billingClient = null;
		}
	}
}
