/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/share/SanitizeShareUrlsPatch.kt
 */
package app.morphe.patches.tiktok.misc.share

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/share/ShareUrlSanitizer;"

@Suppress("unused")
val sanitizeShareUrlsPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes tracking parameters from shared links. (Supports TikTok 43.8.3.)",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(*AppCompatibilities.tiktok4383())

    execute {
        ShareUrlShorteningFingerprint.method.apply {
            val longUrlRegister = implementation!!.registerCount - 6 + 3
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldSanitize()Z
                    move-result v0
                    if-eqz v0, :morphe_skip_multi_share_url_sanitization

                    invoke-static/range { v$longUrlRegister .. v$longUrlRegister }, $EXTENSION_CLASS_DESCRIPTOR->sanitizeShareUrl(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$longUrlRegister
                """,
                ExternalLabel("morphe_skip_multi_share_url_sanitization", getInstruction(0)),
            )
        }

        SingleShareUrlShorteningFingerprint.method.apply {
            val firstParameterRegister = implementation!!.registerCount - parameterTypes.size
            val itemTypeRegister = firstParameterRegister + 1
            val sourceRegister = firstParameterRegister + 2
            val inputUrlRegister = firstParameterRegister + 3
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { v$itemTypeRegister .. v$inputUrlRegister }, $EXTENSION_CLASS_DESCRIPTOR->getSanitizedShorteningUrlOrNull(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :morphe_continue_single_share_url_shortening

                    new-instance v1, LX/0x51;
                    invoke-direct {v1, v0}, LX/0x51;-><init>(Ljava/lang/Object;)V
                    return-object v1
                """,
                ExternalLabel("morphe_continue_single_share_url_shortening", getInstruction(0)),
            )
        }

        ClipboardSetPrimaryClipFingerprint.method.apply {
            val clipDataRegister = implementation!!.registerCount - parameterTypes.size + 1
            addInstructions(
                0,
                """
                    invoke-static {v$clipDataRegister}, $EXTENSION_CLASS_DESCRIPTOR->sanitizeClipboardClipDataIfNeeded(Landroid/content/ClipData;)Landroid/content/ClipData;
                    move-result-object v$clipDataRegister
                """,
            )
        }

        SingleShareShortenResultFingerprint.method.apply {
            val shortenUrlIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IGET_OBJECT &&
                    getReference<FieldReference>()?.let { field ->
                        field.definingClass == "Lcom/ss/android/ugc/aweme/share/model/ShortenModel;" &&
                            field.name == "shortenUrl" &&
                            field.type == "Ljava/lang/String;"
                    } == true
            }
            val urlRegister = getInstruction<Instruction22c>(shortenUrlIndex).registerA

            addInstructions(
                shortenUrlIndex + 1,
                """
                    invoke-static {v$urlRegister}, $EXTENSION_CLASS_DESCRIPTOR->sanitizeShareUrlIfEnabled(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$urlRegister
                """,
            )
        }
    }
}

