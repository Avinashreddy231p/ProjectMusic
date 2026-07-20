package com.mardous.projectmusic.ui.screen.update

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.remote.github.model.GitHubRelease
import com.mardous.projectmusic.databinding.DialogUpdateInfoBinding
import com.mardous.projectmusic.extensions.openUrl
import com.mardous.projectmusic.extensions.resources.setMarkdownText
import com.mardous.projectmusic.extensions.showToast
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UpdateDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private var _binding: DialogUpdateInfoBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModel<UpdateViewModel>()

    private var release: GitHubRelease? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        release = viewModel.latestRelease
        if (release != null && release!!.isNewer(requireContext())) {
            _binding = DialogUpdateInfoBinding.inflate(layoutInflater)
            binding.infoAction.setOnClickListener(this)
            binding.downloadAction.setOnClickListener(this)
            binding.versionName.text = release?.name ?: release?.tag
            val body = release?.body
            if (!body.isNullOrEmpty()) {
                binding.versionInfo.setMarkdownText(body)
            } else {
                binding.versionInfo.isVisible = false
            }
            val dialog = BottomSheetDialog(requireContext()).also {
                it.setContentView(binding.root)
                it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            return dialog
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.the_app_is_up_to_date)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        release?.setIgnored()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.syncDownloadStatus(requireContext())
        viewModel.downloadStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is UpdateViewModel.DownloadStatus.Idle -> {
                    binding.downloadAction.setText(R.string.download_action)
                    binding.downloadAction.setIconResource(R.drawable.ic_download_24dp)
                    binding.downloadAction.isEnabled = true
                    binding.downloadProgress?.isVisible = false
                }
                is UpdateViewModel.DownloadStatus.Downloading -> {
                    binding.downloadAction.text = getString(R.string.downloading_progress, status.progress)
                    binding.downloadAction.icon = null
                    binding.downloadAction.isEnabled = false
                    binding.downloadProgress?.isVisible = true
                    binding.downloadProgress?.isIndeterminate = false
                    binding.downloadProgress?.progress = status.progress
                }
                is UpdateViewModel.DownloadStatus.Completed -> {
                    binding.downloadAction.text = getString(R.string.install_update_action)
                    binding.downloadAction.setIconResource(R.drawable.ic_update_24dp)
                    binding.downloadAction.isEnabled = true
                    binding.downloadProgress?.isVisible = false
                }
                is UpdateViewModel.DownloadStatus.Failed -> {
                    binding.downloadAction.setText(R.string.download_action)
                    binding.downloadAction.setIconResource(R.drawable.ic_download_24dp)
                    binding.downloadAction.isEnabled = true
                    binding.downloadProgress?.isVisible = false
                    status.error?.let { showToast(it) }
                }
            }
        }
    }

    override fun onClick(view: View) {
        when (view) {
            binding.infoAction -> {
                release?.let {
                    requireContext().openUrl(it.url)
                }
            }

            binding.downloadAction -> {
                val status = viewModel.downloadStatus.value
                if (status is UpdateViewModel.DownloadStatus.Completed) {
                    viewModel.installUpdate(requireContext())
                } else {
                    release?.let {
                        viewModel.downloadUpdate(requireContext(), it)
                        showToast(R.string.downloading_update)
                    }
                }
            }
        }
    }
}