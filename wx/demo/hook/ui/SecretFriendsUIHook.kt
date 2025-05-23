package wx.demo.hook.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import wx.demo.data.config.SecretFriendsSettings

// --- Assumed Framework Imports ---
// These are based on the task description. If the actual paths/names differ,
// these would need to be adjusted.
// FRAMEWORK DEPENDENCY: This hook relies on `SwitchHook` and `IDexFind` from the
// `me.hd.wauxv` framework. Ensure these base classes/interfaces are correctly
// implemented and available in the target environment. The behavior of `SwitchHook`
// (e.g., how it adds the UI entry and calls `onClick`) is critical.
import me.hd.wauxv.hook.base.SwitchHook
import me.hd.wauxv.hook.core.dex.IDexFind
import me.hd.wauxv.hook.core.dex.DexKitBridge
// --- End Assumed Framework Imports ---

object SecretFriendsUIHook : SwitchHook(
    id = "SecretFriendsUIHook",
    title = SecretFriendsUIHook.funcName, // Title for the switch/preference
    summary = SecretFriendsUIHook.funcDesc, // Summary for the switch/preference
    defaultEnabled = true // The hook entry itself is enabled, actual feature is controlled by IsSecretFeatureEnabled
), IDexFind {

    private const val TAG = "SecretFriendsUIHook"

    // Metadata (can also be used by SwitchHook if it reads these properties)
    const val location = "杂项" // Or "Privacy" or other relevant settings category
    const val funcName = "微信密友"
    const val funcDesc = "设置微信密友功能，隐藏指定的好友和群聊"

    /**
     * This method is assumed to be called when the user clicks on the settings entry
     * created by the SwitchHook.
     *
     * CONTEXT HANDLING: The `context` provided here is crucial. It's likely an Activity context.
     * Ensure it's valid and suitable for displaying a dialog.
     * UI THREADING: This method is likely called on the UI thread. If not, dialog operations
     * must be dispatched to the main thread (e.g., using `activity.runOnUiThread`).
     */
    override fun onClick(context: Context) {
        // The SwitchHook itself might handle the enabled/disabled state for the hook entry.
        // Here, we open our custom settings dialog.
        
        // ERROR HANDLING: Consider wrapping `showSettingsDialog` in a try-catch block
        // to handle potential errors during dialog creation or display (e.g., invalid context,
        // issues with window manager).
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                showSettingsDialog(context)
            } else {
                Log.w(TAG, "onClick called on non-UI thread. Dialog display skipped. Developer should dispatch to UI thread.")
                // Example: (context as? Activity)?.runOnUiThread { showSettingsDialog(context) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing settings dialog from onClick", e)
            Toast.makeText(context, "Error opening settings.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Implementation for IDexFind.
     * For this hook, direct DEX searching might not be needed if SwitchHook handles
     * the UI injection and onClick provides the interaction point.
     * If specific classes/methods needed to be found for deeper integration,
     * this method would be used (e.g., to find the exact settings Activity class to
     * verify context or inject more complex UI).
     */
    override fun dexFind(dexKit: DexKitBridge) {
        // Stub implementation.
        // Actual dex finding logic can be added here if needed later.
        // For example, finding a specific settings Activity to inject into if SwitchHook
        // didn't automatically create an entry.
        Log.i(TAG, "dexFind called. DexKitBridge available if needed.")
    }

    /**
     * Shows the settings dialog for Secret Friends feature.
     *
     * CONTEXT HANDLING: `context.applicationContext` is used for `SecretFriendsSettings.init`.
     * For UI elements (AlertDialog, Views), the passed `context` (likely Activity) is used.
     * Ensure this `context` is valid throughout the dialog's lifecycle.
     *
     * UI THREADING: This method MUST be called on the UI thread as it creates and shows a dialog.
     */
    fun showSettingsDialog(context: Context) {
        // Initialize SecretFriendsSettings
        // ROBUSTNESS: Ensure context.applicationContext is valid.
        try {
            SecretFriendsSettings.init(context.applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SecretFriendsSettings", e)
            Toast.makeText(context, "Error initializing settings.", Toast.LENGTH_SHORT).show()
            return // Cannot proceed without settings
        }

        val dialogView: LinearLayout = try {
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(context, 24), dpToPx(context, 16), dpToPx(context, 24), dpToPx(context, 16))

                // 1. Switch for IsSecretFeatureEnabled
                val featureSwitch = Switch(context).apply {
                    text = "Enable Secret Friends Feature"
                    isChecked = SecretFriendsSettings.IsSecretFeatureEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        SecretFriendsSettings.IsSecretFeatureEnabled = isChecked
                        Toast.makeText(context, "Feature enabled: $isChecked", Toast.LENGTH_SHORT).show()
                    }
                }
                addView(featureSwitch, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(context, 16) })

                // 2. EditText for SecretPassword
                val passwordLabel = TextView(context).apply {
                    text = "Secret Password:"
                }
                addView(passwordLabel)

                val passwordEditText = EditText(context).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setText(SecretFriendsSettings.SecretPassword)
                    hint = "Enter new password"
                }
                addView(passwordEditText, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(context, 8) })

                // 3. "Save Password" button
                val savePasswordButton = Button(context).apply {
                    text = "Save Password"
                    setOnClickListener {
                        val newPassword = passwordEditText.text.toString()
                        SecretFriendsSettings.SecretPassword = newPassword
                        Toast.makeText(context, "Password saved!", Toast.LENGTH_SHORT).show()
                    }
                }
                addView(savePasswordButton, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(context, 16) })

                // 4. "Manage Hidden Friends" button
                val manageFriendsButton = Button(context).apply {
                    text = "Manage Hidden Friends"
                    setOnClickListener {
                        Toast.makeText(context, "Manage Hidden Friends: Not yet implemented", Toast.LENGTH_LONG).show()
                    }
                }
                addView(manageFriendsButton)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating dialog view", e)
            Toast.makeText(context, "Error creating settings view.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            AlertDialog.Builder(context)
                .setTitle("$funcName Settings")
                .setView(dialogView)
                .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            // This can happen if context is invalid (e.g. Activity finishing) or other WindowManager issues.
            Log.e(TAG, "Error showing AlertDialog", e)
            Toast.makeText(context, "Error displaying settings dialog.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Utility to convert dp to pixels.
     * ROBUSTNESS: Ensure `context.resources` is available. This should generally be safe
     * if `context` itself is valid.
     */
    private fun dpToPx(context: Context, dp: Int): Int {
        return try {
            (dp * context.resources.displayMetrics.density).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error in dpToPx, defaulting to dp value: $dp", e)
            dp // Fallback, though this indicates a problem with context or resources
        }
    }
}
