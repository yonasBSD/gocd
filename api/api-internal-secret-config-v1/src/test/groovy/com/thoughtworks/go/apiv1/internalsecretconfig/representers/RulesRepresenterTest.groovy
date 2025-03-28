/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.apiv1.internalsecretconfig.representers

import com.thoughtworks.go.config.rules.Allow
import com.thoughtworks.go.config.rules.Deny
import com.thoughtworks.go.config.rules.Rules
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson

class RulesRepresenterTest {

  @Nested
  class toJSON {
    @Test
    void shouldSerializeRules() {
      Rules rules = new Rules()
      rules.add(new Allow("refer", "PipelineGroup", "DeployPipelines"))
      rules.add(new Allow("view", "Environment", "DeployEnvironment"))
      rules.add(new Deny("refer", "PipelineGroup", "TestPipelines"))
      rules.add(new Deny("view", "Environment", "TestEnvironment"))

      def json = toArrayString({ RulesRepresenter.toJSON(it, rules) })

      assertThatJson(json).isEqualTo(
        [
          [
            directive: "allow",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "DeployPipelines"
          ],
          [
            directive: "allow",
            action   : "view",
            resource : "DeployEnvironment",
            type     : "Environment"
          ],
          [
            directive: "deny",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "TestPipelines"
          ],
          [
            directive: "deny",
            action   : "view",
            resource : "TestEnvironment",
            type     : "Environment"
          ]
        ]
      )
    }

    @Test
    void shouldSerializeRulesWithErrors() {
      def allow = new Allow("refer", "PipelineGroup", "DeployPipelines")
      allow.addError("type", "Type must be one of [pipeline_group]")

      Rules rules = new Rules(allow)

      def json = toArrayString({ RulesRepresenter.toJSON(it, rules) })

      assertThatJson(json).isEqualTo(
        [
          [
            "errors" : [
              "type": ["Type must be one of [pipeline_group]"]
            ],
            directive: "allow",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "DeployPipelines"
          ]
        ]
      )
    }

    @Test
    void shouldSerializeEmptyRules() {
      def json = toArrayString({ RulesRepresenter.toJSON(it, new Rules()) })

      assertThatJson(json).isEqualTo("[]")
    }

    @Test
    void shouldSerializeUnknownRule() {
      Rules rules = new Rules()
      rules.add(new RulesRepresenter.Unknown("invalid", "refer", "pipeline_group", "*"))
      def json = toArrayString({ RulesRepresenter.toJSON(it, rules) })

      assertThatJson(json).isEqualTo([
        [
          "errors"   : [
            "directive": [
              "Invalid directive, must be either 'allow' or 'deny'."
            ]
          ],
          "directive": "invalid",
          "action"   : "refer",
          "type"     : "pipeline_group",
          "resource" : "*"
        ]
      ])
    }
  }
}
