package com.perqa.byebox.ui.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.IntrinsicSize
import com.amurcanov.tgwsproxy.R as TgwsR

@Composable
fun ProxyStatusPanel(
    cfEnabled: Boolean,
    poolSize: Int,
    port: String,
    version: String? = null
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProxyStatusItem(
                text = if (cfEnabled) "CF" else androidx.compose.ui.res.stringResource(TgwsR.string.direct_mode),
                modifier = Modifier
                    .weight(0.9f)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
            ProxyStatusDivider()
            ProxyStatusItem(
                text = androidx.compose.ui.res.stringResource(TgwsR.string.pool_short, poolSize),
                modifier = Modifier
                    .weight(1.05f)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
            ProxyStatusDivider()
            ProxyStatusItem(
                text = androidx.compose.ui.res.stringResource(TgwsR.string.port_short, port),
                modifier = Modifier
                    .weight(1.35f)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
            if (version != null) {
                ProxyStatusDivider()
                ProxyStatusItem(
                    text = version,
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ProxyStatusItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProxyStatusDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
}

@Composable
fun ModeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private val telegramPackages = listOf(
    "org.telegram.messenger",
    "com.radolyn.ayugram",
    "com.exteragram.messenger",
    "org.telegram.plus",
    "ir.ilmili.telegraph",
    "org.telegram.BifToGram",
    "tw.nekomimi.nekogram",
    "xyz.nextalone.nagram",
    "uz.unnarsx.cherrygram",
    "org.telegram.mdgram",
    "org.forkclient.messenger.beta",
    "app.nicegram",
    "top.qwq2333.nullgram",
    "com.iMe.android",
    "ru.dahl.messenger",
    "com.scriptsaz.litegram",
    "org.thunderdog.challegram"
)

fun applyToTelegramPackages(context: Context, url: String, language: String = "ru") {
    val pm = context.packageManager
    val availablePackages = telegramPackages.filter {
        try {
            pm.getPackageInfo(it, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    if (availablePackages.isEmpty()) {
        Toast.makeText(context, Loc.get("tgws_no_clients", language), Toast.LENGTH_SHORT).show()
        return
    }

    val targetedIntents = availablePackages.map { pkg ->
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(pkg)
        }
    }

    if (targetedIntents.size == 1) {
        val intent = targetedIntents.first().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, Loc.get("tgws_open_error", language), Toast.LENGTH_SHORT).show()
        }
    } else {
        val chooserIntent = Intent.createChooser(targetedIntents.first(), Loc.get("tgws_choose_title", language))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.drop(1).toTypedArray())
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, Loc.get("tgws_choose_error", language), Toast.LENGTH_SHORT).show()
        }
    }
}