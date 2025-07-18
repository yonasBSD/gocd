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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RakeTaskTest {
    @Test
    public void shouldReturnEmptyStringForDefault() {
        RakeTask rakeTask = new RakeTask();
        assertThat(rakeTask.arguments()).isEqualTo("");
    }

    @Test
    public void shouldContainBuildFileWhenDefined() {
        RakeTask rakeTask = new RakeTask();
        rakeTask.setBuildFile("myrakefile.rb");
        assertThat(rakeTask.arguments()).isEqualTo("-f \"myrakefile.rb\"");
    }

    @Test
    public void shouldContainBuildFileAndTargetWhenBothDefined() {
        RakeTask rakeTask = new RakeTask();
        rakeTask.setBuildFile("myrakefile.rb");
        rakeTask.setTarget("db:migrate VERSION=0");
        assertThat(rakeTask.arguments()).isEqualTo("-f \"myrakefile.rb\" db:migrate VERSION=0");
    }

    @Test
    public void shouldUseRakeFileFromAnyDirectoryUnderRoot() {
        RakeTask rakeTask = new RakeTask();
        String rakeFile = "build/myrakefile.rb";

        rakeTask.setBuildFile(rakeFile);
        rakeTask.setTarget("db:migrate VERSION=0");
        assertThat(rakeTask.arguments()).isEqualTo("-f \"" + rakeFile + "\" db:migrate VERSION=0");
    }

    @Test
    public void describeTest() {
        RakeTask rakeTask = new RakeTask();
        rakeTask.setBuildFile("myrakefile.rb");
        rakeTask.setTarget("db:migrate VERSION=0");
        rakeTask.setWorkingDirectory("lib");

        assertThat(rakeTask.describe()).isEqualTo("rake -f \"myrakefile.rb\" db:migrate VERSION=0 (workingDirectory: lib)");
    }

    @Test
    public void shouldShowCommandName() {
        assertThat(new RakeTask().command()).isEqualTo("rake");
    }

    @Test
    public void shouldGiveArgumentsForRakeTask() {
        RakeTask rakeTask = new RakeTask();
        rakeTask.setBuildFile("myrakefile.rb");
        rakeTask.setTarget("db:migrate VERSION=0");
        assertThat(rakeTask.arguments()).isEqualTo("-f \"myrakefile.rb\" db:migrate VERSION=0");
    }
}
