/*
 * Copyright (c) 2021-2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.tomg.fiiok9control.gaia.fiio

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class K9Command(val commandId: Int) : Parcelable {

    sealed class Get(commandId: Int) : K9Command(commandId) {

        @Parcelize
        object AudioFormat : Get(commandId = 0x044c)

        @Parcelize
        object ChannelBalance : Get(commandId = 0x0413)

        @Parcelize
        object CodecBit : Get(commandId = 0x0416)

        @Parcelize
        object CodecEnabled : Get(commandId = 0x0417)

        @Parcelize
        object EqEnabled : Get(commandId = 0x0419)

        @Parcelize
        object EqPreSet : Get(commandId = 0x0435)

        @Parcelize
        object IndicatorRgbLighting : Get(commandId = 0x043d)

        @Parcelize
        object InputSource : Get(commandId = 0x0448)

        @Parcelize
        object LowPassFilter : Get(commandId = 0x0411)

        @Parcelize
        object MqaEnabled : Get(commandId = 0x044F)

        @Parcelize
        object MuteEnabled : Get(commandId = 0x044a)

        @Parcelize
        object Simultaneously : Get(commandId = 0x044D)

        @Parcelize
        object Version : Get(commandId = 0x0418)

        @Parcelize
        object Volume : Get(commandId = 0x0412)
    }

    sealed class Set(commandId: Int) : K9Command(commandId) {

        @Parcelize
        object ChannelBalance : Set(commandId = 0x0403)

        @Parcelize
        object CodecEnabled : Set(commandId = 0x0407)

        @Parcelize
        object EqEnabled : Set(commandId = 0x0408)

        @Parcelize
        object EqPreSet : Set(commandId = 0x0423)

        @Parcelize
        object EqValue : Set(commandId = 0x0409)

        @Parcelize
        object IndicatorRgbLighting : Set(commandId = 0x043e)

        @Parcelize
        object InputSource : Set(commandId = 0x0449)

        @Parcelize
        object LowPassFilter : Set(commandId = 0x0401)

        @Parcelize
        object MqaEnabled : Set(commandId = 0x0450)

        @Parcelize
        object MuteEnabled : Set(commandId = 0x044b)

        @Parcelize
        object Restore : Set(commandId = 0x0404)

        @Parcelize
        object Simultaneously : Set(commandId = 0x044E)

        @Parcelize
        object Standby : Set(commandId = 0x0425)

        @Parcelize
        object Volume : Set(commandId = 0x0402)
    }
}
