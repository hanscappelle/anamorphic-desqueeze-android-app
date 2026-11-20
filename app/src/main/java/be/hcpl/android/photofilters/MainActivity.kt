package be.hcpl.android.photofilters

import android.content.ContentValues
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.scale
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.lang.Float.parseFloat
import kotlin.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    private var mediaConfig by mutableStateOf(MediaConfig())
    private val exportHandler = Handler(Looper.getMainLooper())
    private var videoTransformer: Transformer? = null
    private var videoOutputFile: File? = null
    private var progressRunnable: Runnable? = null
    private var exportCancelled = false
    private var pendingExport: (() -> Unit)? = null
    private val stateObserver = Observer<MainViewModel.State>(::handleNewState)

    private val mediaPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val type = try {
                    contentResolver.getType(uri)
                } catch (_: SecurityException) {
                    Toast.makeText(this, R.string.error_media_access, Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
                when {
                    type?.startsWith(INTENT_TYPE_ALL_IMAGE) == true -> selectMedia(uri, isVideo = false)
                    type?.startsWith(INTENT_TYPE_ALL_VIDEO) == true -> selectMedia(uri, isVideo = true)
                    else -> Toast.makeText(this, R.string.error_unsupported_media, Toast.LENGTH_LONG).show()
                }
            }
        }

    private val writePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val export = pendingExport
            pendingExport = null
            if (granted) {
                export?.invoke()
            } else {
                Toast.makeText(
                    this,
                    R.string.video_export_permission_required,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setAppContent()
        if (savedInstanceState != null) {
            restoreSelectedMedia(savedInstanceState)
        } else {
            handleReceivedContent()
        }
        viewModel.state.observeForever(stateObserver)
    }

    private fun handleNewState(state: MainViewModel.State) {
        mediaConfig = mediaConfig.copy(
            aspectRatio = state.lastUsedRatio,
        )
    }

    private fun setAppContent() {
        setContent {
            AppScaffold {
                DesqueezeAppContent(
                    content = mediaConfig,
                    onGallery = ::openMediaPicker,
                    selectRatio = ::updateRatio,
                    selectMode = { mediaConfig = mediaConfig.copy(mode = it) },
                    selectLetterboxRatio = {
                        mediaConfig = mediaConfig.copy(letterboxRatio = it.toFloatOrNull() ?: 0f)
                    },
                    onResize = ::handleResizeMedia,
                    onCancelExport = ::cancelVideoExport,
                )
            }
        }
    }

    private fun openMediaPicker() {
        try {
            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_media_picker_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateRatio(newRatioValue: String) {
        // TODO move this logic to viewModel
        val newRatio = try {
            parseFloat(newRatioValue)
        } catch (_: NumberFormatException) {
            0f
        }
        mediaConfig = mediaConfig.copy(aspectRatio = newRatio)
        viewModel.updateLastUsedRatio(newRatio)
    }

    private fun handleResizeMedia() {
        val ratio = if (mediaConfig.mode == ConversionMode.LETTERBOX) {
            mediaConfig.letterboxRatio
        } else {
            mediaConfig.aspectRatio
        }
        if (!ratio.isFinite() || ratio <= 0f) {
            Toast.makeText(this, R.string.error_invalid_ratio, Toast.LENGTH_LONG).show()
            return
        }
        when {
            mediaConfig.imageUrl != null && mediaConfig.mode == ConversionMode.LETTERBOX -> handleLetterboxImage()
            mediaConfig.imageUrl != null -> handleResizeImage()
            mediaConfig.videoUrl != null -> requestVideoExportPermission()
        }
    }

    private fun handleLetterboxImage() {
        val uri = mediaConfig.imageUrl ?: return
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var output: Bitmap? = null
        try {
            val orientation = contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: throw IOException("Could not open the image.")
            decoded = contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                ?: throw IOException("Could not decode the image.")
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                }
            }
            oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            calculateLetterboxBars(oriented.width, oriented.height, mediaConfig.letterboxRatio)
            output = oriented.copy(Bitmap.Config.ARGB_8888, true)
                ?: throw IOException("Could not create an output image.")
            drawLetterboxBars(Canvas(output), mediaConfig.letterboxRatio)
            val fileName = "${PREFIX}_${System.currentTimeMillis()}.${EXT_JPEG}"
            val savedUri = insertImage(output, fileName) ?: throw IOException("Could not save the image.")
            mediaConfig = mediaConfig.copy(imageUrl = null, videoUrl = null)
            handleOpenGallery(savedUri)
        } catch (_: InvalidLetterboxRatioException) {
            Toast.makeText(this, R.string.error_letterbox_ratio, Toast.LENGTH_LONG).show()
        } catch (exception: IOException) {
            Toast.makeText(this, "${getString(R.string.image_export_failed)} ${exception.localizedMessage.orEmpty()}", Toast.LENGTH_LONG).show()
        } catch (exception: SecurityException) {
            Toast.makeText(this, "${getString(R.string.image_export_failed)} ${exception.localizedMessage.orEmpty()}", Toast.LENGTH_LONG).show()
        } finally {
            if (output !== oriented && output !== decoded) output?.recycle()
            if (oriented !== decoded) oriented?.recycle()
            decoded?.recycle()
        }
    }

    private fun requestVideoExportPermission() {
        if (VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pendingExport = ::startVideoExport
            writePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startVideoExport()
        }
    }

    private fun handleResizeImage() {
        // TODO move to viewModel
        mediaConfig.imageUrl?.let { imageUri ->
            var originalBitmap: Bitmap? = null
            var scaledBitmap: Bitmap? = null
            try {
                originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
                val calculatedWidth = (originalBitmap.width * mediaConfig.aspectRatio).toInt()
                val calculatedHeight = originalBitmap.height
                // keep height, change width by distortion value
                if (calculatedWidth > 0 && calculatedHeight > 0) {
                    scaledBitmap = originalBitmap.scale(calculatedWidth, calculatedHeight)
                    val fileName = "${PREFIX}_${System.currentTimeMillis()}.${EXT_JPEG}"
                    insertImage(scaledBitmap, fileName)?.let {
                        handleOpenGallery(it)
                    }
                } else {
                    Toast.makeText(this, "Problem with received image", Toast.LENGTH_LONG).show()
                }
            } finally {
                originalBitmap?.recycle()
                scaledBitmap?.recycle()
                // remove image content set before
                mediaConfig = mediaConfig.copy(imageUrl = null, videoUrl = null)
            }
        }
    }

    private fun startVideoExport() {
        val sourceUri = mediaConfig.videoUrl ?: return
        val outputFile = File(cacheDir, "desqueezed_${System.currentTimeMillis()}.mp4")
        videoOutputFile = outputFile
        exportCancelled = false
        mediaConfig = mediaConfig.copy(exportProgress = 0)

        try {
            val videoEffect = if (mediaConfig.mode == ConversionMode.LETTERBOX) {
                validateVideoLetterbox(sourceUri, mediaConfig.letterboxRatio)
                OverlayEffect(listOf(LetterboxOverlay(mediaConfig.letterboxRatio)))
            } else {
                ScaleAndRotateTransformation.Builder()
                    .setScale(mediaConfig.aspectRatio, 1f)
                    .build()
            }
            val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
                .setEffects(
                    Effects(
                        emptyList(),
                        listOf(videoEffect),
                    )
                )
                .build()
            val transformer = Transformer.Builder(this)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            if (exportCancelled) return
                            stopProgressUpdates()
                            videoTransformer = null
                            try {
                                val savedUri = saveVideoToGallery(outputFile)
                                outputFile.delete()
                                videoOutputFile = null
                                mediaConfig = mediaConfig.copy(imageUrl = null, videoUrl = null, exportProgress = null)
                                handleOpenVideo(savedUri)
                            } catch (exception: Exception) {
                                handleVideoExportFailure(exception)
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            result: ExportResult,
                            exception: ExportException,
                        ) {
                            if (!exportCancelled) handleVideoExportFailure(exception)
                        }
                    }
                )
                .build()
            videoTransformer = transformer
            transformer.start(editedMediaItem, outputFile.absolutePath)
            monitorExportProgress(transformer)
        } catch (_: InvalidLetterboxRatioException) {
            videoOutputFile?.delete()
            videoOutputFile = null
            mediaConfig = mediaConfig.copy(exportProgress = null)
            Toast.makeText(this, R.string.error_letterbox_ratio, Toast.LENGTH_LONG).show()
        } catch (exception: Exception) {
            handleVideoExportFailure(exception)
        }
    }

    private fun validateVideoLetterbox(uri: Uri, ratio: Float) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                ?: throw IOException("Could not read the video width.")
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                ?: throw IOException("Could not read the video height.")
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                calculateLetterboxBars(height, width, ratio)
            } else {
                calculateLetterboxBars(width, height, ratio)
            }
        } finally {
            retriever.release()
        }
    }

    private fun monitorExportProgress(transformer: Transformer) {
        val runnable = object : Runnable {
            override fun run() {
                if (videoTransformer !== transformer) return
                val progressHolder = ProgressHolder()
                if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    mediaConfig = mediaConfig.copy(exportProgress = progressHolder.progress)
                }
                exportHandler.postDelayed(this, 500)
            }
        }
        progressRunnable = runnable
        exportHandler.post(runnable)
    }

    private fun cancelVideoExport() {
        exportCancelled = true
        stopProgressUpdates()
        videoTransformer?.cancel()
        videoTransformer = null
        videoOutputFile?.delete()
        videoOutputFile = null
        mediaConfig = mediaConfig.copy(exportProgress = null)
        Toast.makeText(this, R.string.video_export_cancelled, Toast.LENGTH_SHORT).show()
    }

    private fun handleVideoExportFailure(exception: Exception) {
        stopProgressUpdates()
        videoTransformer = null
        videoOutputFile?.delete()
        videoOutputFile = null
        mediaConfig = mediaConfig.copy(exportProgress = null)
        Toast.makeText(
            this,
            "${getString(R.string.video_export_failed)} ${exception.localizedMessage.orEmpty()}",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let(exportHandler::removeCallbacks)
        progressRunnable = null
    }

    private fun saveVideoToGallery(source: File): Uri {
        val fileName = "${PREFIX}_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/AnamorphicDesqueeze",
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create a gallery entry for the video.")
        try {
            val output = contentResolver.openOutputStream(uri)
                ?: throw IOException("Could not open the gallery entry for writing.")
            output.use { stream ->
                source.inputStream().use { input -> input.copyTo(stream) }
            }
            if (VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val published = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                if (contentResolver.update(uri, published, null, null) == 0) {
                    throw IOException("Could not publish the exported video.")
                }
            }
        } catch (exception: Exception) {
            contentResolver.delete(uri, null, null)
            throw exception
        }
        return uri
    }

    private fun handleReceivedContent() {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith(INTENT_TYPE_ALL_IMAGE) == true) {
                    handleReceivedMedia(intent, isVideo = false)
                } else if (intent.type?.startsWith(INTENT_TYPE_ALL_VIDEO) == true) {
                    handleReceivedMedia(intent, isVideo = true)
                }
            }
            else -> Unit // Handle other intents, such as being started from the home screen
        }
    }

    private fun handleReceivedMedia(intent: Intent, isVideo: Boolean) {
        // TODO move to viewModel
        val uri = if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java) as? Uri
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        }
        uri?.let {
            selectMedia(it, isVideo)
        }
    }

    private fun selectMedia(uri: Uri, isVideo: Boolean) {
        mediaConfig = mediaConfig.copy(
            imageUrl = if (isVideo) null else uri,
            videoUrl = if (isVideo) uri else null,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selected_image_uri", mediaConfig.imageUrl?.toString())
        outState.putString("selected_video_uri", mediaConfig.videoUrl?.toString())
        outState.putString("conversion_mode", mediaConfig.mode.name)
        outState.putFloat("letterbox_ratio", mediaConfig.letterboxRatio)
    }

    private fun restoreSelectedMedia(savedState: Bundle) {
        val mode = when (savedState.getString("conversion_mode")) {
            ConversionMode.LETTERBOX.name -> ConversionMode.LETTERBOX
            else -> ConversionMode.DESQUEEZE
        }
        mediaConfig = mediaConfig.copy(mode = mode, letterboxRatio = savedState.getFloat("letterbox_ratio", 2.39f))
        savedState.getString("selected_image_uri")?.let { selectMedia(Uri.parse(it), isVideo = false) }
        savedState.getString("selected_video_uri")?.let { selectMedia(Uri.parse(it), isVideo = true) }
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

    private fun handleOpenVideo(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.video_saved_no_player, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        exportCancelled = true
        stopProgressUpdates()
        videoTransformer?.cancel()
        videoOutputFile?.delete()
        viewModel.state.removeObserver(stateObserver)
        super.onDestroy()
    }

}