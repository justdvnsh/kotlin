/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor2
import org.jetbrains.kotlin.descriptors.commonizer.builder.createGlobalBuilderComponents
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirTreeMerger
import org.jetbrains.kotlin.storage.LockBasedStorageManager

fun runCommonization(parameters: Parameters): Result {
    if (!parameters.hasAnythingToCommonize())
        return Result.NothingToCommonize

    val storageManager = LockBasedStorageManager("Declaration descriptors commonization")

    checkpoint("START")

    // build merged tree:
    val mergeResult = CirTreeMerger(storageManager, parameters).merge()

    checkpoint("MERGED CIR TREE")

    // commonize:
    val mergedTree = mergeResult.root
    mergedTree.accept(CommonizationVisitor(mergedTree), Unit)
    parameters.progressLogger?.invoke("Commonized declarations")

    checkpoint("COMMONIZED")

    // build resulting descriptors:
    val components = mergedTree.createGlobalBuilderComponents(storageManager, parameters)
    mergedTree.accept(DeclarationsBuilderVisitor1(components), emptyList())
    mergedTree.accept(DeclarationsBuilderVisitor2(components), emptyList())

    checkpoint("BUILT DESCRIPTORS")

    val modulesByTargets = LinkedHashMap<Target, Collection<ModuleResult>>() // use linked hash map to preserve order
    components.targetComponents.forEach { component ->
        val target = component.target
        check(target !in modulesByTargets)

        val commonizedModules: List<ModuleResult.Commonized> = components.cache.getAllModules(component.index).map(ModuleResult::Commonized)

        val absentModules: List<ModuleResult.Absent> = if (target is InputTarget)
            mergeResult.absentModuleInfos.getValue(target).map { ModuleResult.Absent(it.originalLocation) }
        else emptyList()

        modulesByTargets[target] = commonizedModules + absentModules
    }

    parameters.progressLogger?.invoke("Prepared new descriptors")

    return Result.Commonized(modulesByTargets)
}

private fun checkpoint(name: String) {
//    fun printMemoryUsage(message: String) {
//        val runtime = Runtime.getRuntime()
//
//        val free = runtime.freeMemory()
//        val total = runtime.totalMemory()
//        val used = total - free
//        val max = runtime.maxMemory()
//
//        fun Long.toHumanPresentation() = "${(this / 1024 / 1024)}MB"
//
//        println(
//            """
//            $message:
//            - Used:  ${used.toHumanPresentation()}
//            - Free:  ${free.toHumanPresentation()}
//            - Total: ${total.toHumanPresentation()}
//            - Max:   ${max.toHumanPresentation()}
//        """.trimIndent()
//        )
//    }
//
//    println()
//    println("*** Checkpoint \"$name\" ***")
//    printMemoryUsage("Before-GC memory usage")
//    print("Triggering GC...")
//
//    repeat(20) {
//        System.gc()
//    }
////    Thread.sleep(1000 * 30)
//
//    println(" Done")
//    printMemoryUsage("After-GC memory usage")
//    println()
}
