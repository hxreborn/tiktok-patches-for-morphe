package app.morphe.patches.tiktok.misc.translation

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/translation/CommentBatchTranslator;"

@Suppress("unused")
val commentTranslationPatch = bytecodePatch(
    name = "Auto Translate comments",
    description = "Adds Auto comment translation controls that translates all non default langauge comments that are loaded. Supports TikTok 43.8.3.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(*AppCompatibilities.tiktok4383())

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableCommentTranslation()V",
        )

        BaseCommentCellBindFingerprint.method.apply {
            val managerReadyIndex = implementation!!.instructions.withIndex()
                .firstOrNull { (_, instruction) ->
                    instruction.getReference<FieldReference>()?.let { reference ->
                        reference.definingClass == "LX/0QMJ;" &&
                            reference.name == "LLILLJJLI" &&
                            reference.type == "LX/0QMK;"
                    } == true
                }?.index ?: throw PatchException(
                "Translate comments: could not locate bound comment translation manager.",
            )

            addInstructions(
                managerReadyIndex + 1,
                """
                    iget-object v0, v6, Landroidx/recyclerview/widget/RecyclerView${'$'}ViewHolder;->itemView:Landroid/view/View;
                    invoke-static {v0, v5}, $EXTENSION_CLASS_DESCRIPTOR->registerCommentCell(Landroid/view/View;Ljava/lang/Object;)V
                """,
            )
        }

        CommentListLoadedFingerprint.method.apply {
            val responseReadyIndex = implementation!!.instructions.withIndex()
                .firstOrNull { (_, instruction) ->
                    instruction.getReference<FieldReference>()?.let { reference ->
                        reference.definingClass == "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;" &&
                            reference.name == "lazySplitItemsParseTask"
                    } == true
                }?.index ?: throw PatchException(
                "Translate comments: could not locate loaded comment list response.",
            )

            addInstruction(
                responseReadyIndex,
                "invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->onCommentListLoaded(Ljava/lang/Object;)V",
            )
        }

        MultiCommentTranslationStartFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {v16 .. v18}, $EXTENSION_CLASS_DESCRIPTOR->onNativeBatchStart(Ljava/lang/Object;Ljava/lang/Object;Z)V
            """,
        )

        MultiCommentTranslationCompleteFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $EXTENSION_CLASS_DESCRIPTOR->onNativeBatchComplete(Ljava/lang/Object;)V
            """,
        )
    }
}
