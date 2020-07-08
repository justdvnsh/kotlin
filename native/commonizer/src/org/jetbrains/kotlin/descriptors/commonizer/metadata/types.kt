/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.name.ClassId

// TODO: intern types
internal fun CirType.buildType(targetContext: TargetVisitingContext, expandTypeAliases: Boolean = true): KmType {
    return when (this) {
        is CirFlexibleType -> {
            lowerBound.buildType(targetContext, expandTypeAliases).also {
                it.flexibleTypeUpperBound = KmFlexibleTypeUpperBound(
                    type = upperBound.buildType(targetContext, expandTypeAliases),
                    typeFlexibilityId = DynamicTypeDeserializer.id
                )
            }
        }
        is CirSimpleType -> buildType(targetContext, expandTypeAliases)
    }
}

internal fun CirSimpleType.buildType(targetContext: TargetVisitingContext, expandTypeAliases: Boolean): KmType {
    return when (val classifierId = classifierId) {
        is CirClassifierId.Class -> buildClassType(targetContext, expandTypeAliases, classifierId.classId)
        is CirClassifierId.TypeAlias -> buildTypeAliasType(targetContext, expandTypeAliases, classifierId.classId)
        is CirClassifierId.TypeParameter -> buildTypeParameterType(classifierId.index)
    }
}

private fun CirSimpleType.buildTypeParameterType(index: Int): KmType {
    val type = KmType(typeFlags())
    type.classifier = KmClassifier.TypeParameter(index)
    return type
}

private fun CirSimpleType.buildClassType(
    targetContext: TargetVisitingContext,
    expandTypeAliases: Boolean,
    classId: ClassId
): KmType {
    val type = KmType(typeFlags())
    type.classifier = KmClassifier.Class(classId.asString())
    arguments.mapTo(type.arguments) { it.buildArgument(targetContext, expandTypeAliases) }
    // TODO: outerType

    return type
}

private fun CirSimpleType.buildTypeAliasType(
    targetContext: TargetVisitingContext,
    expandTypeAliases: Boolean,
    typeAliasId: ClassId,
): KmType {
    // TODO: this is wrong, type alias has unsubstituted underlying type
    val cirUnderlyingType = (targetContext.resolveClassifier(typeAliasId) as? CirTypeAlias)?.underlyingType
        ?: return buildClassType(targetContext, expandTypeAliases, typeAliasId)

    val abbreviationType = KmType(typeFlags())
    abbreviationType.classifier = KmClassifier.TypeAlias(typeAliasId.asString())
    arguments.mapTo(abbreviationType.arguments) { it.buildArgument(targetContext, expandTypeAliases) }

    return if (expandTypeAliases) {
        val underlyingType = cirUnderlyingType.buildType(targetContext, expandTypeAliases = true)

        val type = KmType(underlyingType.flags)
        type.classifier = underlyingType.classifier
        type.arguments += underlyingType.arguments
        type.abbreviatedType = abbreviationType

        type
    } else {
        abbreviationType
    }
}

private fun CirTypeProjection.buildArgument(targetContext: TargetVisitingContext, expandTypeAliases: Boolean): KmTypeProjection {
    return if (isStarProjection) {
        KmTypeProjection.STAR
    } else {
        KmTypeProjection(
            variance = projectionKind.buildVariance(),
            type = type.buildType(targetContext, expandTypeAliases)
        )
    }
}
