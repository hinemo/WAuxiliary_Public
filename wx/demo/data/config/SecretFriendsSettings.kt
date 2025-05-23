package wx.demo.data.config

// import android.content.Context // SharedPreferences不再使用，移除Context导入
// import android.content.SharedPreferences // SharedPreferences不再使用，移除导入
import me.hd.wauxv.tool.PluginConfigMethod // 假设的插件配置方法类路径

object SecretFriendsSettings {

    // PREFS_NAME 可能不再直接由此类使用，因为 PluginConfigMethod 可能会有自己的存储机制。
    // 但键名仍然是核心，因此保留。
    private const val PREFS_NAME = "secret_friends_prefs" // 内部使用的SharedPreferences文件名，无需翻译

    // 配置项的键名 (保持不变)
    private const val KEY_IS_SECRET_FEATURE_ENABLED = "is_secret_feature_enabled" // 功能启用状态的键名，无需翻译
    private const val KEY_SECRET_PASSWORD = "secret_password" // 秘密密码的键名，无需翻译
    private const val KEY_HIDDEN_FRIEND_IDS = "hidden_friend_ids" // 隐藏好友ID列表的键名，无需翻译
    // 自定义通知设置的键名
    private const val KEY_IS_CUSTOM_NOTIFICATIONS_ENABLED = "is_custom_notifications_enabled" // 自定义通知启用状态的键名
    private const val KEY_CUSTOM_NOTIFICATION_SOUND_URI = "custom_notification_sound_uri" // 自定义通知声音URI的键名
    private const val KEY_CUSTOM_NOTIFICATION_STYLE = "custom_notification_style" // 自定义通知样式的键名
    // 呼叫拒绝系统设置的键名
    private const val KEY_IS_CALL_REJECTION_ENABLED = "is_call_rejection_enabled" // 呼叫拒绝功能启用状态的键名
    private const val KEY_CALL_REJECTION_MODE = "call_rejection_mode" // 呼叫拒绝模式的键名
    private const val KEY_CALL_REJECTION_DELAY_SECONDS = "call_rejection_delay_seconds" // 呼叫拒绝延迟秒数的键名
    private const val KEY_IS_AUTO_REPLY_ENABLED = "is_auto_reply_enabled" // 自动回复启用状态的键名
    private const val KEY_AUTO_REPLY_MESSAGE = "auto_reply_message" // 自动回复消息内容的键名


    // 非持久化设置 (运行时状态) - 这个保持不变，因为它不是通过SharedPreferences存储的
    var IsSecretModeActive: Boolean = false

    // SharedPreferences 初始化方法不再需要，移除
    // fun init(context: Context) {
    //     sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // }

    // IsSecretFeatureEnabled: 布尔值, 默认为 false (密友功能是否已启用)
    var IsSecretFeatureEnabled: Boolean
        get() = PluginConfigMethod.getBoolean(KEY_IS_SECRET_FEATURE_ENABLED, false)
        set(value) {
            PluginConfigMethod.putBoolean(KEY_IS_SECRET_FEATURE_ENABLED, value)
        }

    // SecretPassword: 字符串, 默认为空字符串 (用于进入密友模式的密码)
    var SecretPassword: String
        get() = PluginConfigMethod.getString(KEY_SECRET_PASSWORD, "")
        set(value) {
            PluginConfigMethod.putString(KEY_SECRET_PASSWORD, value)
        }

    // HiddenFriendIds: Set<String>, 默认为空集合 (隐藏的好友ID集合)
    var HiddenFriendIds: Set<String>
        get() = PluginConfigMethod.getStringSet(KEY_HIDDEN_FRIEND_IDS, emptySet())
        set(value) {
            PluginConfigMethod.putStringSet(KEY_HIDDEN_FRIEND_IDS, value)
        }

    // isCustomNotificationsEnabled: 布尔值, 默认为 false (是否为密友消息启用自定义通知)
    var isCustomNotificationsEnabled: Boolean
        get() = PluginConfigMethod.getBoolean(KEY_IS_CUSTOM_NOTIFICATIONS_ENABLED, false)
        set(value) {
            PluginConfigMethod.putBoolean(KEY_IS_CUSTOM_NOTIFICATIONS_ENABLED, value)
        }

    // customNotificationSoundUri: 字符串, 默认为空字符串 (自定义通知声音的URI, 空字符串表示默认或无特定声音)
    var customNotificationSoundUri: String
        get() = PluginConfigMethod.getString(KEY_CUSTOM_NOTIFICATION_SOUND_URI, "")
        set(value) {
            PluginConfigMethod.putString(KEY_CUSTOM_NOTIFICATION_SOUND_URI, value)
        }

    // customNotificationStyle: 字符串, 默认为 "default" (自定义通知样式, 例如LED颜色、振动模式的标识符)
    var customNotificationStyle: String
        get() = PluginConfigMethod.getString(KEY_CUSTOM_NOTIFICATION_STYLE, "default")
        set(value) {
            PluginConfigMethod.putString(KEY_CUSTOM_NOTIFICATION_STYLE, value)
        }

    // isCallRejectionEnabled: 布尔值, 默认为 false (是否为密友来电启用呼叫拒绝功能)
    var isCallRejectionEnabled: Boolean
        get() = PluginConfigMethod.getBoolean(KEY_IS_CALL_REJECTION_ENABLED, false)
        set(value) {
            PluginConfigMethod.putBoolean(KEY_IS_CALL_REJECTION_ENABLED, value)
        }

    // callRejectionMode: 字符串, 默认为 "silent" (呼叫拒绝模式 ('silent'表示静默拒绝, 'delayed'表示延迟拒绝))
    var callRejectionMode: String
        get() = PluginConfigMethod.getString(KEY_CALL_REJECTION_MODE, "silent")
        set(value) {
            PluginConfigMethod.putString(KEY_CALL_REJECTION_MODE, value)
        }

    // callRejectionDelaySeconds: 整数, 默认为 5 (延迟拒绝的秒数 (仅当模式为'delayed'时生效))
    var callRejectionDelaySeconds: Int
        get() = PluginConfigMethod.getInt(KEY_CALL_REJECTION_DELAY_SECONDS, 5)
        set(value) {
            PluginConfigMethod.putInt(KEY_CALL_REJECTION_DELAY_SECONDS, value)
        }

    // isAutoReplyEnabled: 布尔值, 默认为 false (是否在拒绝呼叫后启用自动回复)
    var isAutoReplyEnabled: Boolean
        get() = PluginConfigMethod.getBoolean(KEY_IS_AUTO_REPLY_ENABLED, false)
        set(value) {
            PluginConfigMethod.putBoolean(KEY_IS_AUTO_REPLY_ENABLED, value)
        }

    // autoReplyMessage: 字符串, 默认为 "我现在不方便接听，稍后联系你。" (自动回复的短信内容)
    var autoReplyMessage: String
        get() = PluginConfigMethod.getString(KEY_AUTO_REPLY_MESSAGE, "我现在不方便接听，稍后联系你。")
        set(value) {
            PluginConfigMethod.putString(KEY_AUTO_REPLY_MESSAGE, value)
        }
}
