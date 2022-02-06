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

package com.tomg.fiiok9control.audio.ui

import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.databinding.ItemLowPassFilterBinding

class ItemLowPassFilterViewHolder(
    private val binding: ItemLowPassFilterBinding,
    private val listener: AudioAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.sharp -> {
                    if (binding.sharp.isPressed) {
                        listener.onLowPassFilterRequested(LowPassFilter.Sharp)
                    }
                }
                R.id.slow -> {
                    if (binding.slow.isPressed) {
                        listener.onLowPassFilterRequested(LowPassFilter.Slow)
                    }
                }
                R.id.sharp_delay -> {
                    if (binding.sharpDelay.isPressed) {
                        listener.onLowPassFilterRequested(LowPassFilter.SharpDelay)
                    }
                }
                R.id.slow_delay -> {
                    if (binding.slowDelay.isPressed) {
                        listener.onLowPassFilterRequested(LowPassFilter.SlowDelay)
                    }
                }
                R.id.super_slow -> {
                    if (binding.superSlow.isPressed) {
                        listener.onLowPassFilterRequested(LowPassFilter.SuperSlow)
                    }
                }
                R.id.short_dispersion -> {
                    if (binding.shortDispersion.isPressed) {
                        listener.onLowPassFilterRequested(LowPassFilter.ShortDispersion)
                    }
                }
            }
        }
    }

    fun bindItemLowPassFilter(lowPassFilter: LowPassFilter) {
        when (lowPassFilter) {
            LowPassFilter.Sharp -> binding.sharp.isChecked = true
            LowPassFilter.SharpDelay -> binding.sharpDelay.isChecked = true
            LowPassFilter.ShortDispersion -> binding.shortDispersion.isChecked = true
            LowPassFilter.Slow -> binding.slow.isChecked = true
            LowPassFilter.SlowDelay -> binding.slowDelay.isChecked = true
            LowPassFilter.SuperSlow -> binding.superSlow.isChecked = true
        }
    }
}
