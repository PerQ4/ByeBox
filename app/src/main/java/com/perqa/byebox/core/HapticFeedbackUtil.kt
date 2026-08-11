package com.perqa.byebox.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SUCCESS,
    ERROR
}

object HapticFeedbackUtil {
    fun play(context: Context, type: HapticType, scaleFactor: Float = 1.0f) {
        runCatching {
            val multiplier = when {
                scaleFactor >= 1.00f -> 0.0f
                scaleFactor >= 0.95f -> 0.6f
                scaleFactor >= 0.90f -> 1.0f
                else -> 1.6f
            }
            if (multiplier <= 0.0f) return

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (type) {
                        HapticType.LIGHT -> {
                            val dur = (8 * multiplier).toLong().coerceAtLeast(1)
                            val amp = (30 * multiplier).toInt().coerceIn(1, 255)
                            vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                        }
                        HapticType.MEDIUM -> {
                            val dur = (15 * multiplier).toLong().coerceAtLeast(1)
                            val amp = (70 * multiplier).toInt().coerceIn(1, 255)
                            vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                        }
                        HapticType.HEAVY -> {
                            val dur = (30 * multiplier).toLong().coerceAtLeast(1)
                            val amp = (140 * multiplier).toInt().coerceIn(1, 255)
                            vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                        }
                        HapticType.SUCCESS -> {
                            val timings = longArrayOf(0, (10 * multiplier).toLong().coerceAtLeast(1), 40, (20 * multiplier).toLong().coerceAtLeast(1))
                            val amplitudes = intArrayOf(0, (120 * multiplier).toInt().coerceIn(1, 255), 0, (140 * multiplier).toInt().coerceIn(1, 255))
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        }
                        HapticType.ERROR -> {
                            val timings = longArrayOf(0, (30 * multiplier).toLong().coerceAtLeast(1), 30, (40 * multiplier).toLong().coerceAtLeast(1))
                            val amplitudes = intArrayOf(0, (160 * multiplier).toInt().coerceIn(1, 255), 0, (200 * multiplier).toInt().coerceIn(1, 255))
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    when (type) {
                        HapticType.LIGHT -> vibrator.vibrate((8 * multiplier).toLong().coerceAtLeast(1))
                        HapticType.MEDIUM -> vibrator.vibrate((15 * multiplier).toLong().coerceAtLeast(1))
                        HapticType.HEAVY -> vibrator.vibrate((30 * multiplier).toLong().coerceAtLeast(1))
                        HapticType.SUCCESS -> vibrator.vibrate(longArrayOf(0, (10 * multiplier).toLong().coerceAtLeast(1), 40, (20 * multiplier).toLong().coerceAtLeast(1)), -1)
                        HapticType.ERROR -> vibrator.vibrate(longArrayOf(0, (30 * multiplier).toLong().coerceAtLeast(1), 30, (40 * multiplier).toLong().coerceAtLeast(1)), -1)
                    }
                }
            }
        }
    }
}
