package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.entities.HttpTTS
import io.legado.app.databinding.DialogHttpTtsEditBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding

class SpeakEngineEditDialog() : BaseDialogFragment(R.layout.dialog_http_tts_edit),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogHttpTtsEditBinding::bind)
    private val viewModel by viewModels<SpeakEngineEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        viewModel.initData(arguments) {
            initView(httpTTS = it)
        }
        initMenu()
    }

    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(httpTTS: HttpTTS) {
        binding.tvName.setText(httpTTS.name)
        binding.tvUrl.setText(httpTTS.url)
        binding.tvLoginUrl.setText(httpTTS.loginUrl)
        binding.tvLoginUi.setText(httpTTS.getLoginUiStr())
        binding.tvLoginCheckJs.setText(httpTTS.loginCheckJs)
        binding.tvHeaders.setText(httpTTS.header)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(dataFromView())
            R.id.menu_login -> dataFromView().let { httpTts ->
                if (httpTts.loginUrl.isNullOrBlank()) {
                    toastOnUi("登录url不能为空")
                } else {
                    viewModel.save(httpTts) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "httpTts")
                            putExtra("key", httpTts.id.toString())
                        }
                    }
                }
            }
            R.id.menu_help -> help()
        }
        return true
    }

    private fun dataFromView(): HttpTTS {
        return HttpTTS(
            id = viewModel.id ?: System.currentTimeMillis(),
            name = binding.tvName.text.toString(),
            url = binding.tvUrl.text.toString(),
            loginUrl = binding.tvLoginUrl.text?.toString(),
            loginCheckJs = binding.tvLoginCheckJs.text?.toString(),
            header = binding.tvHeaders.text?.toString()
        ).apply {
            setLoginUi(binding.tvLoginUi.text?.toString())
        }
    }

    private fun help() {
        val helpStr = String(
            requireContext().assets.open("help/httpTTSHelp.md").readBytes()
        )
        showDialogFragment(TextDialog(helpStr, TextDialog.Mode.MD))
    }

}