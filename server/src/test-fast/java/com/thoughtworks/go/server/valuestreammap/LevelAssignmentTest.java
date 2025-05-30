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
package com.thoughtworks.go.server.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.valuestreammap.*;
import com.thoughtworks.go.helper.ModificationsMother;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LevelAssignmentTest {

    @Test
    public void shouldAssignLevelsForUpstreamNodesOfCurrentPipeline() {

        /*
        ---> p1 ---
       /            \
       g -----------p3
       \            /
        ---> p2 ---
        */

        CaseInsensitiveString current = new CaseInsensitiveString("p3");
        CaseInsensitiveString p1name = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2name = new CaseInsensitiveString("p2");
        Node p1 = new PipelineDependencyNode(p1name, p1name.toString());
        Node p2 = new PipelineDependencyNode(p2name, p2name.toString());
        Node gitNode = new SCMDependencyNode("git", "g", "git");

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, new PipelineRevision(current.toString(), 1, "1"));
        valueStreamMap.addUpstreamNode(p1, new PipelineRevision(p1name.toString(), 1, "1"), current);
        valueStreamMap.addUpstreamNode(p2, new PipelineRevision(p2name.toString(), 1, "1"), current);
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("trunk"), p1name, new MaterialRevision(null));
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("main-branch"), new CaseInsensitiveString("p3"), new MaterialRevision(null));
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("main-branch"), p2name, new MaterialRevision(null));
        NodeLevelMap levelToNodeMap = new LevelAssignment().apply(valueStreamMap);

        assertThat(valueStreamMap.getCurrentPipeline().getLevel()).isEqualTo(0);
        assertThat(p1.getLevel()).isEqualTo(-1);
        assertThat(p2.getLevel()).isEqualTo(-1);
        assertThat(gitNode.getLevel()).isEqualTo(-2);

        assertThat(levelToNodeMap.get(0)).contains(valueStreamMap.getCurrentPipeline());
        assertThat(levelToNodeMap.get(-1)).contains(p1, p2);
        assertThat(levelToNodeMap.get(-2)).contains(gitNode);
    }

    @Test
    public void shouldAssignLevelsForDownstreamNodesOfCurrentPipeline() {

    	/*
                ---> p1 ----
               /            \
    	git --> p ----------> p3
              \            /
               ---> p2 ----
    	*/

        CaseInsensitiveString current = new CaseInsensitiveString("p");
        CaseInsensitiveString p1name = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2name = new CaseInsensitiveString("p2");
        CaseInsensitiveString p3name = new CaseInsensitiveString("p3");
        Node p1 = new PipelineDependencyNode(p1name, p1name.toString());
        Node p2 = new PipelineDependencyNode(p2name, p2name.toString());
        Node p3 = new PipelineDependencyNode(p3name, p3name.toString());
        Node gitNode = new SCMDependencyNode("git", "g", "git");

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, new PipelineRevision(current.toString(), 1, "1"));
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("trunk"), new CaseInsensitiveString("p"), new MaterialRevision(null));

        valueStreamMap.addDownstreamNode(p1, current);
        valueStreamMap.addDownstreamNode(p2, current);
        valueStreamMap.addDownstreamNode(p3, p1.getId());
        valueStreamMap.addDownstreamNode(p3, p2.getId());

        NodeLevelMap levelToNodeMap = new LevelAssignment().apply(valueStreamMap);

        assertThat(valueStreamMap.getCurrentPipeline().getLevel()).isEqualTo(0);
        assertThat(gitNode.getLevel()).isEqualTo(-1);
        assertThat(p1.getLevel()).isEqualTo(1);
        assertThat(p2.getLevel()).isEqualTo(1);
        assertThat(p3.getLevel()).isEqualTo(2);

        assertThat(levelToNodeMap.get(0)).contains(valueStreamMap.getCurrentPipeline());
        assertThat(levelToNodeMap.get(-1)).contains(gitNode);
        assertThat(levelToNodeMap.get(1)).contains(p1, p2);
        assertThat(levelToNodeMap.get(2)).contains(p3);
    }

	@Test
	public void shouldAssignLevelsForDownstreamNodesOfCurrentMaterial() {

		/*
				---> p1 ----
			   /            \
		git --> p ----------> p3
			  \            /
			   ---> p2 ----
		*/

		Node p = new PipelineDependencyNode(new CaseInsensitiveString("p"), "p");
		Node p1 = new PipelineDependencyNode(new CaseInsensitiveString("p1"), "p1");
		Node p2 = new PipelineDependencyNode(new CaseInsensitiveString("p2"), "p2");
		Node p3 = new PipelineDependencyNode(new CaseInsensitiveString("p3"), "p3");

		GitMaterial material = new GitMaterial("url");
		Modification modification = ModificationsMother.aCheckIn("1");
		ValueStreamMap valueStreamMap = new ValueStreamMap(material, null, modification);
		Node gitNode = valueStreamMap.getCurrentMaterial();

		valueStreamMap.addDownstreamNode(p, gitNode.getId());
		valueStreamMap.addDownstreamNode(p1, p.getId());
		valueStreamMap.addDownstreamNode(p2, p.getId());
		valueStreamMap.addDownstreamNode(p3, p1.getId());
		valueStreamMap.addDownstreamNode(p3, p2.getId());

		NodeLevelMap levelToNodeMap = new LevelAssignment().apply(valueStreamMap);

		assertThat(valueStreamMap.getCurrentMaterial().getLevel()).isEqualTo(0);
		assertThat(p.getLevel()).isEqualTo(1);
		assertThat(p1.getLevel()).isEqualTo(2);
		assertThat(p2.getLevel()).isEqualTo(2);
		assertThat(p3.getLevel()).isEqualTo(3);

		assertThat(levelToNodeMap.get(0)).contains(valueStreamMap.getCurrentMaterial());
		assertThat(levelToNodeMap.get(1)).contains(p);
		assertThat(levelToNodeMap.get(2)).contains(p1, p2);
		assertThat(levelToNodeMap.get(3)).contains(p3);
	}
}
