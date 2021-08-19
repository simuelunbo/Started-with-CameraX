package com.example.cameraxapp.extensions

import android.content.res.Resources


internal fun Float.fromDpToPx(): Int { // dp to px
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}