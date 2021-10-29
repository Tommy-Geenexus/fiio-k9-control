/*
 * Copyright (c) 2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.android.material.elevation.SurfaceColors
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.FragmentEqBinding
import com.tomg.fiiok9control.eq.business.EqViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EqFragment : BaseFragment<FragmentEqBinding>(R.layout.fragment_eq) {

    @Suppress("unused")
    val eqViewModel: EqViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.navigationBarColor =
            SurfaceColors.SURFACE_2.getColor(requireActivity())
        setupToolbar(
            toolbar = binding.toolbar,
            titleRes = R.string.app_name
        )
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigate(EqFragmentDirections.eqToSetup())
        }
    }

    override fun onReconnectToDevice() {
    }

    override fun bindLayout(view: View) = FragmentEqBinding.bind(view)
}
