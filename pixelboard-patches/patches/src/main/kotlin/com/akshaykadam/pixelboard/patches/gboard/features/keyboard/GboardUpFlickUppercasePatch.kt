package com.akshaykadam.pixelboard.patches.gboard.features.keyboard

import com.akshaykadam.pixelboard.patches.gboard.shared.addHelperMethodIfMissing
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.mutableClass
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags

private const val SOFT_KEY_VIEW_CLASS =
    "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;"
private const val SOFT_KEY_DEF_CLASS =
    "Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;"
private const val ACTION_DEF_CLASS =
    "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;"
private const val ACTION_TYPE_CLASS = "Lpuc;"
private const val ACTION_DEF_BUILDER_CLASS = "Lpud;"
private const val SOFT_KEY_DEF_BUILDER_CLASS = "Lpwt;"
private const val KEY_DATA_CLASS = "Lpva;"
private const val KEY_DATA_STYLE_CLASS = "Lpuz;"
private const val IMMUTABLE_LIST_CLASS = "Lwhe;"
private const val PATCH_METADATA_METHOD = "pixelboardPatchUpFlickMetadata"

internal val gboardUpFlickUppercaseBytecodePatch = bytecodePatch(
    description = "Inject up-flick uppercase/lowercase toggle into SoftKeyView for Gboard 18.6.3.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val softKeyView = mutableClass(SOFT_KEY_VIEW_CLASS)
        val bindMethod = softKeyView.methods.firstOrNull { method ->
            method.name == "s" &&
                method.parameterTypes == listOf(SOFT_KEY_DEF_CLASS, "J") &&
                method.returnType == "Z" &&
                method.implementation != null
        } ?: error("Could not find $SOFT_KEY_VIEW_CLASS->s(${SOFT_KEY_DEF_CLASS}J)Z")

        addHelperMethodIfMissing(
            classType = SOFT_KEY_VIEW_CLASS,
            name = "pixelboardToggleAsciiCase",
            parameterTypes = listOf("Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
            accessFlags = AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            registerCount = 3,
            body = TOGGLE_ASCII_CASE_BODY,
        )
        addHelperMethodIfMissing(
            classType = SOFT_KEY_VIEW_CLASS,
            name = "pixelboardResolvePrimaryLabel",
            parameterTypes = listOf(SOFT_KEY_DEF_CLASS),
            returnType = "Ljava/lang/String;",
            accessFlags = AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            registerCount = 6,
            body = RESOLVE_PRIMARY_LABEL_BODY,
        )
        addHelperMethodIfMissing(
            classType = SOFT_KEY_VIEW_CLASS,
            name = "pixelboardResolvePressPayload",
            parameterTypes = listOf(SOFT_KEY_DEF_CLASS),
            returnType = "Ljava/lang/String;",
            accessFlags = AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            registerCount = 7,
            body = RESOLVE_PRESS_PAYLOAD_BODY,
        )
        addHelperMethodIfMissing(
            classType = SOFT_KEY_VIEW_CLASS,
            name = PATCH_METADATA_METHOD,
            parameterTypes = listOf(SOFT_KEY_DEF_CLASS),
            returnType = SOFT_KEY_DEF_CLASS,
            accessFlags = AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
            registerCount = 14,
            body = PATCH_INCOMING_METADATA_BODY,
        )

        val patchDescriptor = "$SOFT_KEY_VIEW_CLASS->$PATCH_METADATA_METHOD($SOFT_KEY_DEF_CLASS)$SOFT_KEY_DEF_CLASS"
        if (bindMethod.implementation!!.instructions.none { it.isMethodReference(patchDescriptor) }) {
            bindMethod.addInstructions(
                0,
                """
                    invoke-direct {p0, p1}, $patchDescriptor
                    move-result-object p1
                    check-cast p1, $SOFT_KEY_DEF_CLASS
                """.trimIndent(),
            )
        }
    }
}

