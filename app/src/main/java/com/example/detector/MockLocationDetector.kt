package com.example.detector

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.provider.Settings

data class AuditedAppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val requestsMockPermission: Boolean,
)

data class PackageAuditResult(
    val totalPackagesScanned: Int,
    val flaggedApps: List<AuditedAppInfo>,
    val allApps: List<AuditedAppInfo>,
) {
    val hasMockApps: Boolean
        get() = flaggedApps.isNotEmpty()
}

/**
 * Kotlin port & diagnostic extension of smarques84/MockLocationDetector.
 * Preserves the three canonical detection methods while surfacing structured
 * metadata for real-time inspection and package auditing.
 */
object MockLocationDetector {

    const val PERMISSION_ACCESS_MOCK_LOCATION = "android.permission.ACCESS_MOCK_LOCATION"

    /**
     * Checks whether [location] originates from a mock provider.
     * - API 31+ (Android 12+): uses [Location.isMock]
     * - API 18..30: uses [Location.isFromMockProvider]
     * - Below API 18: falls back to [isAllowMockLocationsOn]
     */
    fun isLocationFromMockProvider(context: Context, location: Location?): Boolean {
        if (location == null) return false
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> location.isMock
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 -> {
                @Suppress("DEPRECATION")
                location.isFromMockProvider
            }
            else -> isAllowMockLocationsOn(context)
        }
    }

    /**
     * Checks the legacy global mock location setting.
     * Note: Deprecated in Android 6.0 (API 23), where the system replaced the global
     * toggle with per-app selection in Developer Options. Always returns false on API 23+.
     */
    fun isAllowMockLocationsOn(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return false
        }
        @Suppress("DEPRECATION")
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ALLOW_MOCK_LOCATION,
        )
        return setting != null && setting != "0"
    }

    /**
     * Returns true if any visible installed application (other than this app itself)
     * declares [PERMISSION_ACCESS_MOCK_LOCATION] in its manifest.
     */
    fun checkForAllowMockLocationsApps(context: Context): Boolean {
        return inspectVisiblePackages(context).hasMockApps
    }

    /**
     * Inspects visible packages using Play-compliant MAIN/LAUNCHER intent queries
     * merged with installed applications returned by the OS for the current API level.
     */
    fun inspectVisiblePackages(context: Context): PackageAuditResult {
        val pm = context.packageManager
        val selfPackage = context.packageName
        val candidatePackages = linkedSetOf<String>()

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
        resolveInfos.forEach { info ->
            info.activityInfo?.packageName?.let { pkg ->
                if (pkg != selfPackage) candidatePackages.add(pkg)
            }
        }

        val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }
        installedApps.forEach { appInfo ->
            if (appInfo.packageName != selfPackage) {
                candidatePackages.add(appInfo.packageName)
            }
        }

        val audited = candidatePackages.mapNotNull { packageName ->
            runCatching {
                val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                }
                val appInfo = packageInfo.applicationInfo
                val label = appInfo?.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
                    ?: packageName
                val isSystem = appInfo != null &&
                    ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                val requested = packageInfo.requestedPermissions ?: emptyArray()
                val requestsMock = requested.any { it == PERMISSION_ACCESS_MOCK_LOCATION }

                AuditedAppInfo(
                    appName = label,
                    packageName = packageName,
                    versionName = packageInfo.versionName ?: "—",
                    isSystemApp = isSystem,
                    requestsMockPermission = requestsMock,
                )
            }.getOrNull()
        }.sortedWith(
            compareByDescending<AuditedAppInfo> { it.requestsMockPermission }
                .thenBy { it.isSystemApp }
                .thenBy { it.appName.lowercase() },
        )

        val flagged = audited.filter { it.requestsMockPermission }
        return PackageAuditResult(
            totalPackagesScanned = audited.size,
            flaggedApps = flagged,
            allApps = audited,
        )
    }
}
