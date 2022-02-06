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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class InputSource(val id: Int) : Parcelable {

    companion object {

        fun findById(id: Int) = when (id) {
            Usb.id -> Usb
            Optical.id -> Optical
            Coaxial.id -> Coaxial
            LineIn.id -> LineIn
            Bluetooth.id -> Bluetooth
            else -> null
        }
    }

    @Parcelize
    object Usb : InputSource(id = 1)

    @Parcelize
    object Coaxial : InputSource(id = 2)

    @Parcelize
    object Optical : InputSource(id = 3)

    @Parcelize
    object LineIn : InputSource(id = 4)

    @Parcelize
    object Bluetooth : InputSource(id = 5)
}
