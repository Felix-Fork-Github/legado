package io.legado.app.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.help.AppConfig
import io.legado.app.help.SourceHelp
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.model.NoStackTraceException
import io.legado.app.utils.*

class ImportRssSourceViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<RssSource>()
    val checkSources = arrayListOf<RssSource?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val group = groupName?.trim()
            val keepName = AppConfig.importKeepName
            val selectSource = arrayListOf<RssSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    if (keepName) {
                        checkSources[index]?.let {
                            source.sourceName = it.sourceName
                            source.sourceGroup = it.sourceGroup
                            source.customOrder = it.customOrder
                        }
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            source.sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            source.sourceGroup = groups.joinToString(",")
                        } else {
                            source.sourceGroup = group
                        }
                    }
                    selectSource.add(source)
                }
            }
            SourceHelp.insertRssSource(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            val mText = text.trim()
            when {
                mText.isJsonObject() -> {
                    val json = JsonPath.parse(mText)
                    val urls = json.read<List<String>>("$.sourceUrls")
                    if (!urls.isNullOrEmpty()) {
                        urls.forEach {
                            importSourceUrl(it)
                        }
                    } else {
                        GSON.fromJsonArray<RssSource>(mText)?.let {
                            allSources.addAll(it)
                        }
                    }
                }
                mText.isJsonArray() -> {
                    val items: List<Map<String, Any>> = jsonPath.parse(mText).read("$")
                    for (item in items) {
                        val jsonItem = jsonPath.parse(item)
                        GSON.fromJsonObject<RssSource>(jsonItem.jsonString())?.let {
                            allSources.add(it)
                        }
                    }
                }
                mText.isAbsUrl() -> {
                    importSourceUrl(mText)
                }
                else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
            }
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            url(url)
        }.text("utf-8").let { body ->
            val items: List<Map<String, Any>> = jsonPath.parse(body).read("$")
            for (item in items) {
                val jsonItem = jsonPath.parse(item)
                GSON.fromJsonObject<RssSource>(jsonItem.jsonString())?.let { source ->
                    allSources.add(source)
                }
            }
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val has = appDb.rssSourceDao.getByKey(it.sourceUrl)
                checkSources.add(has)
                selectStatus.add(has == null)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}