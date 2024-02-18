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

package io.github.tommygeenexus.fiiok9control.core.fiiok9.feature

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
    data object Usb : InputSource(id = 1)

    @Parcelize
    data object Coaxial : InputSource(id = 2)

    @Parcelize
    data object Optical : InputSource(id = 3)

    @Parcelize
    data object LineIn : InputSource(id = 4)

    @Parcelize
    data object Bluetooth : InputSource(id = 5)
}

fun InputSource?.orDefault() = this ?: InputSource.Usb
