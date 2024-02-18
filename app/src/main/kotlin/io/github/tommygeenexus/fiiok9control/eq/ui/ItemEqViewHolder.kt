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

package io.github.tommygeenexus.fiiok9control.eq.ui

import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqPreSet
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqValue
import io.github.tommygeenexus.fiiok9control.databinding.ItemEqBinding

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
        binding.band31Slider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 1, value = slider.value))
                }
            }
        )
        binding.band31Slider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band62Slider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 2, value = slider.value))
                }
            }
        )
        binding.band62Slider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band125Slider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 3, value = slider.value))
                }
            }
        )
        binding.band125Slider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band250Slider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 4, value = slider.value))
                }
            }
        )
        binding.band250Slider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band500Slider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 5, value = slider.value))
                }
            }
        )
        binding.band500Slider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band1kSlider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 6, value = slider.value))
                }
            }
        )
        binding.band1kSlider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band2kSlider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 7, value = slider.value))
                }
            }
        )
        binding.band2kSlider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band4kSlider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 8, value = slider.value))
                }
            }
        )
        binding.band4kSlider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band8kSlider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 9, value = slider.value))
                }
            }
        )
        binding.band8kSlider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
        binding.band16kSlider.slider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onEqValueChanged(EqValue(id = 10, value = slider.value))
                }
            }
        )
        binding.band16kSlider.slider.setLabelFormatter { value ->
            itemView.context.getString(R.string.eq_offset, value)
        }
    }

    fun bindItemEq(
        isEqEnabled: Boolean,
        eqPreSet: EqPreSet,
        eqValues: List<EqValue>,
        isItemEnabled: Boolean
    ) {
        binding.apply {
            this.eqEnabled.isChecked = isEqEnabled
            this.eqEnabled.isEnabled = isItemEnabled
            this.eqPreSet.isEnabled = isEqEnabled && isItemEnabled
            group.children.forEach { child ->
                child.isEnabled = isEqEnabled && isItemEnabled
            }
            eqCustom.children.forEach { child ->
                child.isEnabled = isEqEnabled && isItemEnabled
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
            band31.text = itemView.context.getString(R.string.eq_offset_31hz, v31)
            band31Slider.slider.value = v31
            val v62 = eqValues.getOrNull(1)?.value ?: 0f
            band62.text = itemView.context.getString(R.string.eq_offset_62hz, v62)
            band62Slider.slider.value = v62
            val v125 = eqValues.getOrNull(2)?.value ?: 0f
            band125.text = itemView.context.getString(R.string.eq_offset_125hz, v125)
            band125Slider.slider.value = eqValues.getOrNull(2)?.value ?: 0f
            val v250 = eqValues.getOrNull(3)?.value ?: 0f
            band250.text = itemView.context.getString(R.string.eq_offset_250hz, v250)
            band250Slider.slider.value = v250
            val v500 = eqValues.getOrNull(4)?.value ?: 0f
            band500.text = itemView.context.getString(R.string.eq_offset_500hz, v500)
            band500Slider.slider.value = v500
            val v1k = eqValues.getOrNull(5)?.value ?: 0f
            band1k.text = itemView.context.getString(R.string.eq_offset_1khz, v1k)
            band1kSlider.slider.value = v1k
            val v2k = eqValues.getOrNull(6)?.value ?: 0f
            band2k.text = itemView.context.getString(R.string.eq_offset_2khz, v2k)
            band2kSlider.slider.value = eqValues.getOrNull(6)?.value ?: 0f
            val v4k = eqValues.getOrNull(7)?.value ?: 0f
            band4k.text = itemView.context.getString(R.string.eq_offset_4khz, v4k)
            band4kSlider.slider.value = eqValues.getOrNull(7)?.value ?: 0f
            val v8k = eqValues.getOrNull(8)?.value ?: 0f
            band8k.text = itemView.context.getString(R.string.eq_offset_8khz, v8k)
            band8kSlider.slider.value = eqValues.getOrNull(8)?.value ?: 0f
            val v16k = eqValues.lastOrNull()?.value ?: 0f
            band16k.text = itemView.context.getString(R.string.eq_offset_16khz, v16k)
            band16kSlider.slider.value = v16k
        }
    }
}
