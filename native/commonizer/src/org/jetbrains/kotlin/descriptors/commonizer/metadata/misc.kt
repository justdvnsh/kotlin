/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmVariance
import org.jetbrains.kotlin.types.Variance

@Suppress("NOTHING_TO_INLINE")
internal inline fun Variance.buildVariance(): KmVariance {
    return when (this) {
        Variance.INVARIANT -> KmVariance.INVARIANT
        Variance.IN_VARIANCE -> KmVariance.IN
        Variance.OUT_VARIANCE -> KmVariance.OUT
    }
}
