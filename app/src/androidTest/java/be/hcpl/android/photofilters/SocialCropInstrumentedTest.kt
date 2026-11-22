package be.hcpl.android.photofilters

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class SocialCropInstrumentedTest {
    @Test
    fun imageCropKeepsTheCenterWithoutStretching() {
        val source = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        try {
            source.eraseColor(Color.BLUE)
            for (x in 92 until 227) {
                for (y in 0 until 240) source.setPixel(x, y, Color.RED)
            }
            val output = cropImage(source, SocialCropPreset.PORTRAIT_9_16)
            try {
                assertEquals(135, output.width)
                assertEquals(240, output.height)
                assertEquals(Color.RED, output.getPixel(0, 120))
                assertEquals(Color.RED, output.getPixel(134, 120))
            } finally {
                if (output !== source) output.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun videoCropPreservesAudioAndRequestedRatios() {
        SocialCropPreset.entries.forEach { verifyVideo("letterbox-fixture.mp4", it, 320, 240) }
    }

    @Test
    fun videoCropUsesDisplayedOrientation() {
        SocialCropPreset.entries.forEach { verifyVideo("letterbox-rotated-fixture.mp4", it, 240, 320) }
    }

    private fun verifyVideo(fixture: String, preset: SocialCropPreset, sourceWidth: Int, sourceHeight: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val input = File(context.cacheDir, "crop-test-input.mp4")
        val output = File(context.cacheDir, "crop-test-output.mp4")
        val done = CountDownLatch(1)
        val failure = AtomicReference<ExportException?>()
        try {
            instrumentation.context.assets.open(fixture).use { source ->
                input.outputStream().use { source.copyTo(it) }
            }
            lateinit var transformer: Transformer
            instrumentation.runOnMainSync {
                val edited = EditedMediaItem.Builder(MediaItem.fromUri(input.toURI().toString()))
                    .setEffects(Effects(emptyList(), listOf(socialCropEffect(preset))))
                    .build()
                transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            done.countDown()
                        }

                        override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                            failure.set(exception)
                            done.countDown()
                        }
                    })
                    .build()
                transformer.start(edited, output.absolutePath)
            }
            if (!done.await(60, TimeUnit.SECONDS)) {
                instrumentation.runOnMainSync { transformer.cancel() }
                throw AssertionError("Video export timed out")
            }
            failure.get()?.let { throw AssertionError("Video export failed", it) }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(output.absolutePath)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
                val displayWidth = if (rotation == 90 || rotation == 270) height else width
                val displayHeight = if (rotation == 90 || rotation == 270) width else height
                val expected = calculateCropBounds(sourceWidth, sourceHeight, preset)
                assertTrue("Unexpected width: $displayWidth", abs(displayWidth - expected.width) <= 2)
                assertTrue("Unexpected height: $displayHeight", abs(displayHeight - expected.height) <= 2)
                assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                val frame = retriever.getFrameAtTime(1_000_000)
                assertNotNull(frame)
                frame!!
                try {
                    assertEquals(displayWidth, frame.width)
                    assertEquals(displayHeight, frame.height)
                    assertTrue(Color.red(frame.getPixel(frame.width / 2, frame.height / 2)) > 180)
                } finally {
                    frame.recycle()
                }
            } finally {
                retriever.release()
            }
        } finally {
            input.delete()
            output.delete()
        }
    }
}
