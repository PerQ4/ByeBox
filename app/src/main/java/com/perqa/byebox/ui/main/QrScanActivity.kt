package com.perqa.byebox.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.perqa.byebox.theme.ByeBoxTheme
import com.perqa.byebox.theme.AppTheme
import com.perqa.byebox.theme.DarkThemeStyle
import java.util.concurrent.Executors

class QrScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences("byebox_settings", MODE_PRIVATE)
            val appTheme = runCatching { AppTheme.valueOf(prefs.getString("app_theme", "SYSTEM_DYNAMIC") ?: "SYSTEM_DYNAMIC") }.getOrDefault(AppTheme.SYSTEM_DYNAMIC)
            val darkThemeStyle = runCatching { DarkThemeStyle.valueOf(prefs.getString("pref_dark_theme_style", "STANDARD") ?: "STANDARD") }.getOrDefault(DarkThemeStyle.STANDARD)
            ByeBoxTheme(appTheme = appTheme, darkThemeStyle = darkThemeStyle) {
                QrScanScreen(
                    onResult = { url ->
                        setResult(RESULT_OK, Intent().putExtra("SCAN_RESULT", url))
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    @Composable
    private fun QrScanScreen(onResult: (String) -> Unit, onBack: () -> Unit) {
        val sysLang = java.util.Locale.getDefault().language
        val language = when {
            sysLang.startsWith("ru") || sysLang.startsWith("be") || sysLang.startsWith("uk") || sysLang.startsWith("kk") || sysLang.startsWith("ky") -> "ru"
            sysLang.startsWith("zh") -> "zh"
            else -> "en"
        }
        var hasCameraPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(this@QrScanActivity, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }
        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> hasCameraPermission = granted }

        LaunchedEffect(Unit) {
            if (!hasCameraPermission) permLauncher.launch(Manifest.permission.CAMERA)
        }

        var found by remember { mutableStateOf(false) }

        val scanner = remember {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            BarcodeScanning.getClient(options)
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                if (found) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val url = barcodes.firstOrNull()?.rawValue
                                            if (url != null && !found) {
                                                found = true
                                                onResult(url)
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    )
                }

                Text(
                    text = Loc.get("qr_scan_hint", language),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                )
            } else {
                Text(
                    text = Loc.get("qr_camera_denied", language),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = Loc.get("back_cd", language),
                    tint = Color.White
                )
            }
        }
    }
}
