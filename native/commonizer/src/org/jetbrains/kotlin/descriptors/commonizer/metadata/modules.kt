/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.KmModuleFragment
import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirModule
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirModuleNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup

internal fun CirModuleNode.buildModules(
    allFragments: GroupOfMutableLists<KmModuleFragment>
): List<KlibModuleMetadata?> {
    val modules = CommonizedGroup<KlibModuleMetadata>(dimension)

    targetDeclarations.forEachIndexed { index, cirModule ->
        cirModule?.buildModule(modules, index, allFragments)
    }
    commonDeclaration()?.buildModule(modules, indexOfCommon, allFragments)

    return modules
}

private fun CirModule.buildModule(
    output: CommonizedGroup<KlibModuleMetadata>,
    index: Int,
    allFragments: GroupOfMutableLists<KmModuleFragment>
) {
    output[index] = KlibModuleMetadata(
        name = name.asString().removeSurrounding("<", ">"),
        fragments = allFragments[index],
        annotations = emptyList()
    )
}
