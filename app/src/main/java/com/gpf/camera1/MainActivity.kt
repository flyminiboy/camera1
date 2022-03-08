package com.gpf.camera1

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.*
import com.gpf.camera1.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception
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
    private var encoderHandler: Handler? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrack: Int = 0
    private val lock = Any()

    private var path: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO 优化 角度问题
        dis = windowManager.defaultDisplay.apply {
            w = height
            h = width
        }

        val rtmpClient = RTMPClient()
        rtmpClient.connect("fff")


        setUpActionTip()
        binding.mainAction.setOnClickListener {
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

        binding.mainTv.surfaceTextureListener = (object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surfaceTexture = surface
                if (hasCameraPermission() && hasWritePermission()) {
                    startPreview()
                } else {
                    requestCameraPermisson()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (hasCameraPermission() && hasWritePermission()) {
            startPreview()
        } else {
            toast("no permission")
            finish()
        }
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

                mediaMuxer?.let { mediaMuxer -> // 这个段代码不写，视频黑屏无法播放，报错 moov atom not found
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


            //初始化 MediaMuxer（混合器） 将H264文件打包成MP4
            val file = File(filesDir, UUID.randomUUID().toString() + ".mp4")
            path = file.let {
                mediaMuxer =
                    MediaMuxer(it.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                mediaMuxer?.let {
//                    因为摄像头本身的视频是旋转了90度，所以设置90度，是为了摆正视频的方向
//                    还是和摄像头传感器旋转角度相关
                    it.setOrientationHint(90)
                }
                it.absolutePath
            }

            val encoderThread = HandlerThread("encode")
            encoderThread.start()
            encoderHandler = Handler(encoderThread.looper)

            isRecord = true
        }

    }

    private fun setUpActionTip() {
        binding.mainAction.text = if (isRecord) {
            "结束录制"
        } else {
            "开始录制"
        }
    }

    private fun nv21Tonv12(data: ByteArray): ByteArray {

        val res = ByteArray(data.size)

        System.arraycopy(data, 0, res, 0, w * h)
        val index = w * h
        for (i in index until data.size step 2) {
            res[i] = data[i + 1]
            res[i+1] = data[i]
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

                // TODO 优化2073600
                params.previewFormat = ImageFormat.NV21
                params.setPreviewSize(w, h)


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

                // 有内存优化
                val buffer = ByteArray(w * h * 3 / 2)
                it.addCallbackBuffer(buffer)
                it.setPreviewCallbackWithBuffer { data, camera ->
                    if (!isRecord) {
                        it.addCallbackBuffer(buffer)
                        return@setPreviewCallbackWithBuffer
                    }
                    encode(nv21Tonv12(data))
                    it.addCallbackBuffer(buffer)
                }

                it.startPreview()

                isPreview = true
            }


        } catch (e: Exception) {
        }
    }

    private fun encode(data: ByteArray) {
        encoderHandler?.let { handler ->
            handler.post {
                synchronized(lock) {
                    if (!isRecord) {
                        return@post
                    }
                    // 子线程开始处理数据 编码
                    mediaCodec?.let { mc ->
                        // 得到输入缓存队列的索引
                        val inputIndex = mc.dequeueInputBuffer(-1)
                        if (inputIndex > 0) {
                            // 根据输入缓存区的索引获取输入缓存区
                            mc.getInputBuffer(inputIndex)?.let { buffer ->
                                buffer.clear()
                                buffer.put(data, 0, data.size) // 填充数据

                                //将缓冲区在放回队列 内部开始编码
                                mc.queueInputBuffer(
                                    inputIndex, 0, data.size,
                                    System.nanoTime() / 1000, 0
                                )
                            }

                            while (true) {
                                // 开始获取输出缓冲区数据 - 编码以后的数据
                                val bufferInfo = MediaCodec.BufferInfo()
                                val outputIndex = mc.dequeueOutputBuffer(bufferInfo, 10_000)
                                // 稍后重试
                                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                    break
                                }
                                //输出格式发生变化 第一次总会调用，所以在这个地方开启混合器
                                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    mediaMuxer?.let { mm ->
                                        val nmf = mc.outputFormat
                                        videoTrack = mm.addTrack(nmf)
                                        mm.start() // 开始工作
                                    }
                                    continue
                                }
                                if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                    continue
                                }
                                // 获取输出缓冲区

                                mc.getOutputBuffer(outputIndex)?.let { buffer ->
                                    // 当前的buffer是配置信息
                                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                        bufferInfo.size = 0
                                    }
                                    if (bufferInfo.size != 0) {

                                        mediaMuxer?.let { mm ->
                                            // 设置数据开始偏移量
                                            buffer.position(bufferInfo.offset)
                                            // 设置数据长度
                                            buffer.limit(bufferInfo.offset + bufferInfo.size)
                                            // 混合器写到MP4文件中
                                            mm.writeSampleData(videoTrack, buffer, bufferInfo)
                                            // 释放输出数据缓冲区
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
    }

    companion object {

        init {
            System.loadLibrary("native-lib")
        }

    }

}

/**
 * nv21 数据格式为2个planar,也就是两个平面，第一个平面是所有的Y分量，而第二个平面是V和U交错平面
 * yyyy
 * yyyy
 * yyyy
 * yyyy
 * vuvu
 * vuvu
 *
 * COLOR_FormatYUV420SemiPlanar 即YUV420SP
 * yyyy
 * yyyy
 * yyyy
 * yyyy
 * uvuv
 * uvuv
 *
 * COLOR_FormatYUV420Planar 即YUV420P
 * yyyy
 * yyyy
 * yyyy
 * yyyy
 * uuuu
 * vvvv
 *
 */