package com.github.optimumcode.json.schema.internal.factories.`object`

import com.github.optimumcode.json.pointer.JsonPointer
import com.github.optimumcode.json.schema.ErrorCollector
import com.github.optimumcode.json.schema.ValidationError
import com.github.optimumcode.json.schema.internal.AssertionContext
import com.github.optimumcode.json.schema.internal.JsonSchemaAssertion
import com.github.optimumcode.json.schema.internal.LoadingContext
import com.github.optimumcode.json.schema.internal.TrueSchemaAssertion
import com.github.optimumcode.json.schema.internal.factories.AbstractAssertionFactory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Suppress("unused")
internal object RequiredAssertionFactory : AbstractAssertionFactory("required") {
  override fun createFromProperty(element: JsonElement, context: LoadingContext): JsonSchemaAssertion {
    require(element is JsonArray) { "$property must be an array" }
    require(element.all { it is JsonPrimitive && it.isString }) { "$property must contain only strings" }
    val uniqueRequiredProperties = element.mapTo(linkedSetOf()) { (it as JsonPrimitive).content }
    require(uniqueRequiredProperties.size == element.size) { "$property must consist of unique elements" }
    return if (uniqueRequiredProperties.isEmpty()) {
      TrueSchemaAssertion
    } else {
      RequiredAssertion(context.schemaPath, uniqueRequiredProperties)
    }
  }
}

private class RequiredAssertion(
  private val path: JsonPointer,
  private val requiredProperties: Set<String>,
) : JsonSchemaAssertion {
  override fun validate(element: JsonElement, context: AssertionContext, errorCollector: ErrorCollector): Boolean {
    if (element !is JsonObject) {
      return true
    }
    val missingProperties = requiredProperties.asSequence()
      .filter { !element.containsKey(it) }
      .toSet()
    if (missingProperties.isEmpty()) {
      return true
    }
    errorCollector.onError(
      ValidationError(
        schemaPath = path,
        objectPath = context.objectPath,
        message = "missing required properties: $missingProperties",
      ),
    )
    return false
  }
}