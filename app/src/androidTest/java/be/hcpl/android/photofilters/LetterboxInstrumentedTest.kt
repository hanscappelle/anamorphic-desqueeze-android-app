package be.hcpl.android.photofilters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.effect.OverlayEffect
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

@RunWith(AndroidJUnit4::class)
class LetterboxInstrumentedTest {
    @Test
    fun imageBarsLeaveCenterAndCanvasUnchanged() {
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.RED)
            drawLetterboxBars(Canvas(bitmap), 2.39f)
            assertEquals(320, bitmap.width)
            assertEquals(240, bitmap.height)
            assertEquals(Color.BLACK, bitmap.getPixel(160, 10))
            assertEquals(Color.RED, bitmap.getPixel(160, 120))
            assertEquals(Color.BLACK, bitmap.getPixel(160, 230))
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun videoBarsPreserveDimensionsAndAudio() {
        verifyVideo("letterbox-fixture.mp4", 320, 240)
    }

    @Test
    fun videoBarsFollowDisplayedOrientation() {
        verifyVideo("letterbox-rotated-fixture.mp4", 240, 320)
    }

    private fun verifyVideo(fixture: String, expectedWidth: Int, expectedHeight: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val input = File(context.cacheDir, "letterbox-test-input.mp4")
        val output = File(context.cacheDir, "letterbox-test-output.mp4")
        val done = CountDownLatch(1)
        val failure = AtomicReference<ExportException?>()
        try {
            instrumentation.context.assets.open(fixture).use { source ->
                input.outputStream().use { source.copyTo(it) }
            }
            lateinit var transformer: Transformer
            instrumentation.runOnMainSync {
                val edited = EditedMediaItem.Builder(MediaItem.fromUri(input.toURI().toString()))
                    .setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(LetterboxOverlay(2.39f))))))
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
                assertEquals(expectedWidth, if (rotation == 90 || rotation == 270) height else width)
                assertEquals(expectedHeight, if (rotation == 90 || rotation == 270) width else height)
                assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                val frame = retriever.getFrameAtTime(1_000_000)
                assertNotNull(frame)
                frame!!
                try {
                    assertEquals(expectedWidth, frame.width)
                    assertEquals(expectedHeight, frame.height)
                    assertTrue(Color.red(frame.getPixel(expectedWidth / 2, 10)) < 30)
                    assertTrue(Color.red(frame.getPixel(expectedWidth / 2, expectedHeight / 2)) > 180)
                    assertTrue(Color.red(frame.getPixel(expectedWidth / 2, expectedHeight - 10)) < 30)
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
