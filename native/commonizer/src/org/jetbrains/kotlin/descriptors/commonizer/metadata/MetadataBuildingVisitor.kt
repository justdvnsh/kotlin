/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.utils.addToStdlib.cast

// TODO: mark expects properly
// TODO: handle lifted up declarations properly
// TODO: add logging
class MetadataBuildingVisitor(
    private val context: VisitingContext
) : CirNodeVisitor<Unit, Any> {
    override fun visitRootNode(node: CirRootNode, data: Unit): Map<Target, List<KlibModuleMetadata>> {
        val allModules = GroupOfMutableLists<KlibModuleMetadata>(node.dimension)
        node.modules.values.forEach { allModules += it.accept(this, Unit).cast() }

        return (node.targetDeclarations + node.commonDeclaration())
            .mapIndexed { index, cirRoot -> cirRoot!!.target to allModules[index] }
            .toMap()
    }

    override fun visitModuleNode(node: CirModuleNode, data: Unit): List<KlibModuleMetadata?> {
        val allFragments = GroupOfMutableLists<KmModuleFragment>(node.dimension)
        node.packages.values.forEach { allFragments += it.accept(this, Unit).cast() }

        addEmptyFragments(allFragments)

        return node.buildModules(allFragments)
    }

    override fun visitPackageNode(node: CirPackageNode, data: Unit): List<KmModuleFragment?> {
        val allClasses = GroupOfMutableLists<KmClass>(node.dimension)
        node.classes.values.forEach { allClasses += it.accept(this, Unit).cast() }

        val allTypeAliases = GroupOfMutableLists<KmTypeAlias>(node.dimension)
        node.typeAliases.values.forEach { typeAliasNode ->
            val classifiers: List<Any?> = typeAliasNode.accept(this, Unit).cast()
            classifiers.forEachIndexed { index, classifier ->
                when (classifier) {
                    null -> Unit
                    is KmClass -> allClasses[index].add(classifier)
                    is KmTypeAlias -> allTypeAliases[index].add(classifier)
                    else -> error("Unexpected classifier: ${classifier::class.java}, $classifier")
                }
            }
        }

        val allFunctions = GroupOfMutableLists<KmFunction>(node.dimension)
        node.functions.values.forEach { allFunctions += it.accept(this, Unit).cast() }

        val allProperties = GroupOfMutableLists<KmProperty>(node.dimension)
        node.properties.values.forEach { allProperties += it.accept(this, Unit).cast() }

        return node.buildFragments(allClasses, allTypeAliases, allFunctions, allProperties)
    }

    override fun visitPropertyNode(node: CirPropertyNode, data: Unit): List<KmProperty?> {
        return node.buildProperties(context)
    }

    override fun visitFunctionNode(node: CirFunctionNode, data: Unit): List<KmFunction?> {
        return node.buildFunctions(context)
    }

    override fun visitClassNode(node: CirClassNode, data: Unit): List<KmClass?> {
        TODO("Not yet implemented")
    }

    override fun visitClassConstructorNode(node: CirClassConstructorNode, data: Unit): List<KmConstructor?> {
        return node.buildClassConstructors(context)
    }

    override fun visitTypeAliasNode(node: CirTypeAliasNode, data: Unit): List<Any?> {
        TODO("Not yet implemented")
    }
}

internal abstract class GroupOfLists<T : Any> : AbstractList<List<T>>()

internal class GroupOfMutableLists<T : Any>(override val size: Int) : GroupOfLists<T>() {
    private val lists: Array<MutableList<T>> = Array(size) { mutableListOf() }

    override fun get(index: Int): MutableList<T> = lists[index]

    override fun iterator(): Iterator<MutableList<T>> = lists.iterator()

    operator fun plusAssign(list: List<T?>) {
        list.forEachIndexed { index, value ->
            if (value != null) lists[index].add(value)
        }
    }
}
