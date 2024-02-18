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

package io.github.tommygeenexus.fiiok9control.audio.ui

import androidx.recyclerview.widget.RecyclerView
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.BluetoothCodec
import io.github.tommygeenexus.fiiok9control.databinding.ItemCodecsEnabledBinding

class ItemCodecsEnabledViewHolder(
    private val binding: ItemCodecsEnabledBinding,
    private val listener: AudioAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.aptXAdaptive.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onBluetoothCodecChanged(BluetoothCodec.AptX.Adaptive, isChecked)
            }
        }
        binding.aac.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onBluetoothCodecChanged(BluetoothCodec.Aac, isChecked)
            }
        }
        binding.ldac.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onBluetoothCodecChanged(BluetoothCodec.Ldac, isChecked)
            }
        }
        binding.aptX.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onBluetoothCodecChanged(BluetoothCodec.AptX.Default, isChecked)
            }
        }
        binding.aptXLl.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onBluetoothCodecChanged(BluetoothCodec.AptX.Ll, isChecked)
            }
        }
        binding.aptXHd.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onBluetoothCodecChanged(BluetoothCodec.AptX.Hd, isChecked)
            }
        }
    }

    fun bindItemCodecsEnabled(codecsEnabled: List<BluetoothCodec>, itemEnabled: Boolean) {
        binding.aptXAdaptive.isChecked = codecsEnabled.contains(BluetoothCodec.AptX.Adaptive)
        binding.aptXAdaptive.isEnabled = itemEnabled
        binding.aac.isChecked = codecsEnabled.contains(BluetoothCodec.Aac)
        binding.aac.isEnabled = itemEnabled
        binding.ldac.isChecked = codecsEnabled.contains(BluetoothCodec.Ldac)
        binding.ldac.isEnabled = itemEnabled
        binding.aptX.isChecked = codecsEnabled.contains(BluetoothCodec.AptX.Default)
        binding.aptX.isEnabled = itemEnabled
        binding.aptXLl.isChecked = codecsEnabled.contains(BluetoothCodec.AptX.Ll)
        binding.aptXLl.isEnabled = itemEnabled
        binding.aptXHd.isChecked = codecsEnabled.contains(BluetoothCodec.AptX.Hd)
        binding.aptXHd.isEnabled = itemEnabled
    }
}
