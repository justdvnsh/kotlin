/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmFunction
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunction
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirFunctionNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup

internal fun CirFunctionNode.buildFunctions(context: VisitingContext): List<KmFunction?> {
    val functions = CommonizedGroup<KmFunction>(dimension)

    targetDeclarations.forEachIndexed { index, cirFunction ->
        cirFunction?.buildFunction(context, functions, index, false)
    }
    commonDeclaration()?.buildFunction(context, functions, indexOfCommon, true)

    return functions
}

private fun CirFunction.buildFunction(
    context: VisitingContext,
    output: CommonizedGroup<KmFunction>,
    index: Int,
    isExpect: Boolean
) {
    output[index] = KmFunction(
        flags = functionFlags(isExpect),
        name = name.asString()
    ).also { function ->
        val targetContext = context.targetContexts[index]

        annotations.mapTo(function.annotations) { it.buildAnnotation() }
        typeParameters.mapTo(function.typeParameters) { it.buildTypeParameter(targetContext) }
        valueParameters.mapTo(function.valueParameters) { it.buildValueParameter(targetContext) }
        extensionReceiver?.let { receiver ->
            // TODO where to write receiver annotations?
            function.receiverParameterType = receiver.type.buildType(targetContext)
        }
        function.returnType = returnType.buildType(targetContext)
    }
}
