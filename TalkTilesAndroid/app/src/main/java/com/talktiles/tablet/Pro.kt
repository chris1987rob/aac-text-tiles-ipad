package com.talktiles.tablet

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.serialization.Serializable
import java.io.File

// Talk Tiles is free to talk with, forever. Pro is a one-time purchase that
// takes the limits off building: more than five pages of your own, more than
// one visual scene, and Saved Buttons. Every new install gets Pro free for
// 14 days first.
//
// The one rule every check here keeps: losing Pro (trial over, refund) never
// takes anything away. Pages made during the trial stay in the book and keep
// talking, saved buttons stay usable - only adding more is refused.

/** What the free version allows. Pure, so the limits are tested on the JVM. */
object ProRules {
    const val TRIAL_DAYS = 14
    /** Pages a free book may have beyond the starter book. */
    const val FREE_EXTRA_PAGES = 5
    /** Visual scene pages a free book may have. The starter book has none. */
    const val FREE_SCENES = 1
    const val DAY_MS = 24L * 60 * 60 * 1000

    val starterPageCount: Int by lazy { AACStore.defaultPages().size }

    /** Pages beyond the starter book's size. Deleting a starter page frees a place - that is fine. */
    fun ownPages(pages: List<PageModel>): Int = maxOf(0, pages.size - starterPageCount)
    fun scenes(pages: List<PageModel>): Int = pages.count { it.type == PageType.SCENE }
}

/** Why an action needs Pro, in words for the Upgrade sheet. */
enum class ProBlock(val message: String) {
    PAGES("The free version holds the starter book plus ${ProRules.FREE_EXTRA_PAGES} pages of your own, and this book is full. Talk Tiles Pro takes the limit off. Every page already in the book keeps working either way."),
    SCENES("The free version has room for ${ProRules.FREE_SCENES} visual scene. Talk Tiles Pro lets you make as many as you like."),
    SAVED_BUTTONS("Saving buttons to reuse on other pages is part of Talk Tiles Pro. Buttons you saved before still work.")
}

/** On disk as aac_licence.json. Never part of a backup: Pro belongs to the Google account, not the book. */
@Serializable
data class LicenceState(
    val trialStartedAt: Long? = null,
    /** The latest time the app has seen, so turning the clock back does not restart the trial. */
    val latestSeenAt: Long? = null,
    val proOwned: Boolean = false,
    val orderId: String? = null
)

class ProAccess(private val file: File?, private val clock: () -> Long = System::currentTimeMillis) {

    var state by mutableStateOf(load())
        private set

    init {
        val now = clock()
        if (state.trialStartedAt == null) write(state.copy(trialStartedAt = now, latestSeenAt = now))
        else if (now > (state.latestSeenAt ?: 0L)) write(state.copy(latestSeenAt = now))
    }

    val isPro: Boolean get() = state.proOwned

    private val trialMsLeft: Long get() {
        val start = state.trialStartedAt ?: return 0
        val now = maxOf(clock(), state.latestSeenAt ?: 0L)
        return start + ProRules.TRIAL_DAYS * ProRules.DAY_MS - now
    }

    val inTrial: Boolean get() = !isPro && trialMsLeft > 0

    /** Whole days left, rounded up: the last afternoon of the trial still says "1 day left". */
    val trialDaysLeft: Int get() = if (trialMsLeft <= 0) 0 else ((trialMsLeft + ProRules.DAY_MS - 1) / ProRules.DAY_MS).toInt()

    val hasFullAccess: Boolean get() = isPro || inTrial

    /** Null when a page of this type may be added, else why not. */
    fun blockAddingPage(pages: List<PageModel>, type: PageType = PageType.GRID): ProBlock? {
        if (hasFullAccess) return null
        if (type == PageType.SCENE && ProRules.scenes(pages) >= ProRules.FREE_SCENES) return ProBlock.SCENES
        if (ProRules.ownPages(pages) >= ProRules.FREE_EXTRA_PAGES) return ProBlock.PAGES
        return null
    }

    fun blockSavingButton(): ProBlock? = if (hasFullAccess) null else ProBlock.SAVED_BUTTONS

    /** Short line for Home and Settings. */
    val statusLine: String get() = when {
        isPro -> "Pro - thank you for supporting Talk Tiles"
        inTrial -> "Pro trial: $trialDaysLeft ${if (trialDaysLeft == 1) "day" else "days"} left"
        else -> "Free version"
    }

    fun grantPro(orderId: String?) {
        if (state.proOwned && state.orderId == orderId) return
        write(state.copy(proOwned = true, orderId = orderId))
    }

