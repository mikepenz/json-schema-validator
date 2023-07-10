package smirnov.oleg.json.schema.assertions.`object`

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import smirnov.oleg.json.pointer.JsonPointer
import smirnov.oleg.json.schema.JsonSchema
import smirnov.oleg.json.schema.ValidationError
import smirnov.oleg.json.schema.base.KEY

@Suppress("unused")
class JsonSchemaRequiredValidationTest : FunSpec() {
  init {
    val schema = JsonSchema.fromDescription(
      """
      {
        "${KEY}schema": "http://json-schema.org/draft-07/schema#",
        "required": ["prop1", "prop2"]
      }
      """.trimIndent(),
    )

    listOf(
      buildJsonObject {
        put("prop1", JsonPrimitive("a"))
        put("prop2", JsonPrimitive("b"))
      },
      buildJsonObject {
        put("prop1", JsonPrimitive("a"))
        put("prop2", JsonPrimitive("b"))
        put("prop3", JsonPrimitive("c"))
      },
    ).forEach {
      test("object with props ${it.keys} passes validation") {
        val errors = mutableListOf<ValidationError>()
        val valid = schema.validate(it, errors::add)
        it.asClue {
          valid shouldBe true
          errors shouldHaveSize 0
        }
      }
    }

    listOf(
      buildJsonObject {
        put("prop1", JsonPrimitive("a"))
      } to listOf("prop2"),
      buildJsonObject {
        put("prop2", JsonPrimitive("a"))
      } to listOf("prop1"),
      buildJsonObject {
      } to listOf("prop1", "prop2"),
    ).forEach { (jsonObject, missingProps) ->
      test("object with props ${jsonObject.keys} fails validation") {
        val errors = mutableListOf<ValidationError>()
        val valid = schema.validate(jsonObject, errors::add)
        jsonObject.asClue {
          valid shouldBe false
          errors.shouldContainExactly(
            ValidationError(
              schemaPath = JsonPointer("/required"),
              objectPath = JsonPointer.ROOT,
              message = "missing required properties: $missingProps",
            ),
          )
        }
      }
    }

    notAnObjectPasses(schema)
  }
}