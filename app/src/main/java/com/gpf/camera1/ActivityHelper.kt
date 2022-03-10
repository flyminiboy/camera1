package com.gpf.camera1

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun Activity.toast(msg:String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}