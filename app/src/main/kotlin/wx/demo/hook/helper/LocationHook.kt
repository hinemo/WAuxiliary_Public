package wx.demo.hook.helper

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Parcelable
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import me.hd.wauxv.data.config.DefaultData
import me.hd.wauxv.data.config.DescriptorData
import me.hd.wauxv.data.factory.WxProcess
import me.hd.wauxv.databinding.ModuleDialogLocationBinding
import me.hd.wauxv.factory.showDialog
import me.hd.wauxv.hook.anno.HookAnno
import me.hd.wauxv.hook.anno.ViewAnno
import me.hd.wauxv.hook.base.SwitchHook
import me.hd.wauxv.hook.core.dex.IDexFind
import me.hd.wauxv.hook.factory.findDexClassMethod
import me.hd.wauxv.hook.factory.toDexMethod
import me.hd.wauxv.hook.factory.toLazyAppClass
import org.json.JSONArray
import org.json.JSONObject
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
@HookAnno
@ViewAnno
object LocationHook : SwitchHook("LocationHook"), IDexFind {

    private const val TAG = "WAuxiliary_LocationHook"

    // Data class for favorite locations
    data class FavoriteLocationItem(val name: String, val latitude: Double, val longitude: Double)

    private object MethodListener : DescriptorData("LocationHook.MethodListener")
    private object MethodListenerWgs84 : DescriptorData("LocationHook.MethodListenerWgs84")
    private object MethodDefaultManager : DescriptorData("LocationHook.MethodDefaultManager")
    private object MethodSelectPoiMapOnClick : DescriptorData("LocationHook.MethodSelectPoiMapOnClick")
    private object ValLatitude : DefaultData("LocationHook.ValLatitude", floatDefVal = LATITUDE_DEF_VAL)
    private object ValLongitude : DefaultData("LocationHook.ValLongitude", floatDefVal = LONGITUDE_DEF_VAL)
    private object FavoriteLocations : DefaultData("LocationHook.FavoriteLocations", stringDefVal = "[]")

    private const val LATITUDE_DEF_VAL = 31.135633f
    private const val LONGITUDE_DEF_VAL = 121.66625f
    private lateinit var binding: ModuleDialogLocationBinding
    private val RedirectUIClass by "com.tencent.mm.plugin.location.ui.RedirectUI".toLazyAppClass()

