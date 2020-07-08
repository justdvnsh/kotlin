/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmProperty
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.compileTimeValue
import kotlinx.metadata.klib.getterAnnotations
import kotlinx.metadata.klib.setterAnnotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirProperty
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirValueParameterImpl
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirPropertyNode
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.Name

internal fun CirPropertyNode.buildProperties(context: VisitingContext): List<KmProperty?> {
    val properties = CommonizedGroup<KmProperty>(dimension)

    targetDeclarations.forEachIndexed { index, cirProperty ->
        cirProperty?.buildProperty(context, properties, index, false)
    }
    commonDeclaration()?.buildProperty(context, properties, indexOfCommon, true)

    return properties
}

private fun CirProperty.buildProperty(
    context: VisitingContext,
    output: CommonizedGroup<KmProperty>,
    index: Int,
    isExpect: Boolean
) {
    output[index] = KmProperty(
        flags = propertyFlags(isExpect),
        name = name.asString(),
        getterFlags = getter?.propertyAccessorFlags(this) ?: NO_FLAGS,
        setterFlags = setter?.let { setter -> setter.propertyAccessorFlags(setter) } ?: NO_FLAGS
    ).also { property ->
        val targetContext = context.targetContexts[index]

        annotations.mapTo(property.annotations) { it.buildAnnotation() }
        getter?.annotations?.mapTo(property.getterAnnotations) { it.buildAnnotation() }
        setter?.annotations?.mapTo(property.setterAnnotations) { it.buildAnnotation() }
        property.compileTimeValue = compileTimeInitializer?.buildAnnotationArgument()
        typeParameters.mapTo(property.typeParameters) { it.buildTypeParameter(targetContext) }
        extensionReceiver?.let { receiver ->
            // TODO where to write receiver annotations?
            property.receiverParameterType = receiver.type.buildType(targetContext)
        }
        setter?.let { setter ->
            property.setterParameter = CirValueParameterImpl(
                annotations = setter.parameterAnnotations,
                name = SETTER_VALUE_NAME,
                returnType = returnType,
                varargElementType = null,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false
            ).buildValueParameter(targetContext)
        }
        property.returnType = returnType.buildType(targetContext)
    }
}

private val SETTER_VALUE_NAME = Name.identifier("value")
