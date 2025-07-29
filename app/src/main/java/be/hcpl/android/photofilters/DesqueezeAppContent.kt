package be.hcpl.android.photofilters

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import be.hcpl.android.photofilters.ui.theme.AnamorphicDesqueezeTheme

@Composable
fun DesqueezeAppContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(16.dp),
        modifier = modifier.padding(16.dp),
    ) {
        AppInfo()
        DesqueezeInfo()
        DesqueezeImage()
    }
}

@Composable
fun AppInfo(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.info_about_app),
        modifier = modifier
    )
}

@Composable
fun DesqueezeInfo(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.info_how_to_use),
        modifier = modifier
    )
}

@Composable
fun DesqueezeImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_background),
        contentDescription = stringResource(R.string.contentDescription_sample_image),
        modifier = modifier.size(200.dp),
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AnamorphicDesqueezeTheme {
        DesqueezeAppContent()
    }
}