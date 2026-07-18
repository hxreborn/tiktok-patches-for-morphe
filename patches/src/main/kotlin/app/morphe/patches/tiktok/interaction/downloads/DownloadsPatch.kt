/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/interaction/downloads/DownloadsPatch.kt
 */
package app.morphe.patches.tiktok.interaction.downloads

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/download/DownloadsPatch;"
private const val STICKER_EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/download/StickerGallerySaver;"

@Suppress("unused")
val downloadsPatch = bytecodePatch(
    name = "Downloads",
    description = "Downloads videos and images without watermark, saves comment stickers/images, and adds download-related controls. (Supports TikTok 43.8.3.)",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(*AppCompatibilities.tiktok4383())

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableDownload()V",
        )

        AclCommonShareFingerprint.method.returnEarly(0)
        AclCommonShare2Fingerprint.method.returnEarly(2)

        // Download videos without watermark.
        AclCommonShare3Fingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldRemoveWatermark()Z
                move-result v0
                if-eqz v0, :noremovewatermark
                const/4 v0, 0x1
                return v0
                :noremovewatermark
                nop
            """,
        )

        AwemeGetVideoFingerprint.method.apply {
            val returnIndex = findInstructionIndicesReversedOrThrow { opcode == Opcode.RETURN_OBJECT }.first()
            val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructions(
                returnIndex,
                """
                    invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->patchVideoObject(Lcom/ss/android/ugc/aweme/feed/model/Video;)V
                """,
            )
        }

        // Download images without TikTok's drawn watermark.
        CommentImageWatermarkFingerprint.method.apply {
            val drawBitmapIndex = findInstructionIndicesReversedOrThrow {
                opcode.name == "invoke-virtual" &&
                    this is ReferenceInstruction &&
                    reference.toString().contains("->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V")
            }.first()

            val drawInstr = getInstruction<FiveRegisterInstruction>(drawBitmapIndex)
            val canvasReg = drawInstr.registerC
            val bitmapReg = drawInstr.registerD
            val xReg = drawInstr.registerE
            val yReg = drawInstr.registerF
            val paintReg = drawInstr.registerG

            removeInstructions(drawBitmapIndex, 1)

            addInstructionsWithLabels(
                drawBitmapIndex,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldRemoveWatermark()Z
                    move-result v$xReg

                    if-nez v$xReg, :skip_watermark

                    const/4 v$xReg, 0x0
                    invoke-virtual {v$canvasReg, v$bitmapReg, v$xReg, v$yReg, v$paintReg}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V

                    :skip_watermark
                    nop
                """,
            )
        }

        // Add local gallery saving to the comment sticker/image preview sheet.
        StickerPreviewBinderFingerprint.method.apply {
            val returnIndex = findInstructionIndicesReversedOrThrow { opcode == Opcode.RETURN_VOID }.first()
            addInstructions(
                returnIndex,
                """
                    invoke-static/range {p0 .. p1}, $STICKER_EXTENSION_CLASS_DESCRIPTOR->attachSaveImageButton(Landroid/view/View;Ljava/lang/Object;)V
                """,
            )
        }

        // Change the download path: video and image posts build their save URI in separate methods,
        // so rewrite each to read its own setting.
        DownloadUriFingerprint.method.redirectDownloadPath("getDownloadPath")
        ImageDownloadUriFingerprint.method.redirectDownloadPath("getImageDownloadPath")
    }
}

// Replace every 'Environment.DIRECTORY_* + "/Camera/"' concatenation in a URI builder with a call to
// the given extension method, reusing whichever registers the original append already used.
private fun MutableMethod.redirectDownloadPath(extensionMethodName: String) {
    findInstructionIndicesReversedOrThrow {
        getReference<FieldReference>().let { ref ->
            ref?.definingClass == "Landroid/os/Environment;" && ref.name.startsWith("DIRECTORY_")
        }
    }.forEach { fieldIndex ->
        val pathRegister = getInstruction<OneRegisterInstruction>(fieldIndex).registerA
        val builderRegister = getInstruction<FiveRegisterInstruction>(fieldIndex + 1).registerC

        // Remove 'field load → append → "/Camera/" → append' block.
        removeInstructions(fieldIndex, 4)

        addInstructions(
            fieldIndex,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->$extensionMethodName()Ljava/lang/String;
                move-result-object v$pathRegister
                invoke-virtual { v$builderRegister, v$pathRegister }, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            """,
        )
    }
}

