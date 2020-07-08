/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.className
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackage
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirPackageNode
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup

internal fun CirPackageNode.buildFragments(
    allClasses: GroupOfLists<KmClass>,
    allTypeAliases: GroupOfLists<KmTypeAlias>,
    allFunctions: GroupOfLists<KmFunction>,
    allProperties: GroupOfLists<KmProperty>
): List<KmModuleFragment?> {
    val fragments = CommonizedGroup<KmModuleFragment>(dimension)

    targetDeclarations.forEachIndexed { index, cirPackage ->
        cirPackage?.buildFragment(fragments, index, allClasses, allTypeAliases, allFunctions, allProperties)
    }
    commonDeclaration()?.buildFragment(fragments, indexOfCommon, allClasses, allTypeAliases, allFunctions, allProperties)

    return fragments
}

private fun CirPackage.buildFragment(
    output: CommonizedGroup<KmModuleFragment>,
    index: Int,
    allClasses: GroupOfLists<KmClass>,
    allTypeAliases: GroupOfLists<KmTypeAlias>,
    allFunctions: GroupOfLists<KmFunction>,
    allProperties: GroupOfLists<KmProperty>
) {
    output[index] = KmModuleFragment().also { fragment ->
        fragment.fqName = fqName.asString()
        allClasses[index].forEach {
            fragment.classes += it
            fragment.className += it.name
        }

        val typeAliases = allTypeAliases[index]
        val functions = allFunctions[index]
        val properties = allProperties[index]

        if (typeAliases.isNotEmpty() || functions.isNotEmpty() || properties.isNotEmpty()) {
            fragment.pkg = KmPackage().also { pkg ->
                pkg.fqName = fqName.asString()
                pkg.typeAliases += allTypeAliases[index]
                pkg.functions += allFunctions[index]
                pkg.properties += allProperties[index]
            }
        }
    }
}

internal fun addEmptyFragments(allFragments: GroupOfMutableLists<KmModuleFragment>) {
    allFragments.iterator().forEachRemaining { fragments ->
        val existingPackageFqNames: Set<String> = fragments.mapTo(HashSet()) { it.fqName!! }

        val missingPackageFqNames: Set<String> = existingPackageFqNames.flatMapTo(HashSet()) { fqName ->
            fqName.mapIndexedNotNull { index, ch ->
                if (ch == '.') {
                    val parentFqName = fqName.substring(0, index)
                    if (parentFqName !in existingPackageFqNames)
                        return@mapIndexedNotNull parentFqName
                }

                null
            }
        }

        missingPackageFqNames.forEach { fqName ->
            fragments += KmModuleFragment().also { fragment ->
                fragment.fqName = fqName
            }
        }
    }
}
