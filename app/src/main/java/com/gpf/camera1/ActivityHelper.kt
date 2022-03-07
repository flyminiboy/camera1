package com.gpf.camera1

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val RC_PERMISSION_REQUEST = 9222

fun Activity.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) === PackageManager.PERMISSION_GRANTED
}

fun Activity.hasWritePermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) === PackageManager.PERMISSION_GRANTED

}

fun Activity.requestCameraPermisson() {

    val shouldRational = ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.CAMERA
    )

    if (shouldRational) {
        this.toast("camera permission is needed to run this application")
    } else {
        val permissions = Array(2){Manifest.permission.CAMERA;Manifest.permission.WRITE_EXTERNAL_STORAGE}
        ActivityCompat.requestPermissions(this, permissions, RC_PERMISSION_REQUEST)
    }

}

fun Activity.toast(msg:String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}