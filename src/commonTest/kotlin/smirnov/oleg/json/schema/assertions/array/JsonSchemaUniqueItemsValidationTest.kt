package smirnov.oleg.json.schema.assertions.array

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import smirnov.oleg.json.pointer.JsonPointer
import smirnov.oleg.json.schema.JsonSchema
import smirnov.oleg.json.schema.ValidationError
import smirnov.oleg.json.schema.base.KEY

@Suppress("unused")
class JsonSchemaUniqueItemsValidationTest : FunSpec() {
  init {
    val validationEnabled = JsonSchema.fromDescription(
      """
      {
        "${KEY}schema": "http://json-schema.org/draft-07/schema#",
        "uniqueItems": true
      }
      """.trimIndent(),
    )

    val validationDisabled = JsonSchema.fromDescription(
      """
      {
        "${KEY}schema": "http://json-schema.org/draft-07/schema#",
        "uniqueItems": false
      }
      """.trimIndent(),
    )

    listOf(
      buildJsonArray { },
      buildJsonArray {
        add(JsonPrimitive("a"))
      },
      buildJsonArray {
        add(JsonPrimitive("a"))
        add(JsonPrimitive("b"))
      },
      buildJsonArray {
        add(JsonPrimitive("a"))
        add(JsonPrimitive("b"))
        add(
          buildJsonArray {
            add(JsonPrimitive("a"))
          },
        )
      },
      buildJsonArray {
        add(JsonPrimitive("a"))
        add(JsonPrimitive("b"))
        add(
          buildJsonArray {
            add(JsonPrimitive("a"))
          },
        )
        add(
          buildJsonObject {
            put("test", JsonPrimitive("a"))
          },
        )
      },
    ).forEach {
      test("array with unique items passes enabled validation: $it") {
        val errors = mutableListOf<ValidationError>()
        val valid = validationEnabled.validate(it, errors::add)
        it.asClue {
          valid shouldBe true
          errors shouldHaveSize 0
        }
      }

      test("array with unique items passes disabled validation: $it") {
        val errors = mutableListOf<ValidationError>()
        val valid = validationDisabled.validate(it, errors::add)
        it.asClue {
          valid shouldBe true
          errors shouldHaveSize 0
        }
      }
    }

    listOf(
      buildJsonArray {
        add(JsonPrimitive("a"))
        add(JsonPrimitive("a"))
      },
      buildJsonArray {
        add(
          buildJsonArray {
            add(JsonPrimitive("a"))
          },
        )
        add(
          buildJsonArray {
            add(JsonPrimitive("a"))
          },
        )
      },
      buildJsonArray {
        add(
          buildJsonObject {
            put("test", JsonPrimitive("a"))
          },
        )
        add(
          buildJsonObject {
            put("test", JsonPrimitive("a"))
          },
        )
      },
    ).forEach { array ->
      test("array with duplicate items fails enabled validation: $array") {
        val errors = mutableListOf<ValidationError>()
        val valid = validationEnabled.validate(array, errors::add)
        array.asClue {
          valid shouldBe false
          errors.shouldContainExactly(
            ValidationError(
              schemaPath = JsonPointer("/uniqueItems"),
              objectPath = JsonPointer.ROOT,
              message = "array contains duplicate values: ${it.toSet()}",
            ),
          )
        }
      }

      test("array with duplicate items passes disabled validation: $array") {
        val errors = mutableListOf<ValidationError>()
        val valid = validationDisabled.validate(array, errors::add)
        array.asClue {
          valid shouldBe true
          errors shouldHaveSize 0
        }
      }
    }

    listOf(
      JsonPrimitive("test"),
      JsonPrimitive(42),
      JsonPrimitive(42.5),
      JsonPrimitive(true),
      JsonNull,
      buildJsonObject { },
    ).forEach {
      test("not array $it passes validation") {
        val errors = mutableListOf<ValidationError>()
        val valid = validationEnabled.validate(it, errors::add)

        valid shouldBe true
        errors shouldHaveSize 0
      }
    }

    listOf(
      JsonPrimitive(42.5),
      JsonPrimitive(42),
      JsonPrimitive("test"),
      JsonNull,
      buildJsonObject { },
      buildJsonArray { },
    ).forEach {
      test("reports not valid boolean value $it") {
        shouldThrow<IllegalArgumentException> {
          JsonSchema.fromDescription(
            """
            {
              "${KEY}schema": "http://json-schema.org/draft-07/schema#",
              "uniqueItems": $it
            }
            """.trimIndent(),
          )
        }.message shouldBe "uniqueItems must be a boolean"
      }
    }
  }
}