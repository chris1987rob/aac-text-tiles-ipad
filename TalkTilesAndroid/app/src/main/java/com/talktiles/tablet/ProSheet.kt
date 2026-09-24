package com.talktiles.tablet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * What Pro adds, where this device stands, and the Buy / Restore buttons.
 * Opened from Home, from Settings, and in place of an action the free
 * version refuses - in which case `reason` says which one.
 */
@Composable
fun UpgradeSheet(store: AACStore, reason: ProBlock? = null, onDismiss: () -> Unit) {
    val pro = store.pro
    val context = LocalContext.current
    ModalSheet(title = "Talk Tiles Pro", onDismiss = onDismiss, leading = "Close") {
        if (reason != null && !pro.hasFullAccess) {
            FormSection { Text(reason.message, style = TTType.body, color = BoardTheme.ink, modifier = Modifier.padding(16.dp)) }
        }
        FormSection("This tablet") {
            FormRow {
                Icon(if (pro.isPro) Icons.Default.CheckCircle else Icons.Default.WorkspacePremium, null,
                    tint = if (pro.isPro) BoardTheme.green else BoardTheme.blue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(pro.statusLine, style = TTType.bodyStrong, color = BoardTheme.ink)
            }
        }
        FormSection("Pro adds", "Talking is never limited: every picture, Bella's voice, your own photos and recordings, the keyboard and the ready-made boards are in the free version too.") {
            ProFeature(Icons.Default.AutoStories, "Unlimited pages", "Free: the starter book plus ${ProRules.FREE_EXTRA_PAGES} pages of your own")
            ProFeature(Icons.Default.Image, "Unlimited visual scenes", "Free: ${ProRules.FREE_SCENES} scene")
            ProFeature(Icons.Default.Star, "Saved Buttons", "Build a button once, put it on any page")
            ProFeature(Icons.Default.WorkspacePremium, "Everything Pro gets later", "New voices, printing boards, more than one learner")
        }
        if (!pro.isPro) {
            FormSection(footer = "One payment, no subscription. Pro stays with your Google account, so it comes back on a new tablet. If Pro ever ends, nothing you made is removed.") {
                val price = PlayBilling.price
                Box(Modifier.fillMaxWidth().padding(12.dp)) {
                    PrimaryButton(
                        if (price != null) "Get Pro - $price" else "Get Pro",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = PlayBilling.canBuy
                    ) { context.findActivity()?.let { PlayBilling.buy(it) } }
                }
                PlayBilling.problem?.let { Text(it, style = TTType.caption, color = BoardTheme.slate, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) }
            }
        }
        FormSection {
            FormButton("Restore purchase", icon = Icons.Default.Restore) { PlayBilling.reconcile(userAsked = true) }
            PlayBilling.lastMessage?.let { Text(it, style = TTType.caption, color = BoardTheme.slate, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun ProFeature(icon: ImageVector, title: String, sub: String) {
    FormRow {
        Icon(icon, null, tint = BoardTheme.blue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TTType.bodyStrong, color = BoardTheme.ink)
            Text(sub, style = TTType.caption, color = BoardTheme.slate)
        }
    }
}
