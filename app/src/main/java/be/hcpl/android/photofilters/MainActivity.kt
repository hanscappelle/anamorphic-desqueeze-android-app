package be.hcpl.android.photofilters

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.graphics.scale
import be.hcpl.android.photofilters.ui.theme.AnamorphicDesqueezeTheme
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL

class MainActivity : ComponentActivity() {

    // TODO add ViewModel here to handle logic
    private var imageContent: ImageContent = ImageContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateContent()
        // check for received images
        handleReceivedContent()

    }

    private fun updateContent() {
        setContent {
            AnamorphicDesqueezeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DesqueezeAppContent(
                        modifier = Modifier.padding(innerPadding),
                        content = imageContent,
                        onResize = ::handleResizeImage
                    )
                }
            }
        }
    }

    private fun handleResizeImage() {
        imageContent.imageUrl?.let { imageUri ->
            var out: OutputStream? = null
            var originalBitmap: Bitmap? = null
            var scaledBitmap: Bitmap? = null
            try {
                originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
                // TODO make ratio configurable
                scaledBitmap = originalBitmap.scale((originalBitmap.width * 1.33).toInt(), originalBitmap.height)// keep height, change width by distortion value
                val path = Environment.getExternalStorageDirectory().toString()
                val file = File(path, "Desqueezed_${System.currentTimeMillis()}.JPG")
                out = FileOutputStream(file);
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
                MediaStore.Images.Media.insertImage(contentResolver, file.absolutePath, file.name, file.name);
            } finally {
                out?.close()
                originalBitmap?.recycle()
                scaledBitmap?.recycle()
            }
        }
    }

    private fun handleReceivedContent() {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleReceivedImage(intent) // Handle single image being sent
                }
            }
            // TODO support multiple images in future release
            //intent?.action == Intent.ACTION_SEND_MULTIPLE
            //        && intent.type?.startsWith("image/") == true -> {
            //    handleSendMultipleImages(intent) // Handle multiple images being sent
            //}
            else -> {
                // Handle other intents, such as being started from the home screen
            }
        }
    }

    private fun handleReceivedImage(intent: Intent) {
        // TODO fix deprecated code here
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            imageContent = ImageContent(imageUrl = it)
            updateContent()
            // TODO fix image display ratio and such
        }
    }
}