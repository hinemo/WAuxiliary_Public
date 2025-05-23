package wx.demo.hook.lifecycle

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle // 用于 onCreate 参数
// import android.util.Log // 被 PluginOtherMethod.log 替代
import wx.demo.data.config.SecretFriendsSettings
import me.hd.wauxv.tool.PluginOtherMethod // 新增导入
// 假设 WAuxiliary Plugin 提供以下回调注册机制，需根据实际API调整
// import me.hd.wauxv.callbacks.PluginCallback // 假设的回调接口
// import me.hd.wauxv.callbacks.PluginCallbackManager // 假设的回调管理器

// =====================================================================================
// 重要实现注意事项 (生命周期管理):
// 1. 目标 Activity 识别: 当使用 onActivityCreated/Paused 等回调时，务必准确识别目标 Activity
//    (例如 com.tencent.mm.ui.LauncherUI)，以避免在不相关的 Activity 中注册/注销监听器。
// 2. Context 有效性: 传递给 registerShakeDetector 的 Context 应确保有效。
//    applicationContext 通常是安全的选择。
// 3. 微信版本更新: WAuxiliary Plugin 的回调机制应相对稳定，但仍需关注插件更新和兼容性。
// =====================================================================================
object AppLifecycleHook : SensorEventListener {

    private const val TAG = "AppLifecycleHook" // 日志标签
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isShakeDetectorRegistered = false // 追踪摇一摇监听器是否已注册

    // 摇一摇检测参数
    private const val SHAKE_THRESHOLD_GRAVITY = 2.7F
    private const val SHAKE_SLOP_TIME_MS = 500
    private var mShakeTimestamp: Long = 0

    // 目标主Activity的类名 (示例)
    private const val TARGET_ACTIVITY_NAME = "com.tencent.mm.ui.LauncherUI"
    // 可以根据需要扩展为 Activity 名称列表
    // private val TARGET_ACTIVITY_NAMES = setOf("com.tencent.mm.ui.LauncherUI", "com.tencent.mm.plugin.profile.ui.ContactInfoUI")


    /**
     * 注册摇一摇检测传感器。
     * 需要从 Activity 的上下文中调用。
     *
     * 性能考虑: 确保在适当的生命周期方法中 (例如 onResume/onPause 或 WAuxiliary 插件对应的回调)
     * 与 unregisterShakeDetector 配对使用，以避免在相关UI未激活时不必要的电池消耗。
     */
    fun registerShakeDetector(context: Context) {
        if (isShakeDetectorRegistered) {
            PluginOtherMethod.log("$TAG: INFO: 摇一摇检测器已注册，无需重复注册。")
            return
        }
        if (sensorManager == null) {
            sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isShakeDetectorRegistered = true
            PluginOtherMethod.log("$TAG: INFO: 摇一摇检测器已注册。")
        } else {
            PluginOtherMethod.log("$TAG: WARN: 加速度计不可用，摇一摇检测已禁用。")
        }
    }

