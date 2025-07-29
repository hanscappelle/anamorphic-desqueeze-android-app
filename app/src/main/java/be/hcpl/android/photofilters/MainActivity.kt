package be.hcpl.android.photofilters

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import be.hcpl.android.photofilters.ui.theme.AnamorphicDesqueezeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent(ImageContent())
        // TODO add ViewModel here to handle logic
        // check for received images
        handleReceivedContent()

    }

    private fun setContent(imageContent: ImageContent) {
        setContent {
            AnamorphicDesqueezeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DesqueezeAppContent(
                        modifier = Modifier.padding(innerPadding),
                        content = imageContent,
                    )
                }
            }
        }
    }


    private fun handleReceivedContent() {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent) // Handle single image being sent
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

    private fun handleSendImage(intent: Intent) {
        // TODO fix deprecated code here
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            setContent(ImageContent(imageUrl = it))
            // TODO Update UI to reflect image being shared + options
        }
    }
}