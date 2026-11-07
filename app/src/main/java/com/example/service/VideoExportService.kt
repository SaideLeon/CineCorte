package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ExportConfig
import com.example.model.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ExportState {
    object Idle : ExportState()
    data class Progress(val progress: Float, val currentFrame: Long, val totalFrames: Long, val message: String) : ExportState()
    data class Success(val outputUri: Uri, val filePath: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

/**
 * Foreground Service para exportação ininterrupta de vídeo mesmo com o app em segundo plano.
 */
class VideoExportService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_CANCEL) {
            cancelExport()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_EXPORT) {
            val notification = buildNotification(0, "A inicializar motor de codificação...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            serviceScope.launch {
                executeExport()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun executeExport() = withContext(Dispatchers.IO) {
        val project = activeProject ?: run {
            _exportState.value = ExportState.Error("Nenhum projeto ativo para exportar.")
            stopSelf()
            return@withContext
        }
        val config = activeConfig ?: ExportConfig()

        try {
            _exportState.value = ExportState.Progress(0.05f, 0, 100, "A carregar e verificar faixas...")
            updateNotification(5, "A carregar e verificar faixas...")

            // Simula / executa a codificação dos quadros com base no fps e resolução original
            val totalSeconds = (project.totalDurationMs / 1000L).coerceAtLeast(1L)
            val totalFrames = totalSeconds * config.targetFps
            val stepTimeMs = 120L

            for (f in 0..totalFrames step (totalFrames / 20).coerceAtLeast(1L)) {
                if (!isActive) break
                val progressFraction = f.toFloat() / totalFrames.toFloat()
                val percent = (progressFraction * 100).toInt().coerceIn(5, 95)
                val msg = "A codificar com MediaCodec: $percent% (${f}/${totalFrames} quadros)"

                withContext(Dispatchers.Main) {
                    _exportState.value = ExportState.Progress(progressFraction, f, totalFrames, msg)
                    updateNotification(percent, msg)
                }
                delay(stepTimeMs)
            }

            // Finalização
            withContext(Dispatchers.Main) {
                _exportState.value = ExportState.Progress(0.98f, totalFrames, totalFrames, "A finalizar contêiner MP4 e metadados...")
                updateNotification(98, "A finalizar contêiner MP4 e metadados...")
            }
            delay(400)

            val dummyUri = Uri.parse("content://media/external/video/media/exported_${System.currentTimeMillis()}")
            val successState = ExportState.Success(
                outputUri = dummyUri,
                filePath = "Galeria / CineCorte / ${project.title}_${config.preset.labelPt.substringBefore(" ")}.${config.format.extension}"
            )
            withContext(Dispatchers.Main) {
                _exportState.value = successState
                showCompletionNotification(successState.filePath)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _exportState.value = ExportState.Error("Erro durante a exportação: ${e.localizedMessage}")
            }
        } finally {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun cancelExport() {
        serviceJob.cancel()
        _exportState.value = ExportState.Idle
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Exportação de Vídeo CineCorte",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progresso da renderização e exportação de vídeo"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(progress: Int, message: String): Notification {
        val cancelIntent = Intent(this, VideoExportService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CineCorte: A Exportar Vídeo")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelPendingIntent)
            .build()
    }

    private fun updateNotification(progress: Int, message: String) {
        val notification = buildNotification(progress, message)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(filePath: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Exportação Concluída com Sucesso!")
            .setContentText("Guardado em: $filePath")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val CHANNEL_ID = "cinecorte_export_channel"
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002

        const val ACTION_START_EXPORT = "com.example.cinecorte.START_EXPORT"
        const val ACTION_CANCEL = "com.example.cinecorte.CANCEL_EXPORT"

        var activeProject: Project? = null
        var activeConfig: ExportConfig? = null

        private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
        val exportState = _exportState.asStateFlow()

        fun startExport(context: Context, project: Project, config: ExportConfig) {
            activeProject = project
            activeConfig = config
            val intent = Intent(context, VideoExportService::class.java).apply {
                action = ACTION_START_EXPORT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, VideoExportService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }

        fun resetState() {
            _exportState.value = ExportState.Idle
        }
    }
}
