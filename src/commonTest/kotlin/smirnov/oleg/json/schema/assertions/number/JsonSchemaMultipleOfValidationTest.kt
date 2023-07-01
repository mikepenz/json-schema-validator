package smirnov.oleg.json.schema.assertions.number

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import smirnov.oleg.json.schema.JsonSchema
import smirnov.oleg.json.schema.KEY
import smirnov.oleg.json.schema.ValidationError

@Suppress("unused")
class JsonSchemaMultipleOfValidationTest : FunSpec() {
  init {
    listOf(
      JsonPrimitive(7),
      JsonPrimitive(49),
      JsonPrimitive(49.0),
      JsonPrimitive(-49),
      JsonPrimitive(-49.0),
    ).forEach {
      val schema = JsonSchema.fromDescription(
        """
          {
            "${KEY}schema": "http://json-schema.org/draft-07/schema#",
            "multipleOf": 7
          }
          """.trimIndent()
      )
      test("integer in multiple of: $it % 7") {
        val errors = mutableListOf<ValidationError>()
        val valid = schema.validate(it, errors::add)
        valid shouldBe true
        errors shouldHaveSize 0
      }
    }

    listOf(
      JsonPrimitive(3),
      JsonPrimitive(3.0),
      JsonPrimitive(4.5),
      JsonPrimitive(-3),
      JsonPrimitive(-3.0),
      JsonPrimitive(-4.5),
    ).forEach {
      val schema = JsonSchema.fromDescription(
        """
          {
            "${KEY}schema": "http://json-schema.org/draft-07/schema#",
            "multipleOf": 1.5
          }
          """.trimIndent()
      )
      test("double in multiple of: $it % 1.5") {
        val errors = mutableListOf<ValidationError>()
        val valid = schema.validate(it, errors::add)
        valid shouldBe true
        errors shouldHaveSize 0
      }
    }

    listOf(
      JsonPrimitive(0),
      JsonPrimitive(0.0),
      JsonPrimitive(-1),
      JsonPrimitive(-1.0),
      JsonPrimitive(-Double.MAX_VALUE),
      JsonPrimitive(Long.MIN_VALUE),
    ).forEach {
      test("reports not positive value $it") {
        shouldThrow<IllegalArgumentException> {
          JsonSchema.fromDescription(
            """
            {
              "${KEY}schema": "http://json-schema.org/draft-07/schema#",
              "multipleOf": $it
            }
            """.trimIndent()
          )
        }.message shouldBe "multipleOf value $it must be greater than zero"
      }
    }

    val numberSchema = JsonSchema.fromDescription(
      """
      {
        "${KEY}schema": "http://json-schema.org/draft-07/schema#",
        "multipleOf": 5
      }
      """.trimIndent()
    )
    listOf(
      JsonPrimitive(true),
      JsonPrimitive("42"),
      JsonNull,
      buildJsonObject { },
      buildJsonArray { },
    ).forEach {
      test("not a number $it passes the validation") {
        val errors = mutableListOf<ValidationError>()
        val valid = numberSchema.validate(it, errors::add)
        valid shouldBe true
        errors shouldHaveSize 0
      }
    }
  }
}