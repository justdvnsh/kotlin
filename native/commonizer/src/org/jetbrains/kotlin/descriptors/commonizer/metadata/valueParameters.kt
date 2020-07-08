/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmValueParameter
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter

internal fun CirValueParameter.buildValueParameter(targetContext: TargetVisitingContext): KmValueParameter {
    return KmValueParameter(
        flags = valueParameterFlags(),
        name = name.asString()
    ).also { parameter ->
        annotations.mapTo(parameter.annotations) { it.buildAnnotation() }
        parameter.type = returnType.buildType(targetContext)
        varargElementType?.let { varargElementType ->
            parameter.varargElementType = varargElementType.buildType(targetContext)
        }
    }
}