    /** Play says the purchase is gone (refunded, revoked). The book is not touched. */
    fun revokePro() {
        if (!state.proOwned) return
        write(state.copy(proOwned = false, orderId = null))
    }

    private fun load(): LicenceState {
        val f = file ?: return LicenceState()
        if (!f.exists()) return LicenceState()
        return try { AppJson.decodeFromString(LicenceState.serializer(), f.readText()) }
        catch (e: Exception) {
            // Fail kind: a fresh trial starts, and a purchase comes back from Play on the next check.
            Log.e("TalkTiles", "licence file unreadable (${e.javaClass.simpleName})")
            LicenceState()
        }
    }

    private fun write(next: LicenceState) {
        state = next
        val f = file ?: return
        try {
            val tmp = File(f.path + ".tmp")
            tmp.writeText(AppJson.encodeToString(LicenceState.serializer(), next))
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        } catch (e: Exception) { Log.e("TalkTiles", "licence save failed", e) }
    }
}

/**
 * Google Play Billing for the one Pro product. The purchase is cached in
 * ProAccess the moment Play reports it, so Pro keeps working offline; every
 * launch with a connection re-checks with Play, which is how a refund lands.
 */
object PlayBilling {
    /** Must match the in-app product created in Play Console › Monetize › In-app products. */
    const val PRO_PRODUCT_ID = "talktiles_pro"

    /** The store's own price text, e.g. "$24.99". Null until Play answers. */
    var price by mutableStateOf<String?>(null)
        private set
    /** Plain-words state for the Upgrade sheet when buying is not possible right now. */
    var problem by mutableStateOf<String?>("Connecting to Google Play...")
        private set
    var busy by mutableStateOf(false)
        private set
    var lastMessage by mutableStateOf<String?>(null)
        private set

    private var client: BillingClient? = null
    private var details: ProductDetails? = null
    private var pro: ProAccess? = null

    val canBuy: Boolean get() = client != null && details != null && !busy

    fun init(context: Context, access: ProAccess) {
        pro = access
        if (client != null) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProduct()
                    reconcile(userAsked = false)
                } else problem = "Google Play is not available on this device right now."
            }
            override fun onBillingServiceDisconnected() { /* auto-reconnection is on */ }
        })
    }

    private fun loadProduct() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )).build()
        c.queryProductDetailsAsync(params) { result, found ->
            val d = found.productDetailsList.firstOrNull()
            if (result.responseCode == BillingClient.BillingResponseCode.OK && d != null) {
                details = d
                price = d.oneTimePurchaseOfferDetails?.formattedPrice
                problem = null
            } else {
                // Normal until the product exists in Play Console and the app is installed from Play.
                problem = "Talk Tiles Pro is not on sale yet. It will be once the app is in the Play Store."
            }
        }
    }

    /** Asks Play what this Google account owns. A purchase restores Pro; its absence (refund) removes it. */
    fun reconcile(userAsked: Boolean) {
        val c = client ?: run { if (userAsked) lastMessage = "Google Play is not available on this device."; return }
        c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (userAsked) lastMessage = "Could not reach Google Play. Try again with an internet connection."
                return@queryPurchasesAsync
            }
            val owned = purchases.firstOrNull { PRO_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            if (owned != null) {
                handle(owned)
                if (userAsked) lastMessage = "Pro restored."
            } else {
                pro?.revokePro()
                if (userAsked) lastMessage = "This Google account has not bought Talk Tiles Pro."
            }
        }
    }

    fun buy(activity: Activity) {
        val c = client ?: return
        val d = details ?: return
        busy = true
        lastMessage = null
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()
        )).build()
        val result = c.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            busy = false
            lastMessage = "Google Play could not start the purchase (${result.responseCode})."
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        busy = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.filter { PRO_PRODUCT_ID in it.products }?.forEach { handle(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> reconcile(userAsked = true)
            else -> lastMessage = "The purchase did not go through (${result.responseCode}). Nothing was charged."
        }
    }

    private fun handle(p: Purchase) {
        when (p.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                pro?.grantPro(p.orderId)
                lastMessage = "Talk Tiles Pro is on. Thank you!"
                // Play refunds a purchase that is not acknowledged within 3 days.
                if (!p.isAcknowledged) {
                    client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r ->
                        if (r.responseCode != BillingClient.BillingResponseCode.OK) Log.e("TalkTiles", "acknowledge failed: ${r.responseCode}")
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> lastMessage = "Your payment is pending. Pro turns on as soon as Google Play confirms it."
            else -> {}
        }
    }
}
