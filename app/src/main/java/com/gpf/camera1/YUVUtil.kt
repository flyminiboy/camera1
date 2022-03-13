package com.gpf.camera1

object YUVUtil {

    /**
     * nv21 转 I420
     * nv21原始数据
     * 宽
     * 高
     * i420目标数据
     */
    external fun nv21ToI420(src: ByteArray, width: Int, height: Int, dst: ByteArray)

    /**
     * 旋转i420数据
     * 宽
     * 高
     * 旋转以后i420目标数据
     * 角度
     */
    external fun rotateI420(
        src: ByteArray,
        width: Int,
        height: Int,
        dst: ByteArray,
        degree: Int
    )

    /**
     * I420 转 NV21
     * i420原始数据
     * 宽
     * 高
     * NV21目标数据
     */
    external fun i420ToNV21(
        src:ByteArray,
        width:Int,
        height:Int,
        dst:ByteArray
    )

    /**
     * nv21 转 ARGB
     * nv21 原始数据
     * 宽
     * 高
     * ARGB目标数据
     */
    external fun nv21ToARGB(
        src:ByteArray,
        width: Int,
        height: Int,
        dst:ByteArray
    )

}