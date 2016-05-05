/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast

internal val ERROR_NAME = "<error>"

internal val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

internal operator fun String.times(n: Int) = this.repeat(n)

internal fun List<UElement>.logString() = joinToString(LINE_SEPARATOR) { it.logString().withMargin }

internal fun UModifierOwner.renderModifiers() = UastModifier.VALUES
        .filter { hasModifier(it) }
        .joinToString(" ") { it.name }

internal fun StringBuilder.appendWithSpace(s: String) {
    if (s.isNotEmpty()) {
        append(s)
        append(' ')
    }
}

internal fun renderAnnotations(annotations: List<UAnnotation>): String = buildString {
    for (annotation in annotations) {
        appendln(annotation.renderString())
    }
    if (annotations.isNotEmpty()) {
        appendln()
    }
}

internal fun <T> lz(f: () -> T) = lazy(LazyThreadSafetyMode.NONE, f)