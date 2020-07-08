/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmTypeParameter
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter

internal fun CirTypeParameter.buildTypeParameter(targetContext: TargetVisitingContext): KmTypeParameter {
    return KmTypeParameter(
        flags = typeParameterFlags(),
        name = name.asString(),
        id = 0, // TODO: supply interned type parameter ID
        variance = variance.buildVariance()
    ).also { parameter ->
        upperBounds.mapTo(parameter.upperBounds) { it.buildType(targetContext) }
        annotations.mapTo(parameter.annotations) { it.buildAnnotation() }
    }
}
