package com.fk.sample.core.notification

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.notification.LocalNotification
import com.fk.core.notification.LocalNotificationChannel
import com.fk.core.notification.LocalNotificationContent
import com.fk.core.notification.LocalNotificationError
import com.fk.core.notification.LocalNotificationImportance
import com.fk.core.notification.LocalNotificationRequest
import com.fk.core.notification.LocalNotificationTrigger
import com.fk.core.permissions.PermissionKind
import com.fk.core.permissions.PermissionRequest
import com.fk.core.permissions.PermissionStatus
import com.fk.core.permissions.Permissions
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.launch

/**
 * Smoke demo for Phase B6 local notification (channel + schedule / cancel).
 */
@Composable
fun NotificationDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val activity = context as ComponentActivity
  val scope = rememberCoroutineScope()

  val notifications = remember {
    LocalNotification.create(context.applicationContext).also { manager ->
      manager.ensureChannel(
        LocalNotificationChannel(
          id = CHANNEL_ID,
          name = "Sample reminders",
          importance = LocalNotificationImportance.Default,
          description = "fk-android sample local notifications",
        ),
      )
    }
  }
  val permissions = remember { Permissions.create(context.applicationContext) }

  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }

  Scaffold(
    topBar = { SampleTopBar(title = "Notification", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("LocalNotification package v${LocalNotification.VERSION}")
      Text("Channels · schedule / cancel · AlarmManager delayed delivery")
      HorizontalDivider()
      Text("Status: $status")
      Text("Detail:\n$detail")

      Button(
        onClick = {
          val can = notifications.canScheduleNotifications()
          val perm = permissions.status(PermissionKind.Notifications, activity)
          status = if (can) "Authorized" else "Not authorized"
          detail = "canSchedule=$can\npermission=$perm"
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Check authorization")
      }

      Button(
        onClick = {
          scope.launch {
            val result = permissions.request(
              activity,
              PermissionRequest(kind = PermissionKind.Notifications),
            )
            status = "Permission ${result.status}"
            detail = "canSchedule=${notifications.canScheduleNotifications()}"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Request notification permission")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              notifications.schedule(
                LocalNotificationRequest(
                  identifier = IMMEDIATE_ID,
                  channelId = CHANNEL_ID,
                  content = LocalNotificationContent(
                    title = "Immediate",
                    body = "Posted right away from sample",
                    userInfo = mapOf("source" to "sample"),
                  ),
                  trigger = LocalNotificationTrigger.Immediate,
                ),
              )
              status = "Immediate scheduled"
              detail = "identifier=$IMMEDIATE_ID"
            } catch (e: LocalNotificationError) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Schedule immediate")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              notifications.schedule(
                LocalNotificationRequest(
                  identifier = DELAYED_ID,
                  channelId = CHANNEL_ID,
                  content = LocalNotificationContent(
                    title = "Delayed",
                    body = "Fires about 5 seconds after schedule",
                  ),
                  trigger = LocalNotificationTrigger.TimeInterval(delayMs = 5_000L),
                ),
              )
              val pending = notifications.pendingRequests()
              status = "Delayed scheduled (+5s)"
              detail = pending.joinToString("\n") { p ->
                "${p.identifier} next=${p.nextFireEpochMs} (${p.triggerDescription})"
              }.ifBlank { "No pending" }
            } catch (e: LocalNotificationError) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Schedule delayed (+5s)")
      }

      Button(
        onClick = {
          scope.launch {
            notifications.cancelPending(DELAYED_ID)
            notifications.cancelPending(IMMEDIATE_ID)
            status = "Cancelled pending"
            detail = notifications.pendingRequests().joinToString("\n") {
              it.identifier
            }.ifBlank { "No pending" }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Cancel pending")
      }

      Button(
        onClick = {
          scope.launch {
            notifications.removeAllDelivered()
            status = "Cleared delivered"
            detail = "Notification shade entries removed for this app"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Remove all delivered")
      }

      Button(
        onClick = {
          scope.launch {
            val pending = notifications.pendingRequests()
            status = "Pending (${pending.size})"
            detail = if (pending.isEmpty()) {
              "No pending requests"
            } else {
              pending.joinToString("\n") { p ->
                "${p.identifier}\n  ${p.triggerDescription}\n  next=${p.nextFireEpochMs}"
              }
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("List pending")
      }

      if (permissions.status(PermissionKind.Notifications, activity) ==
        PermissionStatus.PermanentlyDenied
      ) {
        Button(
          onClick = { permissions.openAppSettings() },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Open app settings")
        }
      }
    }
  }
}

private const val CHANNEL_ID: String = "fk.sample.reminders"
private const val IMMEDIATE_ID: String = "com.fk.sample.notification.immediate"
private const val DELAYED_ID: String = "com.fk.sample.notification.delayed"
