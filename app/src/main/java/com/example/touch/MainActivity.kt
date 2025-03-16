package com.example.touch

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_ACCESSIBILITY_PERMISSION = 1002
    }

    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var btnAccessibilitySettings: Button
    private lateinit var btnOverlayPermission: Button
    private lateinit var btnOpenTestActivity: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnStartService = findViewById(R.id.btn_start_service)
        btnStopService = findViewById(R.id.btn_stop_service)
        btnAccessibilitySettings = findViewById(R.id.btn_accessibility_settings)
        btnOverlayPermission = findViewById(R.id.btn_overlay_permission)
        btnOpenTestActivity = findViewById(R.id.btn_open_test_activity)
    }

    private fun setupListeners() {
        btnStartService.setOnClickListener { startFloatingButtonService() }
        btnStopService.setOnClickListener { stopFloatingButtonService() }
        btnAccessibilitySettings.setOnClickListener { openAccessibilitySettings() }
        btnOverlayPermission.setOnClickListener { requestOverlayPermission() }
        btnOpenTestActivity.setOnClickListener { openTestActivity() }
    }

    override fun onResume() {
        super.onResume()
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val isAccessibilityServiceEnabled = isAccessibilityServiceEnabled()
        val hasOverlayPermission = hasOverlayPermission()

        btnStartService.isEnabled = isAccessibilityServiceEnabled && hasOverlayPermission
        btnStopService.isEnabled = isAccessibilityServiceEnabled && hasOverlayPermission
        btnAccessibilitySettings.isEnabled = !isAccessibilityServiceEnabled
        btnOverlayPermission.isEnabled = !hasOverlayPermission
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // 在Android 6.0以下，不需要动态申请悬浮窗权限
        }
    }

    private fun startFloatingButtonService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        val intent = Intent(this, FloatingButtonService::class.java)
        startService(intent)
    }

    private fun stopFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        stopService(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, REQUEST_ACCESSIBILITY_PERMISSION)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }

    private fun openTestActivity() {
        val intent = Intent(this, GestureTestActivity::class.java)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION || requestCode == REQUEST_ACCESSIBILITY_PERMISSION) {
            updateButtonStates()
        }
    }
} 