private val TOGGLE_ASCII_CASE_BODY = """
    if-eqz p0, :cond_invalid
    invoke-virtual {p0}, Ljava/lang/String;->length()I
    move-result v0
    const/4 v1, 0x1
    if-ne v0, v1, :cond_invalid
    const/4 v0, 0x0
    invoke-virtual {p0, v0}, Ljava/lang/String;->charAt(I)C
    move-result v0
    const/16 v1, 0x61
    if-lt v0, v1, :cond_uppercase
    const/16 v1, 0x7a
    if-gt v0, v1, :cond_uppercase
    add-int/lit8 v0, v0, -0x20
    int-to-char v0, v0
    invoke-static {v0}, Ljava/lang/String;->valueOf(C)Ljava/lang/String;
    move-result-object p0
    return-object p0
    :cond_uppercase
    const/16 v1, 0x41
    if-lt v0, v1, :cond_invalid
    const/16 v1, 0x5a
    if-gt v0, v1, :cond_invalid
    add-int/lit8 v0, v0, 0x20
    int-to-char v0, v0
    invoke-static {v0}, Ljava/lang/String;->valueOf(C)Ljava/lang/String;
    move-result-object p0
    return-object p0
    :cond_invalid
    const/4 p0, 0x0
    return-object p0
""".trimIndent()

private val RESOLVE_PRIMARY_LABEL_BODY = """
    if-eqz p0, :cond_none
    iget-object p0, p0, $SOFT_KEY_DEF_CLASS->i:$IMMUTABLE_LIST_CLASS
    if-eqz p0, :cond_none
    invoke-interface {p0}, Ljava/util/List;->size()I
    move-result v0
    const/4 v1, 0x0
    :goto_0
    if-ge v1, v0, :cond_none
    invoke-interface {p0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;
    move-result-object v2
    if-eqz v2, :cond_next
    invoke-virtual {v2}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v2
    if-eqz v2, :cond_next
    invoke-virtual {v2}, Ljava/lang/String;->length()I
    move-result v3
    if-lez v3, :cond_next
    return-object v2
    :cond_next
    add-int/lit8 v1, v1, 0x1
    goto :goto_0
    :cond_none
    const/4 p0, 0x0
    return-object p0
""".trimIndent()

private val RESOLVE_PRESS_PAYLOAD_BODY = """
    if-eqz p0, :cond_none
    sget-object v0, $ACTION_TYPE_CLASS->a:$ACTION_TYPE_CLASS
    invoke-virtual {p0, v0}, $SOFT_KEY_DEF_CLASS->g($ACTION_TYPE_CLASS)$ACTION_DEF_CLASS
    move-result-object v0
    if-eqz v0, :cond_none
    iget-object v0, v0, $ACTION_DEF_CLASS->f:$IMMUTABLE_LIST_CLASS
    if-eqz v0, :cond_none
    invoke-interface {v0}, Ljava/util/List;->size()I
    move-result v1
    const/4 v2, 0x0
    :goto_0
    if-ge v2, v1, :cond_none
    invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;
    move-result-object v3
    check-cast v3, $KEY_DATA_CLASS
    if-eqz v3, :cond_next
    iget-object v3, v3, $KEY_DATA_CLASS->e:Ljava/lang/Object;
    instance-of v4, v3, Ljava/lang/CharSequence;
    if-eqz v4, :cond_next
    check-cast v3, Ljava/lang/CharSequence;
    invoke-interface {v3}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;
    move-result-object v3
    if-eqz v3, :cond_next
    invoke-virtual {v3}, Ljava/lang/String;->length()I
    move-result v4
    if-lez v4, :cond_next
    return-object v3
    :cond_next
    add-int/lit8 v2, v2, 0x1
    goto :goto_0
    :cond_none
    const/4 p0, 0x0
    return-object p0
""".trimIndent()

