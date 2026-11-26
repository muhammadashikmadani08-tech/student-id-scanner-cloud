package com.institution.idscanner.cloud.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.institution.idscanner.cloud.AppViewModel
import com.institution.idscanner.cloud.data.Student
import com.institution.idscanner.cloud.scanner.BarcodeAnalyzer
import com.institution.idscanner.cloud.scanner.IdParser
import com.institution.idscanner.cloud.scanner.OcrHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(vm: AppViewModel, onProfile: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    var scanning by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<Student?>(null) }
    var notFoundId by remember { mutableStateOf<String?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var manualId by remember { mutableStateOf("") }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var scanTimeMillis by remember { mutableStateOf<Long?>(null) }
    val institutionName by vm.institutionName.collectAsState()

    // Live clock: ticks every second while this screen is open.
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }
    val clockFmt = remember { SimpleDateFormat("dd MMM yyyy  •  hh:mm:ss a", Locale.getDefault()) }
    val resultFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // The PreviewView is created ONCE and kept. Re-binding the camera on every
    // recomposition (the old behaviour) made the preview go black / flicker.
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    fun handleRaw(raw: String) {
        if (busy) return
        busy = true
        scope.launch {
            val student = vm.lookupScanned(raw)
            scanTimeMillis = System.currentTimeMillis()
            busy = false
            scanning = false
            if (student != null) found = student else notFoundId = raw
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Bind the camera exactly once when permission is granted, and unbind
    // when this screen leaves. Never re-bind from the AndroidView update block.
    LaunchedEffect(hasPermission, previewView) {
        if (!hasPermission) return@LaunchedEffect
        try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { handleRaw(it) })
                }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                preview, imageCapture, analysis
            )
            cameraError = null
        } catch (e: Exception) {
            cameraError = "ক্যামেরা চালু হয়নি। অ্যাপ বন্ধ করে আবার খুলুন। (${e.message})"
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try { cameraProvider?.unbindAll() } catch (_: Exception) { }
            cameraExecutor.shutdown()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Scan Student ID", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(Color(0xFFE53935), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("LIVE  •  ${clockFmt.format(Date(nowMillis))}", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))

        if (!hasPermission) {
            Text("ক্যামেরার অনুমতি প্রয়োজন। অনুমতি দিন।")
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("অনুমতি দিন")
            }
        } else if (found == null && notFoundId == null) {
            // Camera preview with a clear, bright scanning frame.
            Box(
                Modifier.fillMaxWidth().height(380.dp)
                    .border(2.dp, MaterialTheme.colorScheme.tertiary)
                    .background(Color.Black)
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                if (cameraError != null) {
                    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Text(cameraError!!, color = Color.White, modifier = Modifier.padding(16.dp))
                    }
                }
                // Bright corner brackets so the scan area is unmistakable.
                Canvas(Modifier.fillMaxSize().padding(28.dp)) {
                    val c = Color(0xFFF5B301)
                    val len = 64f
                    val w = 10f
                    // top-left
                    drawLine(c, Offset(0f, w / 2), Offset(len, w / 2), w, StrokeCap.Square)
                    drawLine(c, Offset(w / 2, 0f), Offset(w / 2, len), w, StrokeCap.Square)
                    // top-right
                    drawLine(c, Offset(size.width - len, w / 2), Offset(size.width, w / 2), w, StrokeCap.Square)
                    drawLine(c, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, len), w, StrokeCap.Square)
                    // bottom-left
                    drawLine(c, Offset(0f, size.height - w / 2), Offset(len, size.height - w / 2), w, StrokeCap.Square)
                    drawLine(c, Offset(w / 2, size.height - len), Offset(w / 2, size.height), w, StrokeCap.Square)
                    // bottom-right
                    drawLine(c, Offset(size.width - len, size.height - w / 2), Offset(size.width, size.height - w / 2), w, StrokeCap.Square)
                    drawLine(c, Offset(size.width - w / 2, size.height - len), Offset(size.width - w / 2, size.height), w, StrokeCap.Square)
                }
                // Animated scan line inside the frame
                val transition = rememberInfiniteTransition(label = "scan")
                val lineY by transition.animateFloat(
                    initialValue = 0f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
                    label = "line"
                )
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                        .height(3.dp)
                        .offset(y = (28 + lineY * 290).dp)
                        .background(Color(0xFFF5B301))
                        .align(Alignment.TopCenter)
                )
                if (busy) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("QR/Barcode-টি হলুদ ফ্রেমের মধ্যে রাখুন।", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    busy = true
                    val file = File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
                    imageCapture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(file).build(),
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                scope.launch {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    val text = if (bitmap != null) OcrHelper.recognizeText(bitmap) else ""
                                    ocrText = text
                                    val id = IdParser.extractId(text)
                                    busy = false
                                    if (id != null) handleRaw(id) else {
                                        scanTimeMillis = System.currentTimeMillis()
                                        notFoundId = "OCR: কোনো ID পাওয়া যায়নি"
                                    }
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                busy = false
                                notFoundId = "ছবি তোলা যায়নি, আবার চেষ্টা করুন।"
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("QR না থাকলে ছবি তুলে OCR করুন") }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = manualId, onValueChange = { manualId = it },
                label = { Text("অথবা Student ID লিখুন") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { if (manualId.isNotBlank()) handleRaw(manualId) }, modifier = Modifier.fillMaxWidth()) {
                Text("ID দিয়ে খুঁজুন")
            }
        } else if (found != null) {
            StudentDetailCard(found!!, institutionName, scanTimeMillis = scanTimeMillis, scanTimeFormat = resultFmt)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { onProfile(found!!.studentId) }, modifier = Modifier.fillMaxWidth()) {
                Text("Full Profile দেখুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { found = null; scanTimeMillis = null; scanning = true }, modifier = Modifier.fillMaxWidth()) {
                Text("আবার Scan করুন")
            }
        } else {
            Text("Student Not Found", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text("ID: ${notFoundId ?: ""}")
            scanTimeMillis?.let {
                Text("স্ক্যানের সময়: ${resultFmt.format(Date(it))}", style = MaterialTheme.typography.bodyMedium)
            }
            if (ocrText.isNotBlank()) Text("OCR পড়েছে: $ocrText", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { notFoundId = null; ocrText = ""; scanTimeMillis = null; scanning = true }, modifier = Modifier.fillMaxWidth()) {
                Text("পুনরায় Scan করুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onProfile("") }, modifier = Modifier.fillMaxWidth()) {
                Text("Search-এ যান")
            }
        }
    }
}
