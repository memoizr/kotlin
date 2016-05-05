/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmMultifileClass
@file:JvmName("UastUtils")
package org.jetbrains.uast

fun UQualifiedExpression.asQualifiedString(): String? = asQualifiedPath()?.joinToString(".")

fun UExpression.asQualifiedPath(): List<String>? {
    if (this is USimpleReferenceExpression) {
        return listOf(this.identifier)
    } else if (this !is UQualifiedExpression) {
        return null
    }

    var error = false
    val list = mutableListOf<String>()
    fun addIdentifiers(expr: UQualifiedExpression) {
        val receiver = expr.receiver
        val selector = expr.selector as? USimpleReferenceExpression ?: run { error = true; return }
        when (receiver) {
            is UQualifiedExpression -> addIdentifiers(receiver)
            is USimpleReferenceExpression -> list += receiver.identifier
            else -> {
                error = true
                return
            }
        }
        list += selector.identifier
    }

    addIdentifiers(this)
    return if (error) null else list
}

/**
 * Checks if the received expression is a qualified chain of identifiers, and the trailing part of such chain is [fqName].
 *
 * @param fqName the chain part to check against. Sequence of identifiers, separated by dot ('.'). Example: "com.example".
 * @return true, if the received expression is a qualified chain of identifiers, and the trailing part of such chain is [fqName].
 */
fun UExpression.matchesQualified(fqName: String): Boolean {
    val identifiers = this.asQualifiedPath() ?: return false
    val passedIdentifiers = fqName.trim('.').split('.')
    return identifiers == passedIdentifiers
}

/**
 * Checks if the received expression is a qualified chain of identifiers, and the leading part of such chain is [fqName].
 *
 * @param fqName the chain part to check against. Sequence of identifiers, separated by dot ('.'). Example: "com.example".
 * @return true, if the received expression is a qualified chain of identifiers, and the leading part of such chain is [fqName].
 */
fun UExpression.startsWithQualified(fqName: String): Boolean {
    val identifiers = this.asQualifiedPath() ?: return false
    val passedIdentifiers = fqName.trim('.').split('.')
    if (identifiers.size < passedIdentifiers.size) return false
    passedIdentifiers.forEachIndexed { i, passedIdentifier ->
        if (passedIdentifier != identifiers[i]) return false
    }
    return true
}

/**
 * Checks if the received expression is a qualified chain of identifiers, and the trailing part of such chain is [fqName].
 *
 * @param fqName the chain part to check against. Sequence of identifiers, separated by dot ('.'). Example: "com.example".
 * @return true, if the received expression is a qualified chain of identifiers, and the trailing part of such chain is [fqName].
 */
fun UExpression.endsWithQualified(fqName: String): Boolean {
    val identifiers = this.asQualifiedPath()?.asReversed() ?: return false
    val passedIdentifiers = fqName.trim('.').split('.').asReversed()
    if (identifiers.size < passedIdentifiers.size) return false
    passedIdentifiers.forEachIndexed { i, passedIdentifier ->
        if (passedIdentifier != identifiers[i]) return false
    }
    return true
}

/**
 * Get the topmost parent qualified expression for the call expression.
 *
 * Example 1:
 *  Code: variable.call(args)
 *  Call element: E = call(args)
 *  Qualified parent (return value): Q = [getQualifiedCallElement](E) = variable.call(args)
 *
 * Example 2:
 *  Code: call(args)
 *  Call element: E = call(args)
 *  Qualified parent (return value): Q = [getQualifiedCallElement](E) = call(args) (no qualifier)
 *
 *  @return containing qualified expression if the call is a child of the qualified expression, call element otherwise.
 */
fun UExpression.getQualifiedParentOrThis(): UExpression {
    fun findParent(current: UExpression?, previous: UExpression): UExpression? = when (current) {
        is UQualifiedExpression -> {
            if (current.selector == previous)
                findParent(current.parent as? UExpression, current) ?: current
            else
                previous
        }
        else -> null
    }

    return findParent(parent as? UExpression, this) ?: this
}

/**
 * Return the outermost qualified expression.
 *
 * @return the outermost qualified expression,
 *  this element if the parent expression is not a qualified expression,
 *  or null if the element is not a qualified expression.
 *
 *  Example:
 *   Code: a.b.c(asd).g
 *   Call element: c(asd)
 *   Outermost qualified (return value): a.b.c(asd).g
 */
fun UExpression.getOutermostQualified(): UQualifiedExpression? {
    val parent = this.parent
    return when (parent) {
        is UQualifiedExpression -> parent.getOutermostQualified()
        else -> if (this is UQualifiedExpression) this else null
    }
}

/**
 * Return the list of qualified expressions.
 *
 * Example:
 *   Code: obj.call(param).anotherCall(param2).getter
 *   Qualified chain: [obj, call(param), anotherCall(param2), getter]
 *
 * @return list of qualified expressions, or the empty list if the received expression is not a qualified expression.
 */
fun UExpression.getQualifiedChain(): List<UExpression> {
    fun collect(expr: UQualifiedExpression, chains: MutableList<UExpression>) {
        val receiver = expr.receiver
        if (receiver is UQualifiedExpression) {
            collect(receiver, chains)
        } else {
            chains += receiver
        }

        val selector = expr.selector
        if (selector is UQualifiedExpression) {
            collect(selector, chains)
        } else {
            chains += selector
        }
    }

    val qualifiedExpression = this.getOutermostQualified() ?: return emptyList()
    val chains = mutableListOf<UExpression>()
    collect(qualifiedExpression, chains)
    return chains
}