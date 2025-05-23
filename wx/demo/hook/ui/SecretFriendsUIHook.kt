package wx.demo.hook.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Looper
import android.text.InputType
// import android.util.Log // 被 PluginOtherMethod.log 替代
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
// import android.widget.Toast // 被 PluginOtherMethod.toast 替代
import wx.demo.data.config.SecretFriendsSettings

// --- 假设的框架导入 ---
// 这些基于任务描述。如果实际路径/名称不同，
// 则需要进行调整。
// 框架依赖: 此 Hook 依赖于 `me.hd.wauxv` 框架中的 `SwitchHook` 和 `IDexFind`。
// 确保这些基类/接口在目标环境中已正确实现并可用。
// `SwitchHook` 的行为 (例如，它如何添加UI条目并调用 `onClick`) 至关重要。
import me.hd.wauxv.hook.base.SwitchHook
import me.hd.wauxv.hook.core.dex.IDexFind
import me.hd.wauxv.hook.core.dex.DexKitBridge
import me.hd.wauxv.tool.PluginContactMethod // 新增导入
import me.hd.wauxv.tool.PluginOtherMethod // 新增导入
// --- 假设的框架导入结束 ---

object SecretFriendsUIHook : SwitchHook(
    id = "SecretFriendsUIHook",
    title = SecretFriendsUIHook.funcName, // SwitchPreference的标题
    summary = SecretFriendsUIHook.funcDesc, // SwitchPreference的摘要
    defaultEnabled = true // Hook条目本身是启用的，实际功能由IsSecretFeatureEnabled控制
), IDexFind {

    private const val TAG = "SecretFriendsUIHook" // 日志标签

    // 元数据 (SwitchHook 也可能读取这些属性)
    const val location = "杂项" // 或 "隐私" 或其他相关设置类别
    const val funcName = "微信密友" // 功能名称 (已是中文)
    const val funcDesc = "设置微信密友功能，隐藏指定的好友和群聊" // 功能描述 (已是中文)

    // 辅助函数，用于更新视图的可见性
    private fun updateViewVisibility(view: View, isVisible: Boolean) {
        view.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * 当用户点击由 SwitchHook 创建的设置条目时，假定会调用此方法。
     *
     * CONTEXT 处理: 此处提供的 `context` 至关重要。它很可能是一个 Activity 上下文。
     * 确保其有效且适合显示对话框。
     * UI 线程: 此方法很可能在 UI 线程上调用。如果不是，则对话框操作
     * 必须分派到主线程 (例如，使用 `activity.runOnUiThread`)。
     */
    override fun onClick(context: Context) {
        // SwitchHook 本身可能处理 Hook 条目的启用/禁用状态。
        // 在这里，我们打开自定义的设置对话框。
        
        // 错误处理: 考虑将 `showSettingsDialog` 包装在 try-catch 块中
        // 以处理对话框创建或显示期间的潜在错误 (例如，无效的上下文，
        // WindowManager 的问题)。
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                showSettingsDialog(context)
            } else {
                PluginOtherMethod.log("$TAG: WARN: onClick 在非UI线程上调用。对话框显示已跳过。开发者应将其分派到UI线程。")
                // 示例: (context as? Activity)?.runOnUiThread { showSettingsDialog(context) }
            }
        } catch (e: Exception) {
            PluginOtherMethod.log("$TAG: ERROR: 从 onClick 显示设置对话框时出错: ${e.message}")
            PluginOtherMethod.toast("打开设置时出错")
        }
    }

    /**
     * IDexFind 的实现。
     * 对于此 Hook，如果 SwitchHook 处理 UI 注入并且 onClick 提供交互点，
     * 则可能不需要直接的 DEX 搜索。
     * 如果需要查找特定的类/方法以进行更深入的集成，则会使用此方法
     * (例如，查找确切的设置 Activity 类以验证上下文或注入更复杂的UI)。
     */
    override fun dexFind(dexKit: DexKitBridge) {
        // 存根实现。
        // 如果以后需要，可以在此处添加实际的 dex 查找逻辑。
        // 例如，如果 SwitchHook 没有自动创建条目，则查找要注入的特定设置 Activity。
        PluginOtherMethod.log("$TAG: INFO: dexFind 已调用。如果需要，DexKitBridge 可用。")
    }

    /**
     * 显示密友功能的设置对话框。
     *
     * CONTEXT 处理: `context.applicationContext` 用于 `SecretFriendsSettings.init`。
     * 对于UI元素 (AlertDialog, Views)，使用传递的 `context` (可能是 Activity)。
     * 确保此 `context` 在对话框的整个生命周期内有效。
     *
     * UI 线程: 此方法必须在 UI 线程上调用，因为它创建并显示对话框。
     */
    fun showSettingsDialog(context: Context) {
        // 初始化 SecretFriendsSettings
        // 健壮性: 确保 context.applicationContext 有效。
        try {
            SecretFriendsSettings.init(context.applicationContext)
        } catch (e: Exception) {
            PluginOtherMethod.log("$TAG: ERROR: 初始化 SecretFriendsSettings 失败: ${e.message}")
            PluginOtherMethod.toast("初始化设置失败")
            return // 没有设置无法继续
        }

        val dialogContext = PluginOtherMethod.getTopActivity() ?: context
        PluginOtherMethod.log("$TAG: INFO: showSettingsDialog 使用的 context: ${dialogContext.javaClass.name}")


        val dialogView: LinearLayout = try {
            LinearLayout(dialogContext).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(dialogContext, 24), dpToPx(dialogContext, 16), dpToPx(dialogContext, 24), dpToPx(dialogContext, 16))

                // 1. IsSecretFeatureEnabled 的开关 (密友功能总开关)
                val featureSwitch = Switch(dialogContext).apply {
                    text = "启用密友功能"
                    isChecked = SecretFriendsSettings.IsSecretFeatureEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        SecretFriendsSettings.IsSecretFeatureEnabled = isChecked
                        PluginOtherMethod.toast("功能已${if (isChecked) "启用" else "禁用"}")
                    }
                }
                addView(featureSwitch, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(dialogContext, 16) })

                // 2. SecretPassword 的 EditText (密友密码设置)
                val passwordLabel = TextView(dialogContext).apply { text = "密友密码:" }
                addView(passwordLabel)
                val passwordEditText = EditText(dialogContext).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setText(SecretFriendsSettings.SecretPassword)
                    hint = "输入新密码"
                }
                addView(passwordEditText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(dialogContext, 8) })
                val savePasswordButton = Button(dialogContext).apply {
                    text = "保存密码"
                    setOnClickListener {
                        SecretFriendsSettings.SecretPassword = passwordEditText.text.toString()
                        PluginOtherMethod.toast("密码已保存!")
                    }
                }
                addView(savePasswordButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(dialogContext, 16) })


                // --- 自定义通知设置 ---
                val separatorNotifications = View(dialogContext).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dialogContext, 1)).apply { bottomMargin = dpToPx(dialogContext,16); topMargin = dpToPx(dialogContext, 8)}
                    setBackgroundColor(0xCCCCCCCC.toInt())
                }
                addView(separatorNotifications)
                
                val customNotificationSwitch = Switch(dialogContext).apply {
                    text = "启用密友自定义通知"
                    isChecked = SecretFriendsSettings.isCustomNotificationsEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        SecretFriendsSettings.isCustomNotificationsEnabled = isChecked
                        PluginOtherMethod.toast("自定义通知已${if (isChecked) "启用" else "禁用"}")
                    }
                }
                addView(customNotificationSwitch, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(dialogContext, 16) })
                val soundUriLabel = TextView(dialogContext).apply { text = "自定义通知声音 (URI或路径):" }
                addView(soundUriLabel)
                val soundUriEditText = EditText(dialogContext).apply {
                    setText(SecretFriendsSettings.customNotificationSoundUri)
                    hint = "留空使用系统默认"; isSingleLine = true
                }
                addView(soundUriEditText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(dialogContext, 8) })
                val saveSoundUriButton = Button(dialogContext).apply {
                    text = "保存声音设置"
                    setOnClickListener {
                        SecretFriendsSettings.customNotificationSoundUri = soundUriEditText.text.toString()
                        PluginOtherMethod.toast("自定义声音已保存!")
                    }
                }
                addView(saveSoundUriButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(dialogContext, 16) })
                val styleLabel = TextView(dialogContext).apply { text = "自定义通知样式 (标识符):" }
                addView(styleLabel)
                val styleEditText = EditText(dialogContext).apply {
                    setText(SecretFriendsSettings.customNotificationStyle)
                    hint = "例如: 'default', 'led_red', 'vibrate_short'"; isSingleLine = true
                }
                addView(styleEditText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(dialogContext, 8) })
                val saveStyleButton = Button(dialogContext).apply {
                    text = "保存样式设置"
                    setOnClickListener {
                        SecretFriendsSettings.customNotificationStyle = styleEditText.text.toString()
                        PluginOtherMethod.toast("自定义样式已保存!")
                    }
                }
                addView(saveStyleButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(dialogContext, 16) })


                // --- 呼叫拒绝设置 ---
                val separatorCallRejection = View(dialogContext).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dialogContext, 1)).apply { bottomMargin = dpToPx(dialogContext,16); topMargin = dpToPx(dialogContext, 8)}
                    setBackgroundColor(0xCCCCCCCC.toInt())
                }
                addView(separatorCallRejection)

                val callRejectionContainer = LinearLayout(dialogContext).apply { orientation = LinearLayout.VERTICAL }
                val delaySecondsContainer = LinearLayout(dialogContext).apply { orientation = LinearLayout.VERTICAL }
                val autoReplyMessageContainer = LinearLayout(dialogContext).apply { orientation = LinearLayout.VERTICAL }

                val enableCallRejectionSwitch = Switch(dialogContext).apply {
                    text = "启用密友来电拒绝"
                    isChecked = SecretFriendsSettings.isCallRejectionEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        SecretFriendsSettings.isCallRejectionEnabled = isChecked
                        updateViewVisibility(callRejectionContainer, isChecked)
                        PluginOtherMethod.toast("来电拒绝功能已${if (isChecked) "启用" else "禁用"}")
                    }
                }
                addView(enableCallRejectionSwitch)
                
                callRejectionContainer.apply {
                    val rejectionModeLabel = TextView(dialogContext).apply { text = "拒绝模式:" }
                    addView(rejectionModeLabel)

                    val rejectionModeRadioGroup = RadioGroup(dialogContext).apply {
                        orientation = RadioGroup.HORIZONTAL
                        val modeSilent = RadioButton(dialogContext).apply { text = "静默拒绝"; id = View.generateViewId() }
                        val modeDelayed = RadioButton(dialogContext).apply { text = "延迟拒绝"; id = View.generateViewId() }
                        addView(modeSilent)
                        addView(modeDelayed)

                        setOnCheckedChangeListener { _, checkedId ->
                            val newMode = if (checkedId == modeSilent.id) "silent" else "delayed"
                            SecretFriendsSettings.callRejectionMode = newMode
                            updateViewVisibility(delaySecondsContainer, newMode == "delayed")
                            PluginOtherMethod.toast("拒绝模式已设为: $newMode")
                        }
                        if (SecretFriendsSettings.callRejectionMode == "silent") {
                            check(modeSilent.id)
                            updateViewVisibility(delaySecondsContainer, false)
                        } else {
                            check(modeDelayed.id)
                            updateViewVisibility(delaySecondsContainer, true)
                        }
                    }
                    addView(rejectionModeRadioGroup)
                    
                    delaySecondsContainer.apply {
                        val delayLabel = TextView(dialogContext).apply { text = "延迟时间 (秒):" }
                        addView(delayLabel)
                        val delayEditText = EditText(dialogContext).apply {
                            inputType = InputType.TYPE_CLASS_NUMBER
                            setText(SecretFriendsSettings.callRejectionDelaySeconds.toString())
                            hint = "输入秒数"
                        }
                        addView(delayEditText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply{ bottomMargin = dpToPx(dialogContext, 8)})
                        val saveDelayButton = Button(dialogContext).apply {
                            text = "保存延迟"
                            setOnClickListener {
                                try {
                                    SecretFriendsSettings.callRejectionDelaySeconds = delayEditText.text.toString().toInt()
                                    PluginOtherMethod.toast("延迟时间已保存!")
                                } catch (nfe: NumberFormatException) {
                                    PluginOtherMethod.toast("请输入有效的数字!")
                                }
                            }
                        }
                        addView(saveDelayButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply{ bottomMargin = dpToPx(dialogContext, 16)})
                    }
                    addView(delaySecondsContainer)
                    updateViewVisibility(delaySecondsContainer, SecretFriendsSettings.callRejectionMode == "delayed")

                    val enableAutoReplySwitch = Switch(dialogContext).apply {
                        text = "启用拒绝后自动回复"
                        isChecked = SecretFriendsSettings.isAutoReplyEnabled
                        setOnCheckedChangeListener { _, isChecked ->
                            SecretFriendsSettings.isAutoReplyEnabled = isChecked
                            updateViewVisibility(autoReplyMessageContainer, isChecked)
                            PluginOtherMethod.toast("自动回复已${if (isChecked) "启用" else "禁用"}")
                        }
                    }
                    addView(enableAutoReplySwitch)
                    
                    autoReplyMessageContainer.apply {
                        val autoReplyLabel = TextView(dialogContext).apply { text = "自动回复内容:" }
                        addView(autoReplyLabel)
                        val autoReplyEditText = EditText(dialogContext).apply {
                            setText(SecretFriendsSettings.autoReplyMessage)
                            hint = "输入拒绝后自动发送的消息"
                            minLines = 2 
                        }
                        addView(autoReplyEditText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply{ bottomMargin = dpToPx(dialogContext, 8)})
                        val saveAutoReplyButton = Button(dialogContext).apply {
                            text = "保存回复"
                            setOnClickListener {
                                SecretFriendsSettings.autoReplyMessage = autoReplyEditText.text.toString()
                                PluginOtherMethod.toast("自动回复内容已保存!")
                            }
                        }
                        addView(saveAutoReplyButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply{ bottomMargin = dpToPx(dialogContext, 16)})
                    }
                    addView(autoReplyMessageContainer)
                    updateViewVisibility(autoReplyMessageContainer, SecretFriendsSettings.isAutoReplyEnabled)
                }
                addView(callRejectionContainer)
                updateViewVisibility(callRejectionContainer, SecretFriendsSettings.isCallRejectionEnabled)

                val separatorManageFriends = View(dialogContext).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dialogContext, 1))
                    setBackgroundColor(0xCCCCCCCC.toInt()) 
                    (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(dialogContext, 8) 
                    (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(dialogContext, 16)
                }
                addView(separatorManageFriends)
                
                // "管理隐藏好友" 按钮 - 修改其点击事件
                val manageFriendsButton = Button(dialogContext).apply {
                    text = "管理隐藏好友"
                    setOnClickListener {
                        showManageHiddenFriendsDialog(dialogContext) // 传入当前对话框的上下文
                    }
                }
                addView(manageFriendsButton)
            }
        } catch (e: Exception) {
            PluginOtherMethod.log("$TAG: ERROR: 创建对话框视图时出错: ${e.message}")
            PluginOtherMethod.toast("创建设置视图时出错")
            return
        }

        try {
            // 使用 dialogContext (可能是 getTopActivity() 的结果)
            AlertDialog.Builder(dialogContext)
                .setTitle("$funcName 设置") 
                .setView(dialogView)
                .setPositiveButton("关闭") { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            PluginOtherMethod.log("$TAG: ERROR: 显示 AlertDialog 时出错: ${e.message}")
            PluginOtherMethod.toast("显示设置对话框时出错")
        }
    }

    /**
     * 显示管理隐藏好友列表的对话框。
     * @param parentContext 调用此对话框的父级上下文。
     */
    private fun showManageHiddenFriendsDialog(parentContext: Context) {
        // **数据结构假设:**
        //   - `PluginContactMethod.getFriendList()` 和 `getGroupList()` 返回 `List<Map<String, String>>`。
        //   - 每个 Map 中包含 "wxid" (唯一标识符) 和 "nickname" (好友) 或 "name" (群聊) 作为显示名。
        //   - 如果实际返回的键名不同 (例如 "id", "remark", "displayName"), 则需要调整下面的代码。
        PluginOtherMethod.log("$TAG: INFO: 打开管理密友列表对话框。假设好友/群聊列表项为 Map<String, String> 包含 'wxid' 和 'nickname'/'name'。")

        val dialogContext = PluginOtherMethod.getTopActivity() ?: parentContext
        PluginOtherMethod.log("$TAG: INFO: showManageHiddenFriendsDialog 使用的 context: ${dialogContext.javaClass.name}")


        val currentHiddenIds = SecretFriendsSettings.HiddenFriendIds
        val allCheckBoxes = mutableListOf<CheckBox>() // 用于之后收集选中状态

        val scrollView = ScrollView(dialogContext)
        val mainLayout = LinearLayout(dialogContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(dialogContext, 20), dpToPx(dialogContext, 10), dpToPx(dialogContext, 20), dpToPx(dialogContext, 10))
        }
        scrollView.addView(mainLayout)

        // 获取好友列表
        val friendList = PluginContactMethod.getFriendList()
        PluginOtherMethod.log("$TAG: INFO: 获取到好友列表数量: ${friendList?.size ?: 0}")
        mainLayout.addView(TextView(dialogContext).apply { text = "好友列表:"; textSize = 16f })
        if (friendList.isNullOrEmpty()) {
            mainLayout.addView(TextView(dialogContext).apply { text = "好友列表为空或获取失败"; setPadding(0, dpToPx(dialogContext, 8), 0, dpToPx(dialogContext, 8)) })
        } else {
            friendList.forEach { friendMap ->
                val wxid = friendMap["wxid"]
                val nickname = friendMap["nickname"] ?: wxid // 如果昵称为空，则使用wxid
                if (!wxid.isNullOrEmpty()) {
                    val checkBox = CheckBox(dialogContext).apply {
                        text = nickname
                        tag = wxid
                        isChecked = currentHiddenIds.contains(wxid)
                    }
                    allCheckBoxes.add(checkBox)
                    mainLayout.addView(checkBox)
                }
            }
        }
        
        // 添加分隔
        mainLayout.addView(View(dialogContext).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(dialogContext, 1)).apply {
                topMargin = dpToPx(dialogContext, 10)
                bottomMargin = dpToPx(dialogContext, 10)
            }
            setBackgroundColor(0xCCCCCCCC.toInt())
        })

        // 获取群聊列表
        val groupList = PluginContactMethod.getGroupList()
        PluginOtherMethod.log("$TAG: INFO: 获取到群聊列表数量: ${groupList?.size ?: 0}")
        mainLayout.addView(TextView(dialogContext).apply { text = "群聊列表:"; textSize = 16f })
        if (groupList.isNullOrEmpty()) {
            mainLayout.addView(TextView(dialogContext).apply { text = "群聊列表为空或获取失败"; setPadding(0, dpToPx(dialogContext, 8), 0, dpToPx(dialogContext, 8)) })
        } else {
            groupList.forEach { groupMap ->
                val wxid = groupMap["wxid"] // 假设群聊也有 wxid
                val name = groupMap["name"] ?: wxid // 如果名称为空，则使用wxid
                 if (!wxid.isNullOrEmpty()) {
                    val checkBox = CheckBox(dialogContext).apply {
                        text = name
                        tag = wxid
                        isChecked = currentHiddenIds.contains(wxid)
                    }
                    allCheckBoxes.add(checkBox)
                    mainLayout.addView(checkBox)
                }
            }
        }

        AlertDialog.Builder(dialogContext)
            .setTitle("管理密友列表")
            .setView(scrollView)
            .setPositiveButton("保存") { dialog, _ ->
                val newHiddenIds = mutableSetOf<String>()
                allCheckBoxes.forEach { checkBox ->
                    if (checkBox.isChecked) {
                        (checkBox.tag as? String)?.let { wxid ->
                            newHiddenIds.add(wxid)
                        }
                    }
                }
                SecretFriendsSettings.HiddenFriendIds = newHiddenIds
                PluginOtherMethod.toast("密友列表已更新")
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }


    /**
     * 将 dp 转换为像素的实用工具。
     * 健壮性: 确保 `context.resources` 可用。如果 `context` 本身有效，
     * 这通常是安全的。
     */
    private fun dpToPx(context: Context, dp: Int): Int {
        return try {
            (dp * context.resources.displayMetrics.density).toInt()
        } catch (e: Exception) {
            PluginOtherMethod.log("$TAG: ERROR: dpToPx 出错，默认为 dp 值: $dp: ${e.message}")
            dp // 后备值，但这表示上下文或资源存在问题
        }
    }
}
