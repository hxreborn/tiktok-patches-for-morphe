/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/telemetry/DisableTelemetryPatch.kt
 */
package app.morphe.patches.tiktok.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Disables ByteDance AppLog analytics, AppsFlyer attribution tracking, " +
        "BDLocation background uploads, Firebase Analytics, and crash reporting. " +
        "(Supports TikTok 43.8.3.)",
) {
    compatibleWith(*AppCompatibilities.tiktok4383())

    execute {
        listOf(
            AppLogInitFingerprint,
            AppLogStartFingerprint,
            AppLogOnEventFingerprint,
            AppLogOnEventV3StringFingerprint,
            AppLogOnEventV3JsonFingerprint,
            AppLogOnEventV3BundleFingerprint,
            AppLogOnMiscEventFingerprint,
            AppLogFlushFingerprint,
            AppLogFlushAsyncFingerprint,
            AppLogOnActivityPauseFingerprint,
        ).forEach { fingerprint ->
            fingerprint.method.returnEarly()
        }

        InitAppsFlyerRunFingerprint.method.returnEarly()

        // AppsFlyer declares its API methods as abstract, so patch the singleton's concrete overrides.
        val appsFlyerImplementationClass = AppsFlyerGetInstanceFingerprint.method.implementation
            ?.instructions
            ?.firstNotNullOfOrNull { instruction -> instruction.getReference<MethodReference>() }
            ?.definingClass
            ?: throw PatchException("Could not resolve the AppsFlyer implementation class")
        val appsFlyerClass = mutableClassDefBy(appsFlyerImplementationClass)

        listOf(
            AppsFlyerLogEventFingerprint,
            AppsFlyerLogLocationFingerprint,
        ).forEach { fingerprint ->
            appsFlyerClass.findMutableMethodOf(fingerprint.originalMethod).returnEarly()
        }

        // This SDK is not bundled in the 43.8.3 global APK, but some package variants may include it.
        BDLocationSetUploadFingerprint.methodOrNull?.addInstructions(
            0,
            """
                const/4 p0, 0x0
                sput-boolean p0, Lcom/bytedance/bdlocation/client/BDLocationConfig;->sIsUpload:Z
                sput-boolean p0, Lcom/bytedance/bdlocation/client/BDLocationConfig;->sIsUploadGPS:Z
                sput-boolean p0, Lcom/bytedance/bdlocation/client/BDLocationConfig;->sIsUploadLocation:Z
                sput-boolean p0, Lcom/bytedance/bdlocation/client/BDLocationConfig;->sIsUploadBaseSite:Z
                sput-boolean p0, Lcom/bytedance/bdlocation/client/BDLocationConfig;->sIsUploadWIFI:Z
                sput-boolean p0, Lcom/bytedance/bdlocation/client/BDLocationConfig;->sUploadMccAndSystemRegionInfo:Z
                return-void
            """,
        )

        listOf(
            BDLocationIsUploadFingerprint,
            BDLocationIsUploadGPSFingerprint,
            BDLocationIsUploadLocationFingerprint,
        ).forEach { fingerprint ->
            fingerprint.methodOrNull?.returnEarly(false)
        }

        FirebaseSetCurrentScreenFingerprint.method.returnEarly()
        MonitorCrashReportCustomErrFingerprint.method.returnEarly()
        MonitorCrashReportEventFingerprint.method.returnEarly()
    }
}
