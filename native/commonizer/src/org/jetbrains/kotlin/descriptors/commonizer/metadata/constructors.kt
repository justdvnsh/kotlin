/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmConstructor
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassConstructor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassConstructorNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup

internal fun CirClassConstructorNode.buildClassConstructors(context: VisitingContext): List<KmConstructor?> {
    val constructors = CommonizedGroup<KmConstructor>(dimension)

    targetDeclarations.forEachIndexed { index, cirFunction ->
        cirFunction?.buildClassConstructor(context, constructors, index)
    }
    commonDeclaration()?.buildClassConstructor(context, constructors, indexOfCommon)

    return constructors
}

private fun CirClassConstructor.buildClassConstructor(
    context: VisitingContext,
    output: CommonizedGroup<KmConstructor>,
    index: Int
) {
    output[index] = KmConstructor(
        flags = classConstructorFlags()
    ).also { function ->
        val targetContext = context.targetContexts[index]

        annotations.mapTo(function.annotations) { it.buildAnnotation() }
        valueParameters.mapTo(function.valueParameters) { it.buildValueParameter(targetContext) }
    }
}
