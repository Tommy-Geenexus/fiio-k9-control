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

package com.tomg.fiiok9control.state.ui

import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.ItemInputBinding
import com.tomg.fiiok9control.state.InputSource

class ItemInputViewHolder(
    private val binding: ItemInputBinding,
    private val listener: StateAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.usb -> {
                    if (binding.usb.isPressed) {
                        listener.onInputSourceRequested(InputSource.Usb)
                    }
                }
                R.id.opt -> {
                    if (binding.opt.isPressed) {
                        listener.onInputSourceRequested(InputSource.Optical)
                    }
                }
                R.id.coax -> {
                    if (binding.coax.isPressed) {
                        listener.onInputSourceRequested(InputSource.Coaxial)
                    }
                }
                R.id.line -> {
                    if (binding.line.isPressed) {
                        listener.onInputSourceRequested(InputSource.LineIn)
                    }
                }
                R.id.bt -> {
                    if (binding.bt.isPressed) {
                        listener.onInputSourceRequested(InputSource.Bluetooth)
                    }
                }
            }
        }
    }

    fun bindItemInput(inputSource: InputSource) {
        when (inputSource) {
            InputSource.Bluetooth -> binding.bt.isChecked = true
            InputSource.Coaxial -> binding.coax.isChecked = true
            InputSource.LineIn -> binding.line.isChecked = true
            InputSource.Optical -> binding.opt.isChecked = true
            InputSource.Usb -> binding.usb.isChecked = true
        }
    }
}
