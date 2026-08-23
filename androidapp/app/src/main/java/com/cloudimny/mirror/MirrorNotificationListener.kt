package com.cloudimny.mirror

import android.service.notification.NotificationListenerService

/**
 * Deliberately empty: no notification is ever read here. The class exists because
 * `MediaSessionManager.getActiveSessions` will only answer a caller that names an *enabled*
 * notification listener of its own package, so an app that wants to see what another player is
 * doing has to own one — the access is granted to the component, not to the permission.
 */
class MirrorNotificationListener : NotificationListenerService()
