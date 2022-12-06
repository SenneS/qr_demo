package be.senne.qr_demo

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import be.senne.qr_demo.ui.theme.Qr_demoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import net.glxn.qrgen.android.QRCode
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.util.*


class MainActivity : ComponentActivity() {

    companion object {
        lateinit var resources : Resources
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MainActivity.resources = resources

        Security.removeProvider("BC");
        Security.addProvider(BouncyCastleProvider())

        super.onCreate(savedInstanceState)

        setContent {
            Qr_demoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    createQrScreen()
                }
            }
        }
    }
}

@Composable
fun createQrScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(5f)
                .padding(8.dp), contentAlignment = Alignment.Center) {
            createQrGenerator()
        }

        Spacer(modifier = Modifier
            .size(5.dp)
            .weight(1f))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(5f)
                .padding(8.dp)) {
            createQrScanner()
        }
    }
}

private fun generatePublicKeyQr() : Bitmap {
    val ecParameter = ECGenParameterSpec("P-521")
    val keygen = KeyPairGenerator.getInstance("ECDH", "BC");
    keygen.initialize(ecParameter, SecureRandom())
    val key = keygen.genKeyPair()

    val keyBytes = key.public.encoded
    val keyBase64 = Base64.getEncoder().encodeToString(keyBytes)

    val qrCode = QRCode.from(keyBase64)

    val px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 380f, MainActivity.resources.displayMetrics))


    var qrBitmap = qrCode.withSize(px, px).bitmap()
    println("${qrBitmap.width}")
    println("${qrBitmap.height}")

    return qrBitmap
}

@Composable
fun createQrGenerator() {
    val bitmap = remember { mutableStateOf(generatePublicKeyQr()) }
    Image(bitmap = bitmap.value.asImageBitmap(), contentDescription = "public_key", Modifier.clickable(onClick = {
        bitmap.value = generatePublicKeyQr()
    }))
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun createQrScanner() {
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)

    if(!cameraPermissionState.status.isGranted) {
        Column() {
            Text("Ik heb camera permissie nodig om QR codes te scannen.")
            Button(onClick = {cameraPermissionState.launchPermissionRequest()}) {
                Text(text = "Geef Camera Permissie")
            }
        }
    }
    else {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val cameraProviderFuture = remember {
            ProcessCameraProvider.getInstance(context)
        }
        AndroidView(factory = { viewContext ->
            val previewView = PreviewView(viewContext)
            val executor = ContextCompat.getMainExecutor(viewContext)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { preview ->
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                val qrScanner = QrScanner({barcode ->
                    Toast.makeText(context, "Qr scanned: ${barcode[0].toString()}", Toast.LENGTH_SHORT)
                })

                val imageAnalysis : ImageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { ia ->
                    ia.setAnalyzer(executor, qrScanner)
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)

            }, executor)
            previewView
        }, modifier = Modifier.fillMaxSize())
    }
}

//image.image mag anders niet.
@SuppressLint("UnsafeOptInUsageError")
class QrScanner(
    var callback: (barcode : List<Barcode>) -> Unit
) : ImageAnalysis.Analyzer {

    //private var callback: (barcode : List<Barcode>) -> Unit
    private var lastAttempt = 0

    override fun analyze(image: ImageProxy) {
        val currentAttempt = System.currentTimeMillis()
        //1 poging / 0.5 seconden
        if(currentAttempt - lastAttempt >= 500) {

            val barcodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

            val imageImage = image.image
            if (imageImage != null) {
                val inputImage = InputImage.fromMediaImage(imageImage, image.imageInfo.rotationDegrees)
                barcodeScanner.process(inputImage).addOnSuccessListener { barcode ->
                    callback(barcode)
                }.addOnCompleteListener {
                    image.close()
                }
            }
        } else {
            image.close()
        }
    }
}