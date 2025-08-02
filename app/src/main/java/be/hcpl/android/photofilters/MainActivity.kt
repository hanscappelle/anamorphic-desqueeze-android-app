package be.hcpl.android.photofilters

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
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
import java.io.OutputStream
import java.lang.Float.parseFloat
import kotlin.Float

class MainActivity : ComponentActivity() {

    // TODO add ViewModel here to handle logic
    private var imageConfig: ImageConfig = ImageConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateContent()
        handleReceivedContent() // checks for received images
    }

    private fun updateContent() {
        setContent {
            AnamorphicDesqueezeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DesqueezeAppContent(
                        modifier = Modifier.padding(innerPadding),
                        content = imageConfig,
                        onGallery = { handleOpenGallery(null) },
                        selectRatio = ::updateRatio,
                        onResize = ::handleResizeImage,
                    )
                }
            }
        }
    }

    private fun updateRatio(newRatioValue: String){
        val newRatio = try {
            parseFloat(newRatioValue)
        } catch (_: NumberFormatException){
            0f
        }
        imageConfig = imageConfig.copy(aspectRatio = newRatio)
        updateContent()
    }

    private fun handleResizeImage() {
        imageConfig.imageUrl?.let { imageUri ->
            var originalBitmap: Bitmap? = null
            var scaledBitmap: Bitmap? = null
            try {
                originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
                scaledBitmap =
                    originalBitmap.scale(
                        (originalBitmap.width * imageConfig.aspectRatio).toInt(),
                        originalBitmap.height
                    )// keep height, change width by distortion value
                val fileName = "${PREFIX}_${System.currentTimeMillis()}.${EXT_JPEG}"
                insertImage(scaledBitmap, fileName)?.let {
                    handleOpenGallery(it)
                }
            } finally {
                originalBitmap?.recycle()
                scaledBitmap?.recycle()
                // remove image content set before
                imageConfig = ImageConfig()
                updateContent()
            }
        }
    }

    private fun handleReceivedContent() {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith(INTENT_TYPE_ALL_IMAGE) == true) {
                    handleReceivedImage(intent) // Handle single image being sent
                }
            }
            // TODO support multiple images in future release
            //intent?.action == Intent.ACTION_SEND_MULTIPLE
            //        && intent.type?.startsWith("image/") == true -> {
            //    handleSendMultipleImages(intent) // Handle multiple images being sent
            //}
            else -> Unit // Handle other intents, such as being started from the home screen
        }
    }

    private fun handleReceivedImage(intent: Intent) {
        val uri = if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java) as? Uri
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        }
        uri?.let {
            imageConfig = ImageConfig(imageUrl = it)
            updateContent()
        }
    }

    private fun insertImage(source: Bitmap, name: String): Uri? {

        var out: OutputStream? = null
        val cr = applicationContext.contentResolver
        val timestamp = System.currentTimeMillis()

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, name)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name)
        //values.put(MediaStore.Images.Media.DESCRIPTION, description)
        values.put(MediaStore.Images.Media.MIME_TYPE, IMAGE_JPEG)
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(MediaStore.Images.Media.DATE_ADDED, timestamp)
        values.put(MediaStore.Images.Media.DATE_TAKEN, timestamp)

        var uri: Uri? = null
        try {
            uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { uri ->
                out = cr.openOutputStream(uri)
                out?.let { out ->
                    source.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION, out)
                }
            }
        } catch (_: Exception) {
            // do some clean up, something clearly failed
            uri?.let { cr.delete(it, null, null) }
        } finally {
            out?.close()
        }
        return uri // reference to inserted image
    }

    private fun handleOpenGallery(uri: Uri?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        uri?.let {
            intent.setDataAndType(it, IMAGE_JPEG)
        } ?: intent.setType(IMAGE_JPEG)
        startActivity(intent)
    }

}