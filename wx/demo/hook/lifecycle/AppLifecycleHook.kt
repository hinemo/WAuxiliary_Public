package wx.demo.hook.lifecycle

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle // For onCreate parameter
import android.util.Log
import wx.demo.data.config.SecretFriendsSettings

object AppLifecycleHook : SensorEventListener {

    private const val TAG = "AppLifecycleHook"
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // Shake detection parameters
    private const val SHAKE_THRESHOLD_GRAVITY = 2.7F
    private const val SHAKE_SLOP_TIME_MS = 500
    private var mShakeTimestamp: Long = 0

    /**
     * Registers the shake detector sensor.
     * Needs to be called from an Activity's context.
     *
     * PERFORMANCE CONSIDERATIONS: Ensure this is paired with unregisterShakeDetector
     * in appropriate lifecycle methods (e.g., onResume/onPause) to avoid unnecessary
     * battery drain when the relevant UI is not active.
     */
    fun registerShakeDetector(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Log.i(TAG, "Shake detector registered.")
        } else {
            Log.w(TAG, "Accelerometer not available, shake detection disabled.")
        }
    }

    /**
     * Unregisters the shake detector sensor.
     *
     * PERFORMANCE CONSIDERATIONS: Call this promptly (e.g., in onPause) to prevent
     * battery drain from an active sensor when the app/feature is not in use.
     */
    fun unregisterShakeDetector() {
        sensorManager?.unregisterListener(this)
        Log.i(TAG, "Shake detector unregistered.")
        // Optionally nullify sensorManager and accelerometer if appropriate for lifecycle
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

            // gForce will be close to 1 when there is no movement.
            val gForce = kotlin.math.sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                // Ignore shake events too close to each other (500ms)
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }
                mShakeTimestamp = now
                Log.i(TAG, "Shake detected with force: $gForce")

                if (SecretFriendsSettings.IsSecretFeatureEnabled && SecretFriendsSettings.IsSecretModeActive) {
                    SecretFriendsSettings.IsSecretModeActive = false
                    Log.i(TAG, "Secret mode deactivated due to device shake.")

                    // UI REFRESH MECHANISM:
                    // Since the app is in the foreground when a shake occurs, UI elements
                    // (chat lists, contact lists, search results) need to be refreshed
                    // to reflect that Secret Mode is now OFF (i.e., hidden items should become hidden again).
                    // This could involve:
                    // - Broadcasting an Intent that relevant UI components listen for.
                    // - Calling notifyDataSetChanged() on adapters if direct references are available.
                    // - Invalidating caches or triggering data re-fetches for lists.
                    // Example: context.sendBroadcast(Intent("wx.demo.REFRESH_UI_ACTION"))
                    Log.i(TAG, "TODO: Trigger global UI refresh for deactivation due to shake. (e.g., broadcast Intent, notifyDataSetChanged)")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be ignored for this simple implementation
    }

    /**
     * Placeholder for initializing and applying the hooks.
     * This method would be called by the hooking framework.
     * @param classLoader The classloader to use for finding classes.
     * @param context The Android context, if available and needed for initialization.
     */
    fun applyHooks(classLoader: ClassLoader, context: Context? = null) {
        if (context != null) {
            // Initialize settings if needed, though shake registration also gets context
            SecretFriendsSettings.init(context.applicationContext)
        }

        // ERROR HANDLING: Consider wrapping each findAndHookMethod call and its callback
        // logic in a try-catch block to prevent the entire app from crashing if a hook fails
        // due to WeChat updates or unexpected errors. Log exceptions for debugging.
        // Example: try { /* findAndHookMethod(...) */ } catch (t: Throwable) { Log.e(TAG, "Hook failed", t); }

        // --- Conceptual Hook for Activity onStop (Background Detection) ---
        /*
        // IMPORTANCE OF CORRECT CLASS/METHOD NAMES:
        // The developer MUST replace "com.tencent.mm.ui.LauncherUI" and "onStop"
        // with actual, verified names from the target WeChat version using reverse engineering tools.
        findAndHookMethod(
            "com.tencent.mm.ui.LauncherUI", // Target main UI activity (actual name needed)
            classLoader,
            "onStop", // Or "onPause" (actual name needed)
            // No parameters for onStop usually
            { param ->
                // param.callOriginalMethod() // Call original first or last depending on need

                if (SecretFriendsSettings.IsSecretModeActive) {
                    SecretFriendsSettings.IsSecretModeActive = false
                    Log.i(TAG, "Secret mode deactivated: app went to background (onStop).")
                    // UI REFRESH CONSIDERATIONS:
                    // When the app resumes, UI should naturally reflect IsSecretModeActive = false.
                    // If onPause is used, and parts of the UI remain active or resume quickly without a full
                    // redraw, an explicit refresh might be needed here or upon resume.
                    // For onStop, usually a full redraw occurs on next start, mitigating this.
                }
            }
        )
        */
        Log.i(TAG, "App background detection hook placeholder registered for LauncherUI.onStop.")

        // --- Conceptual Hooks for Shake Detector Registration/Unregistration ---
        // Hook Activity.onCreate() to register listener
        /*
        // IMPORTANCE OF CORRECT CLASS/METHOD NAMES:
        // The developer MUST replace "com.tencent.mm.ui.LauncherUI" and "onCreate"
        // with actual, verified names from the target WeChat version.
        findAndHookMethod(
            "com.tencent.mm.ui.LauncherUI", // Target main UI activity (actual name needed)
            classLoader,
            "onCreate",
            Bundle::class.java, // Parameter type for onCreate
            { param ->
                // param.callOriginalMethod() // It's common to call original onCreate first

                val activity = param.thisObject as? Activity
                if (activity != null) {
                    registerShakeDetector(activity.applicationContext)
                } else {
                    Log.w(TAG, "Failed to get Activity instance from onCreate hook to register shake detector.")
                }
            }
        )
        */
        Log.i(TAG, "Shake detector registration hook placeholder for LauncherUI.onCreate.")

        // Hook Activity.onPause() or onStop() or onDestroy() to unregister listener
        /*
        // IMPORTANCE OF CORRECT CLASS/METHOD NAMES:
        // The developer MUST replace "com.tencent.mm.ui.LauncherUI" and "onPause"
        // with actual, verified names from the target WeChat version.
        findAndHookMethod(
            "com.tencent.mm.ui.LauncherUI", // Target main UI activity (actual name needed)
            classLoader,
            "onPause", // onPause is good for sensors to save battery when activity not active (actual name needed)
            { param ->
                // param.callOriginalMethod()

                unregisterShakeDetector()
            }
        )
        */
        Log.i(TAG, "Shake detector unregistration hook placeholder for LauncherUI.onPause.")
        
        // Alternative: Hook Application's lifecycle callbacks if available/suitable
        // This might provide a more global way to manage shake detection or background status.
    }
}
