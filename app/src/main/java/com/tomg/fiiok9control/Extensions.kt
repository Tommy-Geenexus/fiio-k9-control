/*
 * Copyright (c) 2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tomg.fiiok9control

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBREDR
import dev.chrisbanes.insetter.applyInsetter
import kotlin.experimental.and

fun View.applyInsetMargins() {
    applyInsetter {
        type(statusBars = true) {
            margin()
        }
    }
}

fun View.showSnackbar(
    anchor: View? = null,
    @StringRes msgRes: Int,
    @StringRes actionMsgRes: Int = 0,
    onActionClick: View.OnClickListener? = null,
    length: Int = Snackbar.LENGTH_SHORT
) {
    var backingSnackbar: Snackbar? = Snackbar
        .make(this, context.getString(msgRes), length)
        .setAnchorView(anchor)
        .setAction(if (actionMsgRes != 0) context.getString(actionMsgRes) else null, onActionClick)
    val snackbar = backingSnackbar
    val lifecycle = findViewTreeLifecycleOwner()?.lifecycle
    if (snackbar != null && lifecycle != null) {
        lifecycle.addObserver(
            object : LifecycleEventObserver {

                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            lifecycle.removeObserver(this)
                            snackbar.dismiss()
                            backingSnackbar = null
                        }
                        else -> {
                        }
                    }
                }
            }
        )
        snackbar.show()
    } else {
        backingSnackbar = null
    }
}

fun ByteArray.toHexString(): String {
    if (size == 0) return String.Empty
    val sb = StringBuffer()
    for (b in this) {
        val s = (b and GaiaPacketBREDR.SOF).toString(radix = 16)
        if (s.length < 2) sb.append(0)
        sb.append(s)
    }
    return sb.toString()
}
