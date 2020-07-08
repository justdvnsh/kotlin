/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassifierId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*

internal fun CirAnnotation.buildAnnotation(): KmAnnotation {
    val arguments = LinkedHashMap<String, KmAnnotationArgument<*>>(constantValueArguments.size + annotationValueArguments.size, 1F)

    constantValueArguments.forEach { (name: Name, value: ConstantValue<*>) ->
        arguments[name.asString()] = value.buildAnnotationArgument()
    }

    annotationValueArguments.forEach { (name: Name, nested: CirAnnotation) ->
        arguments[name.asString()] = KmAnnotationArgument.AnnotationValue(nested.buildAnnotation())
    }

    return KmAnnotation(
        className = (type.classifierId as CirClassifierId.Class).classId.asString(),
        arguments = arguments
    )
}

internal fun ConstantValue<*>.buildAnnotationArgument(): KmAnnotationArgument<*> {
    return when (this) {
        is StringValue -> KmAnnotationArgument.StringValue(value)
        is CharValue -> KmAnnotationArgument.CharValue(value)

        is ByteValue -> KmAnnotationArgument.ByteValue(value)
        is ShortValue -> KmAnnotationArgument.ShortValue(value)
        is IntValue -> KmAnnotationArgument.IntValue(value)
        is LongValue -> KmAnnotationArgument.LongValue(value)

        is UByteValue -> KmAnnotationArgument.UByteValue(value)
        is UShortValue -> KmAnnotationArgument.UShortValue(value)
        is UIntValue -> KmAnnotationArgument.UIntValue(value)
        is ULongValue -> KmAnnotationArgument.ULongValue(value)

        is FloatValue -> KmAnnotationArgument.FloatValue(value)
        is DoubleValue -> KmAnnotationArgument.DoubleValue(value)
        is BooleanValue -> KmAnnotationArgument.BooleanValue(value)

        is EnumValue -> KmAnnotationArgument.EnumValue(enumClassId.asString(), enumEntryName.asString())
        is ArrayValue -> KmAnnotationArgument.ArrayValue(value.map { it.buildAnnotationArgument() })

        else -> error("Unsupported annotation argument type: ${this::class.java}, $this")
    }
}
