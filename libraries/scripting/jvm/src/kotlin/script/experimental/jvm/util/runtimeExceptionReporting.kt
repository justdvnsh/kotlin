/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.util

import java.io.PrintStream
import java.util.*
import kotlin.script.experimental.api.ResultValue

fun ResultValue.Error.renderError(stream: PrintStream) {
    val fullTrace = error.stackTrace
    if (wrappingException == null || fullTrace.size < wrappingException!!.stackTrace.size) {
        error.printStackTrace(stream)
    } else {
        // subtracting wrapping message stacktrace from error stacktrace to show only user-specific part of it
        // using code and approach from Throwable
        // Guard against malicious overrides of Throwable.equals by using a Set with identity equality semantics.
        val dejaVu = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        dejaVu.add(wrappingException)
        error.printEnclosedStackTrace(stream, wrappingException!!.stackTrace, "", "", dejaVu)
    }
}

// Taken from Throwable.printEnclosedStackTrace with minimal modifications
private fun Throwable.printEnclosedStackTrace(
    s: PrintStream,
    enclosingTrace: Array<StackTraceElement>,
    caption: String,
    prefix: String,
    dejaVu: MutableSet<Throwable>
) {
    if (dejaVu.contains(this)) {
        s.println("\t[CIRCULAR REFERENCE:$this]")
    } else {
        dejaVu.add(this)
        // Compute number of frames in common between this and enclosing trace
        val trace: Array<StackTraceElement> = stackTrace
        var m = trace.size - 1
        var n = enclosingTrace.size - 1
        while (m >= 0 && n >= 0 && trace[m] == enclosingTrace[n]) {
            m--
            n--
        }
        val framesInCommon = trace.size - 1 - m

        // Print our stack trace
        s.println(prefix + caption + this)
        for (i in 0..m) s.println(prefix + "\tat " + trace[i])
        // here is the difference with the code form Throwable - we do not want the common part to be marked in the trace
//        if (framesInCommon != 0) s.println("$prefix\t... $framesInCommon more")

        // Print suppressed exceptions, if any
        for (se in getSuppressed()) se.printEnclosedStackTrace(
            s, trace, "Suppressed: ",
            prefix + "\t", dejaVu
        )

        // Print cause, if any
        val ourCause: Throwable? = cause
        ourCause?.printEnclosedStackTrace(s, trace, "Caused by: ", prefix, dejaVu)
    }
}
