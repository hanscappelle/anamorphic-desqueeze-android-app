package be.hcpl.android.photofilters

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import be.hcpl.android.photofilters.ui.theme.AnamorphicDesqueezeTheme

data class MediaConfig(
    val imageUrl: Uri? = null,
    val videoUrl: Uri? = null,
    val aspectRatio: Float = ASPECT_RATIO_DEFAULT,
    val exportProgress: Int? = null,
)

@Composable
fun DesqueezeAppContent(
    modifier: Modifier = Modifier,
    content: MediaConfig = MediaConfig(),
    onGallery: () -> Unit = {},
    onResize: () -> Unit = {},
    onCancelExport: () -> Unit = {},
    selectRatio: (String) -> Unit = {},
) {
    val hasMedia = content.imageUrl != null || content.videoUrl != null
    val exporting = content.exportProgress != null
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.screen_heading), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.info_about_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.media_heading), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            when {
                                content.videoUrl != null -> R.string.video_selected
                                content.imageUrl != null -> R.string.image_selected
                                else -> R.string.media_not_selected
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!exporting) {
                        OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (hasMedia) R.string.action_change_media else R.string.action_open_gallery))
                        }
                    }
                }
            }
        }
        if (hasMedia) {
            item {
                RatioCard(
                    content = content,
                    selectRatio = selectRatio,
                    onResize = onResize,
                    onCancelExport = onCancelExport,
                )
            }
        }
        item {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.title_how_to_use), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.info_how_to_use),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/hanscappelle/anamorphic-desqueeze-android-app"),
                        )
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, R.string.error_no_browser, Toast.LENGTH_LONG).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_view_github))
                }
            }
        }
    }
}

@Composable
private fun RatioCard(
    content: MediaConfig,
    selectRatio: (String) -> Unit,
    onResize: () -> Unit,
    onCancelExport: () -> Unit,
) {
    val exporting = content.exportProgress != null
    val ratioIsCustom = content.aspectRatio != ASPECT_RATIO_1_33 && content.aspectRatio != ASPECT_RATIO_1_55
    var isCustom by rememberSaveable { mutableStateOf(ratioIsCustom) }
    var customRatio by rememberSaveable {
        mutableStateOf(if (ratioIsCustom) content.aspectRatio.toString() else "1.0")
    }

    LaunchedEffect(content.aspectRatio) {
        if (ratioIsCustom && !isCustom) {
            customRatio = content.aspectRatio.toString()
            isCustom = true
        }
    }

    Card(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.ratio_heading), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.ratio_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.selectableGroup()) {
                RatioOption(
                    label = stringResource(R.string.ratio_1_33),
                    selected = !isCustom && content.aspectRatio == ASPECT_RATIO_1_33,
                    enabled = !exporting,
                    onSelect = {
                        isCustom = false
                        selectRatio(ASPECT_RATIO_1_33.toString())
                    },
                )
                RatioOption(
                    label = stringResource(R.string.ratio_1_55),
                    selected = !isCustom && content.aspectRatio == ASPECT_RATIO_1_55,
                    enabled = !exporting,
                    onSelect = {
                        isCustom = false
                        selectRatio(ASPECT_RATIO_1_55.toString())
                    },
                )
                RatioOption(
                    label = stringResource(R.string.ratio_custom),
                    selected = isCustom,
                    enabled = !exporting,
                    onSelect = {
                        isCustom = true
                        selectRatio(customRatio)
                    },
                )
            }
            if (isCustom) {
                OutlinedTextField(
                    value = customRatio,
                    onValueChange = {
                        customRatio = it
                        selectRatio(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !exporting,
                    label = { Text(stringResource(R.string.custom_ratio_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
            if (content.exportProgress != null) {
                Text(
                    stringResource(R.string.video_export_progress, content.exportProgress),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { content.exportProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = if (exporting) onCancelExport else onResize,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (exporting) R.string.action_cancel_export else R.string.action_desqueeze))
            }
        }
    }
}

@Composable
private fun RatioOption(label: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AnamorphicDesqueezeTheme {
        DesqueezeAppContent(content = MediaConfig(imageUrl = Uri.EMPTY))
    }
}
