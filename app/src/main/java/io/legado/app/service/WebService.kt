package io.legado.app.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.PreferKey
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.*
import io.legado.app.web.HttpServer
import io.legado.app.web.WebSocketServer
import java.io.IOException

class WebService : BaseService() {

    companion object {
        var isRun = false
        var hostAddress = ""

        fun start(context: Context) {
            context.startService<WebService>()
        }

        fun stop(context: Context) {
            context.stopService<WebService>()
        }

    }

    private var httpServer: HttpServer? = null
    private var webSocketServer: WebSocketServer? = null
    private var notificationContent = ""

    override fun onCreate() {
        super.onCreate()
        isRun = true
        notificationContent = getString(R.string.service_starting)
        upNotification()
        upTile(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        postEvent(EventBus.WEB_SERVICE, "")
        upTile(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.stop -> stopSelf()
            else -> upWebServer()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun upWebServer() {
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        val address = NetworkUtils.getLocalIPAddress()
        if (address != null) {
            val port = getPort()
            httpServer = HttpServer(port)
            webSocketServer = WebSocketServer(port + 1)
            try {
                httpServer?.start()
                webSocketServer?.start(1000 * 30) // 通信超时设置
                hostAddress = getString(R.string.http_ip, address.hostAddress, port)
                isRun = true
                postEvent(EventBus.WEB_SERVICE, hostAddress)
                notificationContent = hostAddress
                upNotification()
            } catch (e: IOException) {
                toastOnUi(e.localizedMessage ?: "")
                e.printOnDebug()
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    private fun getPort(): Int {
        var port = getPrefInt(PreferKey.webPort, 1122)
        if (port > 65530 || port < 1024) {
            port = 1122
        }
        return port
    }

    /**
     * 更新通知
     */
    private fun upNotification() {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdWeb)
            .setSmallIcon(R.drawable.ic_web_service_noti)
            .setOngoing(true)
            .setContentTitle(getString(R.string.web_service))
            .setContentText(notificationContent)
            .setContentIntent(
                activityPendingIntent<MainActivity>("webService")
            )
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            servicePendingIntent<WebService>(IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(AppConst.notificationIdWeb, notification)
    }

    private fun upTile(active: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startService<WebTileService> {
                action = if (active) {
                    IntentAction.start
                } else {
                    IntentAction.stop
                }
            }
        }
    }
}
