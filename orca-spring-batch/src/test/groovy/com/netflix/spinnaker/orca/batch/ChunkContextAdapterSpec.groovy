/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.batch

import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.StepContext
import org.springframework.batch.test.MetaDataInstanceFactory
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ChunkContextAdapterSpec extends Specification {

  @Unroll
  def "traverses the hierarchy of contexts to retrieve values"() {
    given:
    def jobParametersBuilder = new JobParametersBuilder()
    if (jobParamValue) {
      jobParametersBuilder.addString(key, jobParamValue)
    }
    def stepExecution = MetaDataInstanceFactory.createStepExecution(jobParametersBuilder.toJobParameters())
    def stepExecutionContext = stepExecution.executionContext
    def jobExecutionContext = stepExecution.jobExecution.executionContext

    and:
    if (jobContextValue) {
      jobExecutionContext.put(key, jobContextValue)
    }
    if (stepContextValue) {
      stepExecutionContext.put(key, stepContextValue)
    }

    and:
    def stepContext = new StepContext(stepExecution)
    def chunkContext = new ChunkContext(stepContext)

    and:
    @Subject context = new ChunkContextAdapter(chunkContext)

    expect:
    context.inputs[key] == expected

    where:
    stepContextValue | jobContextValue | jobParamValue || expected
    "step"           | null            | null          || "step"
    null             | "job"           | null          || "job"
    null             | null            | "param"       || "param"
    null             | null            | null          || null
    "step"           | "job"           | "param"       || "step"
    null             | "job"           | "param"       || "job"

    and:
    key = "foo"
  }

  def "can get a subset of the inputs based on a prefix"() {
    given:
    def stepExecution = MetaDataInstanceFactory.createStepExecution()
    def stepContext = new StepContext(stepExecution)
    def chunkContext = new ChunkContext(stepContext)

    and:
    stepExecution.executionContext.put("foo.a", "a")
    stepExecution.executionContext.put("foo.b", "b")
    stepExecution.executionContext.put("bar.c", "c")
    stepExecution.executionContext.put("bar.d", "d")

    when:
    @Subject context = new ChunkContextAdapter(chunkContext)

    then:
    context.getInputs("foo") == [a: "a", b: "b"]
    context.getInputs("bar") == [c: "c", d: "d"]
  }

}
