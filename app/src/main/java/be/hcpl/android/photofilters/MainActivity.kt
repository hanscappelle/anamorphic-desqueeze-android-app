package be.hcpl.android.photofilters

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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

class MainActivity : ComponentActivity() {

    // TODO add ViewModel here to handle logic
    private var imageContent: ImageContent = ImageContent()

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
                        content = imageContent,
                        onResize = ::handleResizeImage
                    )
                }
            }
        }
    }

    private fun handleResizeImage() {
        // TODO make ratio configurable, support at least also 1,55x
        imageContent.imageUrl?.let { imageUri ->
            var originalBitmap: Bitmap? = null
            var scaledBitmap: Bitmap? = null
            try {
                originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
                scaledBitmap =
                    originalBitmap.scale((originalBitmap.width * 1.33).toInt(), originalBitmap.height)// keep height, change width by distortion value
                val fileName = "Desqueezed_${System.currentTimeMillis()}.JPG"
                insertImage(scaledBitmap, fileName)?.let {
                    displayCreatedImage(it)
                }
            } finally {
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
            else -> Unit // Handle other intents, such as being started from the home screen
        }
    }

    private fun handleReceivedImage(intent: Intent) {
        // TODO fix deprecated code here
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            imageContent = ImageContent(imageUrl = it)
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
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(MediaStore.Images.Media.DATE_ADDED, timestamp)
        values.put(MediaStore.Images.Media.DATE_TAKEN, timestamp)

        var uri: Uri? = null
        try {
            uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { uri ->
                out = cr.openOutputStream(uri)
                out?.let { out ->
                    source.compress(Bitmap.CompressFormat.JPEG, 85, out)
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

    private fun displayCreatedImage(uri: Uri) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "image/jpeg")
        startActivity(intent)
    }
}