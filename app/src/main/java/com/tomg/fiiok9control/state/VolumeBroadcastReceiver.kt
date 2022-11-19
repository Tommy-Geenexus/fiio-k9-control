/*
 * Copyright (c) 2021-2022, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.tomg.fiiok9control.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomg.fiiok9control.INTENT_ACTION_VOLUME_DOWN
import com.tomg.fiiok9control.INTENT_ACTION_VOLUME_MUTE
import com.tomg.fiiok9control.INTENT_ACTION_VOLUME_UP

class VolumeBroadcastReceiver(
    private val onVolumeUp: () -> Unit,
    private val onVolumeDown: () -> Unit,
    private val onVolumeMute: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(
        context: Context?,
        intent: Intent?
    ) {
        when (intent?.action) {
            INTENT_ACTION_VOLUME_UP -> {
                onVolumeUp()
            }
            INTENT_ACTION_VOLUME_DOWN -> {
                onVolumeDown()
            }
            INTENT_ACTION_VOLUME_MUTE -> {
                onVolumeMute()
            }
        }
    }
}