    override val location = "辅助"
    override val funcName = "虚拟定位"
    override val funcDesc = "将腾讯定位SDK结果虚拟为指定经纬度"
    override var onClick: ((View) -> Unit)? = { layoutView ->
        binding = ModuleDialogLocationBinding.inflate(LayoutInflater.from(layoutView.context))
        binding.moduleDialogBtnLocationSelect.setOnClickListener {
            val activity = layoutView.context as Activity
            activity.startActivityForResult(Intent(layoutView.context, RedirectUIClass).apply { putExtra("map_view_type", 8) }, 6)
        }
        binding.moduleDialogEdtLocationLatitude.setText("${ValLatitude.floatVal}")
        binding.moduleDialogEdtLocationLongitude.setText("${ValLongitude.floatVal}")

        // Create a new horizontal LinearLayout for the favorite buttons
        val favoritesButtonsLayout = LinearLayout(layoutView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
        }

        val btnAddFavorite = Button(layoutView.context).apply {
            text = "添加到收藏夹"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                marginEnd = 8 // Add some margin between buttons
            }
            setOnClickListener {
                val currentLatStr = binding.moduleDialogEdtLocationLatitude.text.toString()
                val currentLonStr = binding.moduleDialogEdtLocationLongitude.text.toString()

                val lat = currentLatStr.toDoubleOrNull()
                val lon = currentLonStr.toDoubleOrNull()

                if (lat == null || lon == null) {
                    Toast.makeText(layoutView.context, "无效的经纬度", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val nameInputView = EditText(layoutView.context).apply {
                    hint = "输入收藏名称"
                    inputType = InputType.TYPE_CLASS_TEXT
                }

                AlertDialog.Builder(layoutView.context)
                    .setTitle("添加到收藏夹")
                    .setView(nameInputView)
                    .setPositiveButton("保存") { dialog, _ ->
                        val name = nameInputView.text.toString().trim()
                        if (name.isEmpty()) {
                            Toast.makeText(layoutView.context, "名称不能为空", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        try {
                            Log.d(TAG, "Attempting to save favorite: Name='$name', Lat=$lat, Lon=$lon")
                            val oldJsonString = FavoriteLocations.stringVal
                            Log.d(TAG, "Current favorites JSON (before add): $oldJsonString")

                            val jsonArray = JSONArray(oldJsonString)
                            val newFavorite = JSONObject().apply {
                                put("name", name)
                                put("latitude", lat)
                                put("longitude", lon)
                            }
                            jsonArray.put(newFavorite)
                            val newJsonString = jsonArray.toString()
                            FavoriteLocations.stringVal = newJsonString

                            Log.d(TAG, "New favorites JSON (after add): $newJsonString")
                            Log.d(TAG, "Successfully saved favorite: $name")
                            Toast.makeText(layoutView.context, "已保存: $name", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving favorite: Name='$name', Lat=$lat, Lon=$lon", e)
                            Toast.makeText(layoutView.context, "保存失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        val btnViewFavorites = Button(layoutView.context).apply {
            text = "查看收藏夹"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                marginStart = 8 // Add some margin between buttons
            }
            setOnClickListener {
                // Placeholder for "View Favorites" logic
                showFavoritesDialog(layoutView.context) // Call to new function
            }
        }

        favoritesButtonsLayout.addView(btnAddFavorite)
        favoritesButtonsLayout.addView(btnViewFavorites)

        // Add the new buttons layout to the main dialog view
        // Assuming binding.root is a LinearLayout or can accommodate a new child view.
        // If binding.root is a ConstraintLayout or other complex layout, this might need adjustment
        // or the original XML layout `module_dialog_location.xml` would be the better place.
        // For now, we assume it's a ViewGroup that can add views.
        (binding.root as? LinearLayout)?.addView(favoritesButtonsLayout)
            ?: (binding.root as? android.view.ViewGroup)?.addView(favoritesButtonsLayout)


        layoutView.context.showDialog {
            title = funcName
            view = binding.root
            positiveButton("保存") { // Changed from positiveButton without text to "保存" for clarity
                ValLatitude.floatVal = binding.moduleDialogEdtLocationLatitude.text.toString().toFloat()
                ValLongitude.floatVal = binding.moduleDialogEdtLocationLongitude.text.toString().toFloat()
            }
            neutralButton("重置") {
                ValLatitude.floatVal = LATITUDE_DEF_VAL
                ValLongitude.floatVal = LONGITUDE_DEF_VAL
            }
            negativeButton("取消") // Changed from negativeButton() to "取消" for clarity
        }
    }

    // Function to show the "View Favorites" dialog
    private fun showFavoritesDialog(context: android.content.Context) {
        val dialogView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16) // Add some padding
        }

        val favoritesListLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        dialogView.addView(favoritesListLayout)
        // Initial population of the list
        _populateFavoritesList(context, favoritesListLayout)

        AlertDialog.Builder(context).apply {
            setTitle("查看收藏夹")
            setView(dialogView)
            setPositiveButton("关闭", null)
        }.show()
    }

    // Extracted function to populate the favorites list
    private fun _populateFavoritesList(context: android.content.Context, layout: LinearLayout) {
        Log.d(TAG, "Loading favorites list...")
        layout.removeAllViews() // Clear previous items

        try {
            val favoritesJson = FavoriteLocations.stringVal
            Log.d(TAG, "Raw favorites JSON from storage: $favoritesJson")
            val jsonArray = JSONArray(favoritesJson)
            val numFavorites = jsonArray.length()
            Log.d(TAG, "Number of favorites loaded: $numFavorites")

            if (numFavorites == 0) {
                val noFavoritesMsg = TextView(context).apply {
                    text = "没有收藏的地点"
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                layout.addView(noFavoritesMsg)
            } else {
                for (i in 0 until jsonArray.length()) {
                    val favoriteJson = jsonArray.getJSONObject(i)
                    val name = favoriteJson.getString("name")
                    val lat = favoriteJson.getDouble("latitude")
                    val lon = favoriteJson.getDouble("longitude")

                    val itemLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(0, 8, 0, 8) // Padding for each item
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val infoText = TextView(context).apply {
                        text = "名称: $name\nLat: $lat, Lon: $lon"
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f // Weight to take available space
                        )
                    }
                    itemLayout.addView(infoText)

                    val applyButton = Button(context).apply {
                        text = "应用"
                        setOnClickListener {
                            Log.d(TAG, "Applying favorite: Name='$name', Lat=$lat, Lon=$lon")
                            binding.moduleDialogEdtLocationLatitude.setText(lat.toString())
                            binding.moduleDialogEdtLocationLongitude.setText(lon.toString())
                            Toast.makeText(context, "已应用: $name", Toast.LENGTH_SHORT).show()
                        }
                    }
                    itemLayout.addView(applyButton)

                    val deleteButton = Button(context).apply {
                        text = "删除"
                        setOnClickListener {
                            AlertDialog.Builder(context)
                                .setTitle("确认删除")
                                .setMessage("确定要删除收藏地点 \"$name\" 吗?")
                                .setPositiveButton("删除") { _, _ ->
                                    Log.d(TAG, "Attempting to delete favorite: Name='$name', Index=$i")
                                    val oldJsonString = FavoriteLocations.stringVal
                                    Log.d(TAG, "Current favorites JSON (before delete): $oldJsonString")
                                    try {
                                        val currentFavorites = JSONArray(oldJsonString)
                                        currentFavorites.remove(i) // i is captured from the loop
                                        val newJsonString = currentFavorites.toString()
                                        FavoriteLocations.stringVal = newJsonString
                                        Log.d(TAG, "New favorites JSON (after delete): $newJsonString")
                                        Log.d(TAG, "Successfully deleted favorite: Name='$name', Index=$i")
                                        Toast.makeText(context, "已删除: $name", Toast.LENGTH_SHORT).show()
                                        _populateFavoritesList(context, layout) // Refresh the list in the current dialog
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error deleting favorite: Name='$name', Index=$i", e)
                                        Toast.makeText(context, "删除失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                    }
                    itemLayout.addView(deleteButton)
                    layout.addView(itemLayout)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading/parsing favorites JSON: $favoritesJson", e)
            Toast.makeText(context, "加载收藏夹失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            val errorMsg = TextView(context).apply {
                text = "加载收藏夹失败。"
                gravity = Gravity.CENTER
            }
            layout.addView(errorMsg)
        }
    }

    override val targetProcess = arrayOf(
        WxProcess.MAIN_PROCESS.processName,
        WxProcess.APP_BRAND_0.processName
    )
    override val isNeedRestartApp = true

    @Suppress("DEPRECATION")
    override fun initOnce() {
        RedirectUIClass.method {
            name = "onActivityResult"
            param(IntType, IntType, IntentClass)
        }.hook {
            after {
                val requestCode = args(0).cast<Int>()!!
                val resultCode = args(1).cast<Int>()!!
                if (requestCode == 6 && resultCode == Activity.RESULT_OK) {
                    val intent = args(2).cast<Intent>()!!
                    val locationIntent = intent.getParcelableExtra<Parcelable>("KLocationIntent")!!
                    val locationDataStr = locationIntent::class.java.method { returnType(StringClass) }.get(locationIntent).string()
                    val pattern = Regex("lat ([-+]?[0-9]*\\.?[0-9]+);lng ([-+]?[0-9]*\\.?[0-9]+);")
                    val match = pattern.find(locationDataStr)
                    if (match != null && match.groupValues.size == 3) {
                        binding.moduleDialogEdtLocationLatitude.setText("${match.groupValues[1].toFloatOrNull() ?: LATITUDE_DEF_VAL}")
                        binding.moduleDialogEdtLocationLongitude.setText("${match.groupValues[2].toFloatOrNull() ?: LONGITUDE_DEF_VAL}")
                    } else {
                        binding.moduleDialogEdtLocationLatitude.setText("$LATITUDE_DEF_VAL")
                        binding.moduleDialogEdtLocationLongitude.setText("$LONGITUDE_DEF_VAL")
                    }
                }
            }
        }
        listOf(MethodListener, MethodListenerWgs84, MethodDefaultManager).forEach { descData ->
            descData.toDexMethod {
                hook {
                    beforeIfEnabled {
                        args(0).any()?.also { location ->
                            location::class.java.apply {
                                method { name = "getLatitude" }.hook {
                                    beforeIfEnabled {
                                        result = ValLatitude.floatVal.toDouble()
                                    }
                                }
                                method { name = "getLongitude" }.hook {
                                    beforeIfEnabled {
                                        result = ValLongitude.floatVal.toDouble()
                                    }
                                }
                            }
                        }
                        removeSelf()
                    }
                }
            }
        }
        MethodSelectPoiMapOnClick.toDexMethod {
            hook {
                beforeIfEnabled {
                    val view = args(0).cast<View>()!!
                    AlertDialog.Builder(view.context).apply {
                        setTitle("修改经纬度")
                        setView(LinearLayout(context).apply {
                            gravity = Gravity.CENTER
                            orientation = RadioGroup.HORIZONTAL
                            addView(EditText(context).apply {
                                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                                setText("${ValLatitude.floatVal}")
                                addTextChangedListener {
                                    doAfterTextChanged {
                                        val input = it.toString().toFloatOrNull()
                                        if (input != null) {
                                            ValLatitude.floatVal = input
                                        }
                                    }
                                }
                            })
                            addView(EditText(context).apply {
                                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                                setText("${ValLongitude.floatVal}")
                                addTextChangedListener {
                                    doAfterTextChanged {
                                        val input = it.toString().toFloatOrNull()
                                        if (input != null) {
                                            ValLongitude.floatVal = input
                                        }
                                    }
                                }
                            })
                        })
                        setPositiveButton("确定", null)
                        setNegativeButton("取消", null)
                    }.show()
                }
            }
        }
    }

    override fun dexFind(dexKit: DexKitBridge) {
        MethodListener.findDexClassMethod(dexKit) {
            onMethod {
                matcher {
                    name = "onLocationChanged"
                    usingEqStrings("MicroMsg.SLocationListener")
                }
            }
        }
        MethodListenerWgs84.findDexClassMethod(dexKit) {
            onMethod {
                matcher {
                    name = "onLocationChanged"
                    usingEqStrings("MicroMsg.SLocationListenerWgs84")
                }
            }
        }
        MethodDefaultManager.findDexClassMethod(dexKit) {
            onMethod {
                matcher {
                    name = "onLocationChanged"
                    usingEqStrings("MicroMsg.DefaultTencentLocationManager", "[mlocationListener]error:%d, reason:%s")
                }
            }
        }
        MethodSelectPoiMapOnClick.findDexClassMethod(dexKit) {
            onMethod {
                matcher {
                    usingEqStrings("MicroMsg.MMPoiMapUI", "invalid lat lng")
                }
            }
        }
    }
}
