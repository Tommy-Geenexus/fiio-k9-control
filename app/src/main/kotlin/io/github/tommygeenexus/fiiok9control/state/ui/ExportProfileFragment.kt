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

package io.github.tommygeenexus.fiiok9control.state.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowInsets
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.tommygeenexus.fiiok9control.core.util.KEY_PROFILE_NAME
import io.github.tommygeenexus.fiiok9control.core.util.KEY_PROFILE_VOLUME_EXPORT
import io.github.tommygeenexus.fiiok9control.databinding.FragmentExportProfileBinding

class ExportProfileFragment : DialogFragment() {

    private var _binding: FragmentExportProfileBinding? = null
    internal val binding: FragmentExportProfileBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentExportProfileBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    KEY_PROFILE_NAME,
                    bundleOf(
                        KEY_PROFILE_NAME to binding.profileName.text?.trim().toString(),
                        KEY_PROFILE_VOLUME_EXPORT to binding.exportVolume.isChecked
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener {
                    binding.profileName.requestFocus()
                    window?.insetsController?.show(WindowInsets.Type.ime())
                }
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        _binding = null
    }
}
