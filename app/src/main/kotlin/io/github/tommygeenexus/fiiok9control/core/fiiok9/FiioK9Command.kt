/*
 * Copyright (c) 2021-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.core.fiiok9

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class FiioK9Command(val commandId: Int) : Parcelable {

    sealed class Get(commandId: Int) : FiioK9Command(commandId) {

        @Parcelize
        data object AudioFormat : Get(commandId = 0x044c)

        @Parcelize
        data object ChannelBalance : Get(commandId = 0x0413)

        @Parcelize
        data object CodecBit : Get(commandId = 0x0416)

        @Parcelize
        data object CodecEnabled : Get(commandId = 0x0417)

        @Parcelize
        data object EqEnabled : Get(commandId = 0x0419)

        @Parcelize
        data object EqPreSet : Get(commandId = 0x0435)

        @Parcelize
        data object IndicatorRgbLighting : Get(commandId = 0x043d)

        @Parcelize
        data object InputSource : Get(commandId = 0x0448)

        @Parcelize
        data object LowPassFilter : Get(commandId = 0x0411)

        @Parcelize
        data object MqaEnabled : Get(commandId = 0x044F)

        @Parcelize
        data object MuteEnabled : Get(commandId = 0x044a)

        @Parcelize
        data object Simultaneously : Get(commandId = 0x044D)

        @Parcelize
        data object Version : Get(commandId = 0x0418)

        @Parcelize
        data object Volume : Get(commandId = 0x0412)
    }

    sealed class Set(commandId: Int) : FiioK9Command(commandId) {

        @Parcelize
        data object ChannelBalance : Set(commandId = 0x0403)

        @Parcelize
        data object CodecEnabled : Set(commandId = 0x0407)

        @Parcelize
        data object EqEnabled : Set(commandId = 0x0408)

        @Parcelize
        data object EqPreSet : Set(commandId = 0x0423)

        @Parcelize
        data object EqValue : Set(commandId = 0x0409)

        @Parcelize
        data object IndicatorRgbLighting : Set(commandId = 0x043e)

        @Parcelize
        data object InputSource : Set(commandId = 0x0449)

        @Parcelize
        data object LowPassFilter : Set(commandId = 0x0401)

        @Parcelize
        data object MqaEnabled : Set(commandId = 0x0450)

        @Parcelize
        data object MuteEnabled : Set(commandId = 0x044b)

        @Parcelize
        data object Restore : Set(commandId = 0x0404)

        @Parcelize
        data object Simultaneously : Set(commandId = 0x044E)

        @Parcelize
        data object Standby : Set(commandId = 0x0425)

        @Parcelize
        data object Volume : Set(commandId = 0x0402)
    }
}
