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

package com.tomg.fiiok9control.eq.ui

import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.ItemEqBinding
import com.tomg.fiiok9control.eq.EqPreSet
import com.tomg.fiiok9control.eq.EqValue

class ItemEqViewHolder(
    private val binding: ItemEqBinding,
    private val listener: EqAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.eqEnabled.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onEqEnabled(isChecked)
            }
        }
        binding.group.setOnCheckedChangeListener { _, checkedId ->
            binding.eqCustom.isVisible = checkedId == R.id.custom
            when (checkedId) {
                R.id.jazz -> {
                    if (binding.jazz.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Jazz)
                    }
                }
                R.id.pop -> {
                    if (binding.pop.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Pop)
                    }
                }
                R.id.rock -> {
                    if (binding.rock.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Rock)
                    }
                }
                R.id.dance -> {
                    if (binding.dance.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Dance)
                    }
                }
                R.id.custom -> {
                    if (binding.custom.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Custom)
                    }
                }
                R.id.rb -> {
                    if (binding.rb.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Rb)
                    }
                }
                R.id.classical -> {
                    if (binding.classical.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Classical)
                    }
                }
            }
        }
        binding.band31Slider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 1, value = value)
                )
            }
        }
        binding.band62Slider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 2, value = value)
                )
            }
        }
        binding.band125Slider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 3, value = value)
                )
            }
        }
        binding.band250Slider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 4, value = value)
                )
            }
        }
        binding.band500Slider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 5, value = value)
                )
            }
        }
        binding.band1kSlider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 6, value = value)
                )
            }
        }
        binding.band2kSlider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 7, value = value)
                )
            }
        }
        binding.band4kSlider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 8, value = value)
                )
            }
        }
        binding.band8kSlider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(id = 9, value = value)
                )
            }
        }
        binding.band16kSlider.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onEqValueChanged(
                    EqValue(
                        id = 10,
                        value = value
                    )
                )
            }
        }
    }

    fun bindItemEq(
        eqEnabled: Boolean,
        eqPreSet: EqPreSet,
        eqValues: List<EqValue>,
        itemEnabled: Boolean
    ) {
        binding.apply {
            this.eqEnabled.isChecked = eqEnabled
            this.eqEnabled.isEnabled = itemEnabled
            this.eqPreSet.isEnabled = eqEnabled && itemEnabled
            group.children.forEach { child ->
                child.isEnabled = eqEnabled && itemEnabled
            }
            eqCustom.children.forEach { child ->
                child.isEnabled = eqEnabled && itemEnabled
            }
            when (eqPreSet) {
                EqPreSet.Classical -> classical.isChecked = true
                EqPreSet.Dance -> dance.isChecked = true
                is EqPreSet.Custom -> custom.isChecked = true
                EqPreSet.Jazz -> jazz.isChecked = true
                EqPreSet.Pop -> pop.isChecked = true
                EqPreSet.Rb -> rb.isChecked = true
                EqPreSet.Rock -> rock.isChecked = true
            }
            val v31 = eqValues.firstOrNull()?.value ?: 0f
            band31.text = itemView.context.getString(R.string._31hz, v31)
            band31Slider.slider.value = v31
            val v62 = eqValues.getOrNull(1)?.value ?: 0f
            band62.text = itemView.context.getString(R.string._62hz, v62)
            band62Slider.slider.value = v62
            val v125 = eqValues.getOrNull(2)?.value ?: 0f
            band125.text = itemView.context.getString(R.string._125hz, v125)
            band125Slider.slider.value = eqValues.getOrNull(2)?.value ?: 0f
            val v250 = eqValues.getOrNull(3)?.value ?: 0f
            band250.text = itemView.context.getString(R.string._250hz, v250)
            band250Slider.slider.value = v250
            val v500 = eqValues.getOrNull(4)?.value ?: 0f
            band500.text = itemView.context.getString(R.string._500hz, v500)
            band500Slider.slider.value = v500
            val v1k = eqValues.getOrNull(5)?.value ?: 0f
            band1k.text = itemView.context.getString(R.string._1khz, v1k)
            band1kSlider.slider.value = v1k
            val v2k = eqValues.getOrNull(6)?.value ?: 0f
            band2k.text = itemView.context.getString(R.string._2khz, v2k)
            band2kSlider.slider.value = eqValues.getOrNull(6)?.value ?: 0f
            val v4k = eqValues.getOrNull(7)?.value ?: 0f
            band4k.text = itemView.context.getString(R.string._4khz, v4k)
            band4kSlider.slider.value = eqValues.getOrNull(7)?.value ?: 0f
            val v8k = eqValues.getOrNull(8)?.value ?: 0f
            band8k.text = itemView.context.getString(R.string._8khz, v8k)
            band8kSlider.slider.value = eqValues.getOrNull(8)?.value ?: 0f
            val v16k = eqValues.getOrNull(9)?.value ?: 0f
            band16k.text = itemView.context.getString(R.string._16khz, v16k)
            band16kSlider.slider.value = v16k
        }
    }
}
