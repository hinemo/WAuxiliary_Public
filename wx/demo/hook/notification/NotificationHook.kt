package wx.demo.hook.notification

// import android.app.Notification // 原始Notification对象不再直接在主要逻辑中使用
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
// import android.media.RingtoneManager // RingtoneManager 可能仍用于某些高级声音处理，但基础setSound用Uri
import android.net.Uri
import android.os.Build
// import android.util.Log // 被 PluginOtherMethod.log 替代
import androidx.core.app.NotificationCompat // 使用兼容库以获得更好的特性和向后兼容性
import wx.demo.data.config.SecretFriendsSettings
import me.hd.wauxv.tool.PluginOtherMethod // 新增导入
import me.hd.wauxv.bean.MsgInfo // 新增导入
import me.hd.wauxv.tool.PluginMsgMethod // 新增导入

// 假设的 Hook 框架类 - 根据实际框架替换 (这些不再是此文件的主要关注点)
// ... (原有XposedHelpers等占位符注释可以保留或移除，因为不再直接使用)


object NotificationHook {

    private const val TAG = "NotificationHook" // 日志标签

    /**
     * 构建并显示自定义通知的辅助函数。
     * 此函数现在基于 MsgInfo 的内容创建通知，而不是一个原始的Notification对象。
     * @param context 上下文
     * @param talker 发送者/聊天ID (例如，用于通知标题)
     * @param content 消息内容 (用于通知文本)
     * @param originalMsgId 原始消息的ID (用于生成通知ID)
     */
    private fun buildAndDisplayCustomNotification(
        context: Context,
        talker: String,
        content: String,
        originalMsgId: Long
    ) {
        PluginOtherMethod.log("$TAG: INFO: 为密友 '$talker' 构建自定义通知 (基于消息内容)。")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 通知基本信息
        // TODO: 实际应用中，可能需要根据 talker 从联系人数据中查找显示名称
        val contentTitle = "来自密友: $talker" // 示例标题
        val contentText = content // 直接使用消息内容

        // TODO: 设置一个合适的应用图标或插件定义的默认图标
        // 由于我们不再有原始Notification对象，需要一个预定义的图标。
        val smallIcon = android.R.drawable.sym_def_app_icon // 这是一个系统占位符图标

        // TODO: 创建一个合适的 PendingIntent
        // 理想情况下，点击通知应打开对应的聊天界面。这需要知道如何构造这样的Intent。
        // 作为占位符，这里创建一个打开应用主界面的Intent。
        val launchIntent: Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent: PendingIntent? = launchIntent?.let {
            PendingIntent.getActivity(context, originalMsgId.toInt(), it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        // 2. 创建新的通知构建器
        val channelId = "wx_secret_friend_channel" // 与之前一致的渠道ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                channel = android.app.NotificationChannel(
                    channelId,
                    "密友通知",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
                PluginOtherMethod.log("$TAG: INFO: 已创建密友通知渠道: $channelId (在 buildAndDisplayCustomNotification 中检查)")
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(smallIcon) // 使用占位符或预定义图标
            .setContentIntent(pendingIntent) // 使用占位符或特定功能的PendingIntent
            .setAutoCancel(true)

        // 3. 应用自定义声音
        val soundUriString = SecretFriendsSettings.customNotificationSoundUri
        if (soundUriString.isNotEmpty()) {
            try {
                val soundUri = Uri.parse(soundUriString)
                builder.setSound(soundUri)
                PluginOtherMethod.log("$TAG: INFO: 自定义通知声音设置为: $soundUriString")
            } catch (e: Exception) {
                PluginOtherMethod.log("$TAG: ERROR: 解析或设置自定义通知声音URI失败: $soundUriString: ${e.message}")
            }
        } else {
            PluginOtherMethod.log("$TAG: INFO: 自定义通知声音URI为空，使用默认声音 (或渠道设置)。")
        }

        // 4. 应用自定义样式 (LED灯、振动模式等)
        val styleIdentifier = SecretFriendsSettings.customNotificationStyle
        PluginOtherMethod.log("$TAG: INFO: 尝试应用自定义通知样式: $styleIdentifier")
        when (styleIdentifier) {
            "led_red" -> {
                PluginOtherMethod.log("$TAG: INFO: 样式 'led_red': (概念) 尝试设置红色LED。实际实现需适配渠道。")
            }
            "vibrate_short" -> {
                PluginOtherMethod.log("$TAG: INFO: 样式 'vibrate_short': (概念) 尝试设置短振动。实际实现需适配渠道。")
            }
            "default" -> {
                PluginOtherMethod.log("$TAG: INFO: 样式 'default': 使用默认振动和灯光。")
            }
            else -> {
                PluginOtherMethod.log("$TAG: WARN: 未知的自定义通知样式: $styleIdentifier")
            }
        }

        // 5. 发出自定义通知
        // 使用原始消息ID (转换为Int) 作为通知ID，确保一致性。
        // 注意: 通知ID是Int类型，而消息ID通常是Long。直接转换可能导致ID冲突，
        // 更好的做法是维护一个映射或使用消息ID的低32位。此处为简化直接转换。
        val notificationId = originalMsgId.toInt()
        try {
            notificationManager.notify(notificationId, builder.build())
            PluginOtherMethod.log("$TAG: INFO: 已为用户 '$talker' 发出自定义通知 (基于MsgInfo, ID: $notificationId)。")
        } catch (e: Exception) {
            PluginOtherMethod.log("$TAG: ERROR: 发出自定义通知失败 for user '$talker': ${e.message}")
        }
    }

    /**
     * 当插件通过 `PluginCallback.onHandleMsg` 收到消息时，此方法被调用。
     * 这是处理密友自定义通知的主要入口点。
     *
     * @param msgInfo 包含消息详细信息的对象。
     * @param context Android 上下文。
     */
    fun onPluginMsgReceived(msgInfo: MsgInfo, context: Context?) {
        if (context == null) {
            PluginOtherMethod.log("$TAG: WARN: onPluginMsgReceived 上下文为空，无法处理消息。")
            return
        }
        // 确保设置已初始化 (如果之前未初始化)
        // 在多Hook场景下，init可能已在别处调用，但此处调用可确保独立性。
        SecretFriendsSettings.init(context.applicationContext)

        val talker = msgInfo.talker // 发送者或群聊ID
        val content = msgInfo.content // 消息内容
        val msgId = msgInfo.msgId // 消息ID

        PluginOtherMethod.log("$TAG: INFO: onPluginMsgReceived: 收到消息 from '$talker', msgId: $msgId, content: \"$content\"")

        if (SecretFriendsSettings.IsSecretFeatureEnabled &&
            SecretFriendsSettings.isCustomNotificationsEnabled &&
            SecretFriendsSettings.HiddenFriendIds.contains(talker)) {
            
            PluginOtherMethod.log("$TAG: INFO: 检测到密友 '$talker' 的消息 (msgId: $msgId)，准备处理自定义通知。")

            // **重要: 消息撤回影响**
            //   - 调用 `PluginMsgMethod.revokeMsg(msgId)` 会尝试撤回原始消息。
            //   - 这意味着该消息将不会出现在微信的聊天列表和聊天界面中（除非“密友模式”激活时有特殊逻辑恢复它）。
            //   - 这也有效地阻止了微信自身为这条消息生成标准通知。
            //   - 开发者需要确保这种行为是期望的，并且有机制在“密友模式”下正确显示这些“被撤回”的消息。
            val revokeSuccess = PluginMsgMethod.revokeMsg(msgId) // 假设此方法返回Boolean指示成功与否
            if (revokeSuccess) {
                PluginOtherMethod.log("$TAG: INFO: 成功尝试撤回原始消息 (msgId: $msgId) 以阻止其通知。")
            } else {
                PluginOtherMethod.log("$TAG: WARN: 尝试撤回原始消息 (msgId: $msgId) 可能失败或不受支持。仍继续尝试发送自定义通知。")
            }
            
            // 构建并发送自定义通知
            buildAndDisplayCustomNotification(context, talker, content, msgId)

        } else {
            PluginOtherMethod.log("$TAG: INFO: 非密友消息或自定义通知功能未启用 (用户: '$talker')，不进行特殊处理。")
        }
    }


    /**
     * [已过时/待审阅] 初始化和应用 Hook 的占位符 (原用于Xposed等通用Hook)。
     * 若模块完全依赖 `PluginCallback.onHandleMsg` (通过 `onPluginMsgReceived` 实现)，
     * 则此方法可能仅用于一次性初始化，或不再需要Hook通知相关的特定微信方法。
     *
     * @param classLoader 用于查找类的类加载器。
     * @param context Android 上下文，如果可用且初始化需要。
     */
    @Deprecated("主要逻辑已迁移到 onPluginMsgReceived，此方法可能仅用于初始化或被废弃。")
    fun applyHooks(classLoader: ClassLoader, context: Context?) {
        // **微信版本更新兼容性:**
        //   - 微信的通知机制和相关类名、方法名可能会随版本更新而改变。
        //   - 每次微信更新后，Hook点可能需要重新定位和适配，否则功能可能失效或导致应用崩溃。
        //   - 建议使用更健壮的特征码搜索或更抽象的Hook点（如果可能），但这通常更复杂。
        //   - 使用 PluginCallback.onHandleMsg 后，对微信内部通知方法的直接Hook依赖降低。

        PluginOtherMethod.log("$TAG: WARN: applyHooks (基于通用Hook框架的方法) 被调用。主要通知逻辑现应通过 onPluginMsgReceived (由 PluginCallback.onHandleMsg 触发) 处理。")
        if (context != null) {
            // 确保设置在此处初始化，以防 onPluginMsgReceived 中的 context 为 null 或延迟调用。
            SecretFriendsSettings.init(context.applicationContext) 
            PluginOtherMethod.log("$TAG: INFO: NotificationHook 设置已在 applyHooks 中初始化。")
        } else {
            PluginOtherMethod.log("$TAG: WARN: NotificationHook (applyHooks) 未能获取到 context，设置可能未正确初始化。")
        }
        
        PluginOtherMethod.log("$TAG: INFO: (applyHooks) 微信通知的自定义处理逻辑已移至 onPluginMsgReceived。此 applyHooks 方法不再注册特定的微信通知方法Hook。")
    }
}
