package com.perqa.byebox.data

/**
 * Built-in User-Agent presets offered in the subscription editor.
 *
 * Each preset maps a human-readable label (with an optional Loc key) to the
 * exact UA string sent in the `User-agent` header when updating the
 * subscription. An empty value means "use the app default
 * (`v2rayNG/{version}`)".
 */
data class UaPreset(
    val label: String,
    val value: String,
    val locKey: String? = null
)

object UaPresets {
    val all: List<UaPreset> = listOf(
        UaPreset(label = "По умолчанию (v2rayNG)", value = "", locKey = "sub_ua_preset_default"),
        UaPreset(label = "Happ 2.5.0", value = "Happ/2.5.0"),
        UaPreset(label = "INCY 4.3.8", value = "INCY/4.3.8"),
        UaPreset(label = "v2rayNG 2.3.6", value = "v2rayNG/2.3.6"),
        UaPreset(label = "Clash Meta 2.12", value = "ClashMetaForAndroid/2.12.0.Meta"),
        UaPreset(label = "mihomo 1.19", value = "mihomo/1.19.1"),
        UaPreset(label = "FlClash 1.1.1", value = "FlClash X/1.1.1"),
        UaPreset(label = "Qv2ray 2.7.0", value = "Qv2ray/2.7.0"),
        UaPreset(label = "Shadowsocks-Android 5.3.3", value = "Shadowsocks-Android/5.3.3"),
    )
}