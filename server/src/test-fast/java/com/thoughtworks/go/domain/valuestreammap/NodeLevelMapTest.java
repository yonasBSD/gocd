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
package com.thoughtworks.go.domain.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeLevelMapTest {

    @Test
    public void shouldGetNodeLevelsList() {
        NodeLevelMap nodeLevelMap = new NodeLevelMap();
        Node svn = new SCMDependencyNode("svn-fingerprint", "svn", "svn");
        svn.setLevel(-1);
        Node current = new PipelineDependencyNode(new CaseInsensitiveString("current"),"current");
        current.setLevel(0);
        Node p1 = new PipelineDependencyNode(new CaseInsensitiveString("p1"), "p1");
        p1.setLevel(1);
        Node p2 = new PipelineDependencyNode(new CaseInsensitiveString("p2"), "p2");
        p2.setLevel(1);

        svn.addEdge(current);
        current.addEdge(p1);
        current.addEdge(p2);
        current.addEdge(p2);

        nodeLevelMap.add(svn);
        nodeLevelMap.add(p1);
        nodeLevelMap.add(p2);
        nodeLevelMap.add(current);

        List<List<Node>> nodeLevels = nodeLevelMap.nodesAtEachLevel();
        assertThat(nodeLevels.size()).isEqualTo(3);
        assertThat(nodeLevels.get(0)).contains(svn);
        assertThat(nodeLevels.get(1)).contains(current);
        assertThat(nodeLevels.get(2)).contains(p1, p2);
    }
}