    /**
     * 注销摇一摇检测传感器。
     *
     * 性能考虑: 及时调用此方法 (例如在 onPause 或 WAuxiliary 插件对应的回调中)，以防止在应用/功能未使用时
     * 活动传感器造成的电池消耗。
     */
    fun unregisterShakeDetector() {
        if (!isShakeDetectorRegistered) {
            PluginOtherMethod.log("$TAG: INFO: 摇一摇检测器未注册，无需注销。")
            return
        }
        sensorManager?.unregisterListener(this)
        isShakeDetectorRegistered = false
        PluginOtherMethod.log("$TAG: INFO: 摇一摇检测器已注销。")
        // 根据生命周期情况，选择性地将 sensorManager 和 accelerometer 置空，但需注意下次注册时的重新初始化
        // sensorManager = null
        // accelerometer = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            val gForce = kotlin.math.sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }
                mShakeTimestamp = now
                PluginOtherMethod.log("$TAG: INFO: 检测到摇动，力度: $gForce")

                if (SecretFriendsSettings.IsSecretFeatureEnabled && SecretFriendsSettings.IsSecretModeActive) {
                    SecretFriendsSettings.IsSecretModeActive = false
                    PluginOtherMethod.log("$TAG: INFO: 因设备摇动，密友模式已停用。")

                    PluginOtherMethod.log("$TAG: INFO: TODO: 因摇动触发全局UI刷新以停用。(例如 广播Intent, notifyDataSetChanged)")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 对于这个简单实现可以忽略
    }

    /**
     * 初始化并注册 WAuxiliary Plugin 生命周期回调。
     * 此方法将由插件框架在适当的时候调用，用以设置此类提供的功能。
     * 它取代了之前使用通用方法Hook (如Xposed) 的方式来Hook Activity生命周期。
     *
     * @param context Android 上下文，如果插件框架提供，则可用于初始化。
     */
    fun initializeWithPluginCallbacks(context: Context?) {
        PluginOtherMethod.log("$TAG: INFO: 正在使用 WAuxiliary Plugin 回调进行初始化...")
        if (context != null) {
            SecretFriendsSettings.init(context.applicationContext) // 确保设置已初始化
            PluginOtherMethod.log("$TAG: INFO: AppLifecycleHook 设置已初始化 (通过 initializeWithPluginCallbacks)。")
        } else {
            PluginOtherMethod.log("$TAG: WARN: AppLifecycleHook 初始化时未能获取到 context，设置可能未正确初始化。")
        }

        // 1. 应用进入后台检测
        // PluginCallbackManager.registerAppBackgroundCallback { // 假设的注册方式
        //     if (SecretFriendsSettings.IsSecretModeActive) {
        //         SecretFriendsSettings.IsSecretModeActive = false
        //         PluginOtherMethod.log("$TAG: INFO: 密友模式已停用：应用进入后台 (通过 onAppBackground 回调)。")
        //         // UI刷新通常在下次返回前台时自动处理或由其他机制处理
        //     }
        // }
        PluginOtherMethod.log("$TAG: INFO: (概念) 已注册 onAppBackground 回调监听器。")


        // 2. 摇一摇检测器的注册 (特定Activity创建时)
        // PluginCallbackManager.registerActivityCreatedCallback { activity, _ -> // 假设的回调参数
        //     if (activity.javaClass.name == TARGET_ACTIVITY_NAME) {
        //          PluginOtherMethod.log("$TAG: INFO: 检测到目标 Activity (${activity.javaClass.name}) 创建，尝试注册摇一摇检测器。")
        //          registerShakeDetector(activity.applicationContext)
        //     }
        // }
        PluginOtherMethod.log("$TAG: INFO: (概念) 已注册 onActivityCreated 回调监听器 (用于摇一摇注册)。")

        // 3. 摇一摇检测器的注销 (特定Activity暂停时)
        // PluginCallbackManager.registerActivityPausedCallback { activity -> // 假设的回调参数
        //     if (activity.javaClass.name == TARGET_ACTIVITY_NAME) {
        //          PluginOtherMethod.log("$TAG: INFO: 检测到目标 Activity (${activity.javaClass.name}) 暂停，尝试注销摇一摇检测器。")
        //          unregisterShakeDetector()
        //     }
        // }
        PluginOtherMethod.log("$TAG: INFO: (概念) 已注册 onActivityPaused 回调监听器 (用于摇一摇注销)。")

        PluginOtherMethod.log("$TAG: INFO: AppLifecycleHook 已配置为使用 WAuxiliary Plugin 生命周期回调。")
    }

    // 旧的 applyHooks 方法不再是主要的集成方式，如果模块完全依赖插件回调，此方法可能被废弃或重构。
    // 为保持与先前任务的一致性，暂时保留并标记为待审阅或移除。
    /**
     * [已过时/待审阅] 初始化和应用 Hook 的占位符。
     * 此方法原用于基于Xposed等通用Hook框架的场景。
     * 若已迁移至 WAuxiliary Plugin 的特定回调，则此方法的功能应由 `initializeWithPluginCallbacks` 替代。
     * @param classLoader 用于查找类的类加载器。
     * @param context Android 上下文，如果可用且初始化需要。
     */
    @Deprecated("Prefer initializeWithPluginCallbacks if using WAuxiliary Plugin lifecycle callbacks")
    fun applyHooks(classLoader: ClassLoader, context: Context? = null) {
        PluginOtherMethod.log("$TAG: WARN: applyHooks (基于通用Hook框架的方法) 被调用。如果使用插件回调，请考虑迁移。")
        if (context != null && !SecretFriendsSettings.IsSecretModeActive) { // 避免重复初始化
            SecretFriendsSettings.init(context.applicationContext)
        }
        PluginOtherMethod.log("$TAG: INFO: 应用后台检测及摇一摇监听器的注册/注销现推荐通过 WAuxiliary Plugin 的特定生命周期回调实现。")
        PluginOtherMethod.log("$TAG: INFO: (applyHooks 中的占位符日志) 应用后台检测 Hook占位符已注册 (LauncherUI.onStop)。")
        PluginOtherMethod.log("$TAG: INFO: (applyHooks 中的占位符日志) 摇一摇检测器注册 Hook占位符已注册 (LauncherUI.onCreate)。")
        PluginOtherMethod.log("$TAG: INFO: (applyHooks 中的占位符日志) 摇一摇检测器注销 Hook占位符已注册 (LauncherUI.onPause)。")
    }
}
