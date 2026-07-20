package app.morphe.patches.tiktok.misc.navigation

import app.morphe.patcher.Fingerprint

internal object HomeTabAbilityListFingerprint : Fingerprint(
    definingClass = "/TabAbilityAssem;",
    name = "eT1",
    returnType = "Ljava/util/List;",
    parameters = listOf("Z"),
)

internal object BottomTabBuildListFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
    strings = listOf("HOME", "PUBLISH", "FRIENDS_TAB", "SHOP_MALL", "NOTIFICATION"),
)
