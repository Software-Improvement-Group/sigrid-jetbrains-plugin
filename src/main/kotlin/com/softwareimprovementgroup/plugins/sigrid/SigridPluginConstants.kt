package com.softwareimprovementgroup.plugins.sigrid

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

const val NOTIFICATION_GROUP_ID = "Sigrid"

/** Shared notification builder so every Sigrid notification uses the same group and action pattern. */
fun notifySigrid(project: Project, message: String, type: NotificationType, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP_ID)
        .createNotification(message, type)
    if (actionLabel != null && onAction != null) {
        notification.addAction(NotificationAction.createSimple(actionLabel) {
            onAction()
            notification.expire()
        })
    }
    notification.notify(project)
}