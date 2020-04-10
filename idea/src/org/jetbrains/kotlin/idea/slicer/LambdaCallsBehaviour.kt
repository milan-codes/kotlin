/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceUsage
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.findUsages.handlers.SliceUsageProcessor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

interface KotlinSliceUsageTransformer {
    fun transform(usage: KotlinSliceUsage): SliceUsage?

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

data class LambdaCallsBehaviour(
    val usageTransformer: KotlinSliceUsageTransformer,
    override val originalBehaviour: KotlinSliceUsage.SpecialBehaviour?
) : KotlinSliceUsage.SpecialBehaviour {

    override fun processUsages(element: KtElement, parent: KotlinSliceUsage, uniqueProcessor: SliceUsageProcessor) {
        val processor = object : SliceUsageProcessor {
            override fun process(sliceUsage: SliceUsage): Boolean {
                if (sliceUsage is KotlinSliceUsage && sliceUsage.behaviour === this@LambdaCallsBehaviour) {
                    val sliceElement = sliceUsage.element ?: return true
                    val resolvedCall = (sliceElement as? KtElement)?.resolveToCall()
                    if (resolvedCall?.call?.callType == Call.CallType.INVOKE) {
                        return usageTransformer.transform(sliceUsage)?.let { uniqueProcessor.process(it) } ?: true
                    }
                }
                return uniqueProcessor.process(sliceUsage)
            }
        }
        OutflowSlicer(element, processor, parent).processChildren(parent.forcedExpressionMode)
    }

    override val slicePresentationPrefix: String
        get() = KotlinBundle.message("slicer.text.tracking.lambda.calls")

    override val testPresentationPrefix: String
        get() = "[LAMBDA CALLS] "
}