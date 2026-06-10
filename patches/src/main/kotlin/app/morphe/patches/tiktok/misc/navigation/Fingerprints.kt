package app.morphe.patches.tiktok.misc.navigation

import app.morphe.patcher.Fingerprint

internal object HomeTabAbilityListFingerprint : Fingerprint(
    definingClass = "/TabAbilityAssem;",
    name = "eT1",
    returnType = "Ljava/util/List;",
    parameters = listOf("Z"),
)
