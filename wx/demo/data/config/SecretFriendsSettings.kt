package wx.demo.data.config

import android.content.Context
import android.content.SharedPreferences

object SecretFriendsSettings {

    private const val PREFS_NAME = "secret_friends_prefs"
    private var sharedPreferences: SharedPreferences? = null

    // Keys for SharedPreferences
    private const val KEY_IS_SECRET_FEATURE_ENABLED = "is_secret_feature_enabled"
    private const val KEY_SECRET_PASSWORD = "secret_password"
    private const val KEY_HIDDEN_FRIEND_IDS = "hidden_friend_ids"

    // Non-persistent setting
    var IsSecretModeActive: Boolean = false

    // Initialize SharedPreferences (must be called from Application or similar context)
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // IsSecretFeatureEnabled: Boolean, default to false
    var IsSecretFeatureEnabled: Boolean
        get() = sharedPreferences?.getBoolean(KEY_IS_SECRET_FEATURE_ENABLED, false) ?: false
        set(value) {
            sharedPreferences?.edit()?.putBoolean(KEY_IS_SECRET_FEATURE_ENABLED, value)?.apply()
        }

    // SecretPassword: String, default to an empty string
    var SecretPassword: String
        get() = sharedPreferences?.getString(KEY_SECRET_PASSWORD, "") ?: ""
        set(value) {
            sharedPreferences?.edit()?.putString(KEY_SECRET_PASSWORD, value)?.apply()
        }

    // HiddenFriendIds: Set<String>, default to an empty set
    var HiddenFriendIds: Set<String>
        get() = sharedPreferences?.getStringSet(KEY_HIDDEN_FRIEND_IDS, emptySet()) ?: emptySet()
        set(value) {
            sharedPreferences?.edit()?.putStringSet(KEY_HIDDEN_FRIEND_IDS, value)?.apply()
        }
}
