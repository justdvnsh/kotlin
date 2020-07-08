/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassifier
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderStandardKotlinPackages
import org.jetbrains.kotlin.name.ClassId

class VisitingContext(rootNode: CirRootNode) {
    val targetContexts: List<TargetVisitingContext> = (0 until rootNode.dimension).map { index ->
        TargetVisitingContext(
            isCommon = index == rootNode.indexOfCommon,
            index = index,
            cache = rootNode.cache
        )
    }
}

class TargetVisitingContext(
    private val isCommon: Boolean,
    private val index: Int,
    private val cache: CirClassifiersCache
) {
    fun resolveClassifier(classId: ClassId): CirClassifier {
        if (classId.packageFqName.isUnderStandardKotlinPackages) {
            TODO("How to fetch the classifier from the built-ins module?")
        } else {
            TODO("How to fetch the classifier from the cache?")
        }
    }
}