private val PATCH_INCOMING_METADATA_BODY = """
    if-eqz p1, :cond_return_original
    ${RuntimeCallEmitter.invoke(RuntimeCallId.ENGLISH_UPPERCASE_TOGGLE_RUNTIME_IS_ENABLED, "")}
    move-result v0
    if-eqz v0, :cond_return_original
    ${RuntimeCallEmitter.invoke(RuntimeCallId.ENGLISH_UPPERCASE_TOGGLE_RUNTIME_IS_PATCHED_METADATA, "p1")}
    move-result v0
    if-nez v0, :cond_return_original
    sget-object v0, $ACTION_TYPE_CLASS->c:$ACTION_TYPE_CLASS
    invoke-virtual {p1, v0}, $SOFT_KEY_DEF_CLASS->g($ACTION_TYPE_CLASS)$ACTION_DEF_CLASS
    move-result-object v0
    if-nez v0, :cond_return_original
    ${RuntimeCallEmitter.invoke(RuntimeCallId.ENGLISH_UPPERCASE_TOGGLE_RUNTIME_GET_CACHED_PATCHED_METADATA, "p1")}
    move-result-object v0
    if-eqz v0, :cond_cache_miss
    check-cast v0, $SOFT_KEY_DEF_CLASS
    return-object v0
    :cond_cache_miss
    invoke-static {p1}, $SOFT_KEY_VIEW_CLASS->pixelboardResolvePrimaryLabel($SOFT_KEY_DEF_CLASS)Ljava/lang/String;
    move-result-object v1
    invoke-static {p1}, $SOFT_KEY_VIEW_CLASS->pixelboardResolvePressPayload($SOFT_KEY_DEF_CLASS)Ljava/lang/String;
    move-result-object v2
    if-nez v1, :cond_have_base_label
    move-object v1, v2
    :cond_have_base_label
    invoke-static {v1}, $SOFT_KEY_VIEW_CLASS->pixelboardToggleAsciiCase(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v3
    if-eqz v3, :cond_return_original
    if-eqz v2, :cond_build_action
    invoke-static {v2}, $SOFT_KEY_VIEW_CLASS->pixelboardToggleAsciiCase(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :cond_return_original
    :cond_build_action
    new-instance v4, $SOFT_KEY_DEF_BUILDER_CLASS
    invoke-direct {v4}, $SOFT_KEY_DEF_BUILDER_CLASS-><init>()V
    invoke-virtual {v4, p1}, $SOFT_KEY_DEF_BUILDER_CLASS->k($SOFT_KEY_DEF_CLASS)V
    new-instance v5, $ACTION_DEF_BUILDER_CLASS
    invoke-direct {v5}, $ACTION_DEF_BUILDER_CLASS-><init>()V
    sget-object v6, $ACTION_TYPE_CLASS->c:$ACTION_TYPE_CLASS
    iput-object v6, v5, $ACTION_DEF_BUILDER_CLASS->a:$ACTION_TYPE_CLASS
    const/4 v6, 0x1
    new-array v7, v6, [Ljava/lang/String;
    const/4 v6, 0x0
    aput-object v3, v7, v6
    invoke-virtual {v5, v7}, $ACTION_DEF_BUILDER_CLASS->t([Ljava/lang/String;)V
    const v7, -0x2719
    sget-object v8, $KEY_DATA_STYLE_CLASS->c:$KEY_DATA_STYLE_CLASS
    invoke-virtual {v5, v7, v8, v3}, $ACTION_DEF_BUILDER_CLASS->s(I${KEY_DATA_STYLE_CLASS}Ljava/lang/Object;)V
    invoke-virtual {v5}, $ACTION_DEF_BUILDER_CLASS->c()$ACTION_DEF_CLASS
    move-result-object v5
    if-eqz v5, :cond_return_original
    invoke-virtual {v4, v5}, $SOFT_KEY_DEF_BUILDER_CLASS->t($ACTION_DEF_CLASS)V
    invoke-virtual {v4}, $SOFT_KEY_DEF_BUILDER_CLASS->b()$SOFT_KEY_DEF_CLASS
    move-result-object v0
    check-cast v0, $SOFT_KEY_DEF_CLASS
    if-eqz v0, :cond_return_original
    ${RuntimeCallEmitter.invoke(RuntimeCallId.ENGLISH_UPPERCASE_TOGGLE_RUNTIME_CACHE_PATCHED_METADATA, "p1, v0")}
    move-result-object v0
    check-cast v0, $SOFT_KEY_DEF_CLASS
    if-eqz v0, :cond_return_original
    return-object v0
    :cond_return_original
    return-object p1
""".trimIndent()
