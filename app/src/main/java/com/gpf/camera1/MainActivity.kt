package com.gpf.camera1

import android.Manifest
import android.graphics.*
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import com.gpf.camera1.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.*


class MainActivity : AppCompatActivity() {

    private var camera: Camera? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var dis: Display
    private var w: Int = 0
    private var h: Int = 0

    private var isRecord = false
    private var isPreview = false

    private val encoderThread = HandlerThread("encode")
    private val encoderHandler by lazy {
        encoderThread.start()
        Handler(encoderThread.looper)
    }

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrack: Int = 0
    private val lock = Any()

    private var path: String? = null

    private val converThread = HandlerThread("conver")
    private val converHander by lazy {
        converThread.start()
        Handler(converThread.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            mainAction.setOnClickListener {
                if (!isPreview) {
                    return@setOnClickListener
                }
                if (isRecord) {
                    stopRecord()
                } else {
                    startRecord()
                }
                setUpActionTip()
            }

            mainTake.setOnClickListener {
                flag = false
            }

            mainTv.surfaceTextureListener = (object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    surfaceTexture = surface

                    PermissionX.init(this@MainActivity)
                        .permissions(Manifest.permission.CAMERA)
                        .request { allGranted, grantedList, deniedList ->
                            if (allGranted) {
                                startPreview()
                            } else {
                                toast("These permissions are denied $deniedList")
                            }
                        }

                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    release()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

                }

            })
        }

        setUpActionTip()


        // TODO ?????? ????????????
        dis = windowManager.defaultDisplay.apply {
            w = 1920
            h = 1080
        }

        val rtmpClient = RTMPClient()
        rtmpClient.connect("fff")

    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    private fun release() {
        stopPreview()
        startRecord()
    }

    private fun stopPreview() {
        if (!isPreview) {
            return
        }
        camera?.let { camera ->
            camera.stopPreview()
            camera.release()
            this.camera = null
            isPreview = false
        }
    }

    private fun stopRecord() {
        if (!isRecord) {
            return
        }
        synchronized(lock) {
            mediaCodec?.let { mediaCodec ->
                mediaCodec.stop()
                mediaCodec.release()
                this.mediaCodec = null

                mediaMuxer?.let { mediaMuxer -> // ????????????????????????????????????????????????????????? moov atom not found
                    mediaMuxer.stop()
                    mediaMuxer.release()
                }

                isRecord = false

                toast("video save [ $path ]")
            }
        }
    }

    private fun startRecord() {

        if (isRecord) {
            return
        }

        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC, w, h
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

        mediaCodec?.let { mediaCodec ->

            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()


            //????????? MediaMuxer??????????????? ???H264???????????????MP4
            val file = File(filesDir, UUID.randomUUID().toString() + ".mp4")
            path = file.let { it ->
                mediaMuxer =
                    MediaMuxer(it.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                mediaMuxer?.setOrientationHint(90)
                it.absolutePath
            }

            isRecord = true
        }

    }

    private fun setUpActionTip() {
        binding.mainAction.text = if (isRecord) {
            "????????????"
        } else {
            "????????????"
        }
    }

    private fun nv21Tonv12(data: ByteArray): ByteArray {

        val res = ByteArray(data.size)

        System.arraycopy(data, 0, res, 0, w * h)
        val index = w * h
        for (i in index until data.size step 2) {
            res[i] = data[i + 1]
            res[i + 1] = data[i]
        }

        return res
    }

    private fun startPreview() {
        if (isPreview) {
            return
        }

        try {

            stopPreview()

            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)

            camera?.let {

                val params = it.parameters

                // TODO ??????2073600
                params.previewFormat = ImageFormat.NV21
                // ?????????
                params.setPreviewSize(1920, 1080)


                it.parameters = params

                it.setPreviewTexture(surfaceTexture)

                if (dis.rotation == Surface.ROTATION_0) {
                    it.setDisplayOrientation(90)
                }
                if (dis.rotation == Surface.ROTATION_270) {
                    it.setDisplayOrientation(180)
                }

//                it.setPreviewCallback { data, camera ->
//                    if (!isRecord) {
//                        return@setPreviewCallback
//                    }
//                    encode(nv21Tonv12(data))
//                }

                // ???????????????
                val buffer = ByteArray(w * h * 3 / 2)
                it.addCallbackBuffer(buffer)
                it.setPreviewCallbackWithBuffer { data, camera ->
                    // ???????????????????????????libyuv????????????
                    if (!flag) {
                        converHander.post {

//                            val startTime = System.currentTimeMillis()
//                            logE("????????????")
//                            val i420 = ByteArray(data.size)
//                            YUVUtil.nv21ToI420(data, w, h, i420) // NV21 -> I420
//                            val dst = ByteArray(i420.size)
//                            YUVUtil.rotateI420(i420, w, h, dst, 90)
//                            val nv21 = ByteArray(dst.size)
//                            YUVUtil.i420ToNV21(dst, w, h, nv21)
//                            // ????????????
//                            logE("????????????,?????? [ " + (System.currentTimeMillis() - startTime) + " ]")
//
//                            val yuvImg = YuvImage(nv21, ImageFormat.NV21, w, h, null)
//                            val out = ByteArrayOutputStream()
//                            yuvImg.compressToJpeg(Rect(0, 0, w, h), 100, out)
//                            val bytes = out.toByteArray()
//                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            val startTime = System.currentTimeMillis()
                            logE("????????????")
                            val argb = ByteArray(w * h * 4)
                            YUVUtil.nv21ToARGB(data, w, h, argb)
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argb))
                            logE("????????????,?????? [ " + (System.currentTimeMillis() - startTime) + " ]")


                            flag = true
                            runOnUiThread {
                                // ??????

                                with(binding.mainFrame) {
                                    visibility = View.VISIBLE
                                    setImageBitmap(bitmap)
                                }


                            }

                        }
                    }
                    if (!isRecord) {
                        it.addCallbackBuffer(buffer)
                        return@setPreviewCallbackWithBuffer
                    }

                    logE("native libyuv ????????????")
                    val startTime = System.currentTimeMillis()
                    val nv12 = ByteArray(data.size)
                    YUVUtil.nv21ToNV12(data, w, h, nv12) // 3 - 5 ??????
                    logE("native libyuv ???????????? [ " + (System.currentTimeMillis() - startTime) + " ]")

                    logE("java ????????????")
                    val startTime2 = System.currentTimeMillis()
                    val dst = nv21Tonv12(data) // 12 - 15??????
                    logE("java ???????????? [ " + (System.currentTimeMillis() - startTime) + " ]")

                    encode(dst)
                    it.addCallbackBuffer(buffer)
                }

                it.startPreview()

                isPreview = true
            }

        } catch (e: Exception) {
            logE("fuck ${e.message}")
        }
    }

    var flag = true

    private fun encode(data: ByteArray) {
        encoderHandler.post {
            synchronized(lock) {
                if (!isRecord) {
                    return@post
                }
                // ??????????????????????????? ??????
                mediaCodec?.let { mc ->
                    // ?????????????????????????????????
                    val inputIndex = mc.dequeueInputBuffer(-1)
                    if (inputIndex > 0) {
                        // ???????????????????????????????????????????????????
                        mc.getInputBuffer(inputIndex)?.let { buffer ->
                            buffer.clear()
                            buffer.put(data, 0, data.size) // ????????????

                            //??????????????????????????? ??????????????????
                            mc.queueInputBuffer(
                                inputIndex, 0, data.size,
                                System.nanoTime() / 1000, 0
                            )
                        }

                        while (true) {
                            // ????????????????????????????????? - ?????????????????????
                            val bufferInfo = MediaCodec.BufferInfo()
                            val outputIndex = mc.dequeueOutputBuffer(bufferInfo, 10_000)
                            // ????????????
                            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                break
                            }
                            //???????????????????????? ????????????????????????????????????????????????????????????
                            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                mediaMuxer?.let { mm ->
                                    val nmf = mc.outputFormat
                                    videoTrack = mm.addTrack(nmf)
                                    mm.start() // ????????????
                                }
                                continue
                            }
                            if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                continue
                            }
                            // ?????????????????????

                            mc.getOutputBuffer(outputIndex)?.let { buffer ->
                                // ?????????buffer???????????????
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    bufferInfo.size = 0
                                }
                                if (bufferInfo.size != 0) {

                                    mediaMuxer?.let { mm ->
                                        // ???????????????????????????
                                        buffer.position(bufferInfo.offset)
                                        // ??????????????????
                                        buffer.limit(bufferInfo.offset + bufferInfo.size)
                                        // ???????????????MP4?????????
                                        mm.writeSampleData(videoTrack, buffer, bufferInfo)
                                        // ???????????????????????????
                                        mc.releaseOutputBuffer(outputIndex, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {

        init {
            System.loadLibrary("native-lib")
        }

    }

}

/**
 * nv21 Android???????????? ???????????????2???planar,???????????????????????????????????????????????????Y??????????????????????????????V???U????????????
 * yyyy
 * yyyy
 * yyyy
 * yyyy
 * vuvu
 * vuvu
 *
 * nv12 IOS???????????? ???????????????2???planar,???????????????????????????????????????????????????Y??????????????????????????????U???V????????????
 * yyyy
 * yyyy
 * yyyy
 * yyyy
 * uvuv
 * uvuv
 *
 * COLOR_FormatYUV420SemiPlanar ???YUV420SP
 * yyyy
 * yyyy
 * yyyy
 * yyyy
 * uvuv
 * uvuv
 *
 * COLOR_FormatYUV420Planar ???YUV420P I420
 * yyyy
 * yyyy
 * yyyy
 * yyyy
 * uuuu
 * vvvv
 *
 */