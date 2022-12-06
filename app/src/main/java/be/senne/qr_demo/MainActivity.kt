package be.senne.qr_demo

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import be.senne.qr_demo.ui.theme.Qr_demoTheme
import com.google.common.util.concurrent.ListenableFuture
import net.glxn.qrgen.android.QRCode
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var context : Context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        context = applicationContext

        Security.removeProvider("BC");
        Security.addProvider(BouncyCastleProvider())

        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0);
        }

        setContent {
            Qr_demoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    createQrGenerator()
                    createQrScanner()
                }
            }
        }
    }
}

@Composable
fun createQrScanner() {
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

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

        }, executor)
        previewView
    }, modifier = Modifier.fillMaxSize())
    
    val cameraSelector: CameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()


    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
    }, ContextCompat.getMainExecutor(MainActivity.context))

}

@Composable
fun createQrGenerator() {

    val bitmap = remember { mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)) }

    LazyColumn(content = {
        item {
            Button(onClick = {

            }) {
                Text(text = "Scan")
            }
        }
        item {
            Button(onClick = {
                val ecParameter = ECGenParameterSpec("P-521")
                val keygen = KeyPairGenerator.getInstance("ECDH", "BC");
                keygen.initialize(ecParameter, SecureRandom())
                val key = keygen.genKeyPair()

                val keyBytes = key.public.encoded
                val keyBase64 = Base64.getEncoder().encodeToString(keyBytes)

                val qrCode = QRCode.from(keyBase64)
                var qrBitmap = qrCode.bitmap()
                qrBitmap = qrBitmap.scale(qrBitmap.width * 10, qrBitmap.height * 10)
                bitmap.value = qrBitmap

            }) {
                Text(text = "Generate Qr code")
            }
        }

        item {
            Image(bitmap = bitmap.value.asImageBitmap(), contentDescription = "public_key")
        }
    })

    Column(Modifier.fillMaxWidth()) {
        
    }
}