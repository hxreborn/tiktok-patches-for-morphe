package app.morphe.patches.tiktok.misc.translation

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object TranslationKevaReadFingerprint : Fingerprint(
    definingClass = "/TranslationKevaServiceImpl;",
    name = "LIZIZ",
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("key_one_click_translation_active"),
)

internal object TranslationKevaWriteFingerprint : Fingerprint(
    definingClass = "/TranslationKevaServiceImpl;",
    name = "LJIIIIZZ",
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("key_one_click_translation_active"),
)

internal object TranslationKevaClearFingerprint : Fingerprint(
    definingClass = "/TranslationKevaServiceImpl;",
    name = "LIZLLL",
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("key_one_click_translation_active"),
)

internal object CommentTranslationReevaluateFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    strings = listOf(
        "reEvaluateTranslationState requestType:",
        ", shouldTranslate:",
        ", overrideStatus:",
    ),
)

internal object CommentAllToggleEmit25Fingerprint : Fingerprint(
    definingClass = "/AgS243S0100000_11;",
    name = "emit\$25",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("LY/AgS243S0100000_11;", "Ljava/lang/Object;", "LX/02vj;"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.name == "COMMENT_ALL_TOGGLE"
        } == true
    },
)

internal object CommentAllToggleEmit26Fingerprint : Fingerprint(
    definingClass = "/AgS243S0100000_11;",
    name = "emit\$26",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("LY/AgS243S0100000_11;", "Ljava/lang/Object;", "LX/02vj;"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.name == "COMMENT_ALL_TOGGLE"
        } == true
    },
)

internal object CommentTranslationStateEligibilityFingerprint : Fingerprint(
    definingClass = "/AgS243S0100000_11;",
    name = "emit\$37",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("LY/AgS243S0100000_11;", "Ljava/lang/Object;", "LX/02vj;"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<MethodReference>()?.let { reference ->
                reference.name == "setTranslationState" ||
                    (reference.parameterTypes == listOf("Lcom/ss/android/ugc/aweme/comment/model/Comment;") &&
                        reference.returnType == "Z")
            } == true
        } == true
    },
)

internal object TranslationServiceCommentTranslatableFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJIIJ",
    returnType = "Z",
    parameters = listOf("Lcom/ss/android/ugc/aweme/comment/model/Comment;"),
)

internal object TranslationServiceCommentModeEligibleFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJIILIIL",
    returnType = "Z",
    parameters = listOf(
        "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
        "LX/0Plo;",
    ),
)

internal object TranslationServiceCommentLanguageAllowedFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJJIIJZLJL",
    returnType = "Z",
    parameters = listOf("Lcom/ss/android/ugc/aweme/comment/model/Comment;"),
)

internal object TranslationServiceSeeTranslationClickFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJJJJLI",
    returnType = "V",
    parameters = listOf(
        "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
        "Z",
    ),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.name == "TRIGGER_BY_CLICK_SEE_TRANSLATION_SOMETIMES"
        } == true
    },
)

internal object TranslationServiceSingleStringRequestFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJIIIZ",
    returnType = "LX/0x7U;",
    parameters = listOf("Ljava/lang/String;"),
)

internal object TranslationServiceMultipleStringRequestFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJIIIIZZ",
    returnType = "LX/0x7U;",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/List;"),
)

internal object TranslationServiceAwemeTranslationRequestFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJIIJJI",
    returnType = "LX/0x7U;",
    parameters = listOf(
        "J",
        "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
)

internal object TranslationServiceFutureRequestFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LLIIII",
    returnType = "Lcom/google/common/util/concurrent/ListenableFuture;",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
)

internal object TranslationServiceModeStateWriteFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJI",
    returnType = "V",
    parameters = listOf("LX/0Plo;", "Ljava/lang/String;"),
)

internal object TranslationServiceModeStateBroadcastFingerprint : Fingerprint(
    definingClass = "/TranslationServiceImpl;",
    name = "LJJJLZIJ",
    returnType = "V",
    parameters = listOf("LX/0Plo;", "Ljava/lang/String;"),
)

internal object MultiCommentTranslationStartFingerprint : Fingerprint(
    definingClass = "LX/0Pwq;",
    name = "LJFF",
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "LX/0QML;", "Z"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<MethodReference>()?.let { reference ->
                reference.definingClass == "Lcom/ss/android/ugc/aweme/comment/translation/CommentMultiTranslationApi\$RealApi;" &&
                    reference.name == "getMultiTranslation"
            } == true
        } == true
    },
)

internal object BaseCommentCellBindFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
    name = "g7",
    returnType = "V",
    parameters = listOf("LX/0srE;"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.let { reference ->
                reference.definingClass == "LX/0QMJ;" &&
                    reference.name == "LLILZ" &&
                    reference.type == "LX/0QML;"
            } == true
        } == true
    },
)

internal object CommentListLoadedFingerprint : Fingerprint(
    definingClass = "LX/0sqp;",
    name = "LJIJJLI",
    returnType = "V",
    parameters = listOf(
        "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;",
        "Z",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "LX/0NJN;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "LX/02vj;",
        "I",
        "I",
    ),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.let { reference ->
                reference.definingClass == "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;" &&
                    reference.name == "items" &&
                    reference.type == "Ljava/util/List;"
            } == true
        } == true
    },
)

internal object MultiCommentTranslationCompleteFingerprint : Fingerprint(
    definingClass = "LX/0QfV;",
    name = "run\$2",
    returnType = "V",
    parameters = listOf("LX/0QfV;"),
    strings = listOf("MultiCommentTranslationTask startTranslate onComplete "),
)

internal object MultiCommentTranslationCacheCopyFingerprint : Fingerprint(
    definingClass = "LX/0Pwq;",
    name = "LJI",
    returnType = "V",
    parameters = listOf(
        "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
        "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
        "Z",
    ),
)

internal object MultiCommentTranslationStateFingerprint : Fingerprint(
    definingClass = "LX/0Pwq;",
    name = "LJ",
    returnType = "V",
    parameters = listOf(
        "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
        "Z",
    ),
)

internal object CommentSetTextFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
    name = "setText",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object CommentSetOriginalTextFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
    name = "setOriginalText",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object CommentSetTranslatedTextFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
    name = "setTranslatedText",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object CommentSetTranslatedFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
    name = "setTranslated",
    returnType = "V",
    parameters = listOf("Z"),
)

internal object CommentSetTranslateVariantFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
    name = "setTranslateVariant",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object CommentSetTransBtnStyleFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
    name = "setTransBtnStyle",
    returnType = "V",
    parameters = listOf("I"),
)
