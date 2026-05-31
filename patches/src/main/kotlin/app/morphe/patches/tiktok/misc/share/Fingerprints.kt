/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/share/Fingerprints.kt
 */
package app.morphe.patches.tiktok.misc.share

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object ShareUrlShorteningFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/lang/String;",
        "Ljava/util/List;",
        "Ljava/lang/String;",
        "Z",
        "I",
    ),
    strings = listOf("share_link_id", "invitation_scene"),
    custom = { method, _ ->
        method.parameterTypes.size == 6
    },
)

internal object SingleShareShortenResultFingerprint : Fingerprint(
    name = "apply",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            if (instruction.opcode != Opcode.IGET_OBJECT) return@any false

            val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                ?: return@any false

            field.definingClass == "Lcom/ss/android/ugc/aweme/share/model/ShortenModel;" &&
                field.name == "shortenUrl" &&
                field.type == "Ljava/lang/String;"
        } == true
    },
)

internal object SingleShareUrlShorteningFingerprint : Fingerprint(
    name = "LJ",
    returnType = "LX/",
    parameters = listOf(
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
    strings = listOf("share_link_id", "invitation_scene", "shorten_network_timeout_experiment"),
    custom = { method, _ ->
        method.parameterTypes.size == 4
    },
)

internal object ClipboardSetPrimaryClipFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Landroid/content/ClipboardManager;",
        "Landroid/content/ClipData;",
        "Lcom/bytedance/bpea/basics/Cert;",
    ),
    strings = listOf(
        "clipboard_setClip",
        "clipData",
        "setPrimaryClip(Landroid/content/ClipData;)V",
    ),
)
