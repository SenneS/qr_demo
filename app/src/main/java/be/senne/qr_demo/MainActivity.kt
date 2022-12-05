package be.senne.qr_demo

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.graphics.scale
import be.senne.qr_demo.ui.theme.Qr_demoTheme
import net.glxn.qrgen.android.QRCode
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

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
                    createQrGenerator()
                }
            }
        }
    }
}

@Composable
fun createQrGenerator() {

    val bitmap = remember { mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)) }

    LazyColumn(content = {
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