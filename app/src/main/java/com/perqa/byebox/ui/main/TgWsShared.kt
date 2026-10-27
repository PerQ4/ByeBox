package com.perqa.byebox.ui.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
    version: String? = null,
    language: String = "ru"
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                text = if (cfEnabled) "CF" else Loc.get("tgws_direct", language),
                modifier = Modifier
                    .weight(0.9f)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
            ProxyStatusDivider()
            ProxyStatusItem(
                text = String.format(Loc.get("tgws_pool_short", language), poolSize),
                modifier = Modifier
                    .weight(1.05f)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
            ProxyStatusDivider()
            ProxyStatusItem(
                text = String.format(Loc.get("tgws_port_short", language), port),
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
        // Минимальная зона нажатия 48dp даже если вызывающий код передал
        // меньшую высоту — доступность важнее компактности.
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        // Выбранное состояние не должно читаться только по цвету: добавляем галку.
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Box(modifier = Modifier.width(4.dp))
        }
        Text(
            label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Пакеты клиентов Telegram, которым можно применить прокси и которые
 * показываются в пикере обхода трафика.
 *
 * Единый список на всё приложение: и `applyToTelegramPackages` (применение
 * прокси), и `TgWsProxySettingsContent` (пикер клиента) читают его отсюда,
 * чтобы списки не разъезжались. При добавлении нового форка правьте только тут.
 */
internal val telegramPackages = listOf(
    // Официальные клиенты Telegram
    "org.telegram.messenger",
    "org.telegram.messenger.beta",
    // Telegram X
    "org.thunderdog.challegram",
    // Популярные форки
    "com.exteragram.messenger",
    "com.radolyn.ayugram",
    "com.exteraless.app",
    "org.telegram.plus",
    "tw.nekomimi.nekogram",
    "xyz.nextalone.nagram",
    "uz.unnarsx.cherrygram",
    "org.telegram.mdgram",
    "org.forkgram.messenger",
    "org.forkclient.messenger.beta",
    "app.nicegram",
    "top.qwq2333.nullgram",
    "it.belloworld.mercurygram",
    "org.telegram.group",
    "ir.ilmili.telegraph",
    "org.telegram.BifToGram",
    "com.iMe.android",
    "ru.dahl.messenger",
    "com.scriptsaz.litegram"
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