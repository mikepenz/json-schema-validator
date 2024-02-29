package io.github.optimumcode.json.schema.internal.factories.general

import io.github.optimumcode.json.pointer.JsonPointer
import io.github.optimumcode.json.schema.AnnotationKey
import io.github.optimumcode.json.schema.ErrorCollector
import io.github.optimumcode.json.schema.FormatValidationResult.Invalid
import io.github.optimumcode.json.schema.FormatValidationResult.Valid
import io.github.optimumcode.json.schema.FormatValidator
import io.github.optimumcode.json.schema.ValidationError
import io.github.optimumcode.json.schema.internal.AnnotationKeyFactory
import io.github.optimumcode.json.schema.internal.AssertionContext
import io.github.optimumcode.json.schema.internal.JsonSchemaAssertion
import io.github.optimumcode.json.schema.internal.LoadingContext
import io.github.optimumcode.json.schema.internal.TrueSchemaAssertion
import io.github.optimumcode.json.schema.internal.factories.AbstractAssertionFactory
import io.github.optimumcode.json.schema.internal.formats.DateFormatValidator
import io.github.optimumcode.json.schema.internal.formats.DateTimeFormatValidator
import io.github.optimumcode.json.schema.internal.formats.DurationFormatValidator
import io.github.optimumcode.json.schema.internal.formats.TimeFormatValidator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal sealed class FormatAssertionFactory(
  private val assertion: Boolean,
) : AbstractAssertionFactory(FORMAT_PROPERTY) {
  internal data object AnnotationOnly : FormatAssertionFactory(assertion = false)

  internal data object AnnotationAndAssertion : FormatAssertionFactory(assertion = true)

  override fun createFromProperty(
    element: JsonElement,
    context: LoadingContext,
  ): JsonSchemaAssertion {
    require(element is JsonPrimitive && element.isString) { "$property must be a string" }
    val formatKey = element.content.lowercase()
    val validator =
      KNOWN_FORMATS[formatKey]
        ?: context.customFormatValidators[formatKey]
        // assertion with unknown format must fail schema loading
        // but right now the library does not have all required formats implemented
        // so it cannot throw an error here
        ?: return TrueSchemaAssertion

    return FormatAssertion(
      context.schemaPath,
      formatKey,
      validator,
      assertion,
    )
  }

  internal companion object {
    private const val FORMAT_PROPERTY = "format"
    internal val ANNOTATION: AnnotationKey<String> = AnnotationKeyFactory.create(FORMAT_PROPERTY)
    private val KNOWN_FORMATS: Map<String, FormatValidator> =
      mapOf(
        "date" to DateFormatValidator,
        "time" to TimeFormatValidator,
        "date-time" to DateTimeFormatValidator,
        "duration" to DurationFormatValidator,
      )
  }
}

private class FormatAssertion(
  private val schemaPath: JsonPointer,
  private val formatKey: String,
  private val validator: FormatValidator,
  private val assertion: Boolean,
) : JsonSchemaAssertion {
  override fun validate(
    element: JsonElement,
    context: AssertionContext,
    errorCollector: ErrorCollector,
  ): Boolean {
    val result = validator.validate(element)
    return when (result) {
      Valid -> {
        context.annotationCollector.annotate(FormatAssertionFactory.ANNOTATION, formatKey)
        true
      }

      Invalid -> {
        if (assertion) {
          errorCollector.onError(
            ValidationError(
              schemaPath = schemaPath,
              objectPath = context.objectPath,
              message = "value does not match '$formatKey' format",
            ),
          )
          false
        } else {
          // only annotation should return true if format does not match
          true
        }
      }
    }
  }
}