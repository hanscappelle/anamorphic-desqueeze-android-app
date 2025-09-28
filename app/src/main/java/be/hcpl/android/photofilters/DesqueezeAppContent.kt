package be.hcpl.android.photofilters

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.hcpl.android.photofilters.ui.theme.AnamorphicDesqueezeTheme

@Composable
fun DesqueezeAppContent(
    modifier: Modifier = Modifier,
    content: ImageConfig = ImageConfig(),
    onGallery: () -> Unit = {},
    onResize: () -> Unit = {},
    selectRatio: (String) -> Unit = {},
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(16.dp),
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        item {

            //Title(stringResource(R.string.app_name))
            AppInfo()
        }
        item {
            Title(stringResource(R.string.title_how_to_use))
        }
        item {
            DesqueezeInfo()
        }
        item {
            // action depends on image selected or not
            if (content.imageUrl == null) {
                OpenGalleryAction(onGallery)
            } else {
                DesqueezeAction(content, onResize, selectRatio)
            }
        }
        item {
            DesqueezeImage(Modifier.fillMaxWidth())
        }
    }
}

data class ImageConfig(
    val imageUrl: Uri? = null,
    val aspectRatio: Float = ASPECT_RATIO_DEFAULT,
)

@Composable
fun AppInfo(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.info_about_app),
        modifier = modifier
    )
}

@Composable
fun Title(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        maxLines = 1,
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
fun OpenGalleryAction(
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = { onOpenGallery() },
    ) {
        Text(stringResource(R.string.action_open_gallery))
    }
}

@Composable
fun DesqueezeAction(
    content: ImageConfig,
    onResize: () -> Unit,
    selectRatio: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(16.dp),
        modifier = modifier,
    ) {
        Text(text = stringResource(R.string.hint_ratio))
        Column {
            RatioSelect(ASPECT_RATIO_1_33, content.aspectRatio, content.aspectRatio == ASPECT_RATIO_1_33, selectRatio)
            RatioSelect(ASPECT_RATIO_1_55, content.aspectRatio, content.aspectRatio == ASPECT_RATIO_1_55, selectRatio)
            RatioSelect(
                ASPECT_RATIO_CUSTOM,
                content.aspectRatio,
                content.aspectRatio != ASPECT_RATIO_1_33 && content.aspectRatio != ASPECT_RATIO_1_55,
                selectRatio
            )
        }
        Button(
            onClick = { onResize() },
        ) {
            Text(stringResource(R.string.action_desqueeze))
        }
    }
}

@Composable
fun RatioSelect(initialValue: Float, ratio: Float, selected: Boolean, selectRatio: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = { selectRatio("$initialValue") },
        )
        if (initialValue == 0f) {
            TextField(
                value = "$ratio",
                onValueChange = { newValue -> selectRatio(newValue) },
                modifier = Modifier.width(100.dp),
            )

        } else {
            Text(
                text = "$initialValue",
                modifier = Modifier.clickable {
                    selectRatio("$initialValue")
                },
            )
        }
    }
}

@Composable
fun DesqueezeImage(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(16.dp),
        modifier = modifier,
    ) {

        Image(
            painter = painterResource(id = R.drawable.screenshot_01),
            contentDescription = stringResource(R.string.contentDescription_screenshot_01),
            modifier = Modifier.size(width = 300.dp, height = Dp.Unspecified),
        )

        Image(
            painter = painterResource(id = R.drawable.screenshot_02),
            contentDescription = stringResource(R.string.contentDescription_screenshot_02),
            modifier = Modifier.size(width = 300.dp, height = Dp.Unspecified),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AnamorphicDesqueezeTheme {
        DesqueezeAppContent(content = ImageConfig(imageUrl = Uri.EMPTY))
    }
}