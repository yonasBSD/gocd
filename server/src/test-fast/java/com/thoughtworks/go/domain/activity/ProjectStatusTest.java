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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.util.Dates;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectStatusTest {
    @Test
    public void shouldGetCcTrayStatusXml() {
        String projectName = "projectName";
        String activity = "Building";
        String lastBuildStatus = "Success";
        String lastBuildLabel = "LastBuildLabel";
        Date lastBuildTime = new Date();
        String webUrl = "weburl";
        String contextPath = "http://localhost/go";

        ProjectStatus projectStatus = new ProjectStatus(projectName, activity, lastBuildStatus, lastBuildLabel,
                lastBuildTime, webUrl);

        Element element = projectStatus.ccTrayXmlElement(contextPath);

        assertThat(element.getName()).isEqualTo("Project");
        assertThat(element.getAttributeValue("name")).isEqualTo(projectName);
        assertThat(element.getAttributeValue("activity")).isEqualTo(activity);
        assertThat(element.getAttributeValue("lastBuildStatus")).isEqualTo(lastBuildStatus);
        assertThat(element.getAttributeValue("lastBuildLabel")).isEqualTo(lastBuildLabel);
        assertThat(element.getAttributeValue("lastBuildTime")).isEqualTo(Dates.formatIso8601ForCCTray(lastBuildTime));
        assertThat(element.getAttributeValue("webUrl")).isEqualTo(contextPath + "/" + webUrl);
    }

    @Test
    public void shouldListViewers() {
        Users viewers = mock(Users.class);

        ProjectStatus status = new ProjectStatus("name", "activity", "web-url");
        status.updateViewers(viewers);

        assertThat(status.viewers()).isEqualTo(viewers);
    }

    @Test
    public void shouldProvideItsXmlRepresentation_WhenThereAreNoBreakers() {
        ProjectStatus status = new ProjectStatus("name", "activity1", "build-status-1", "build-label-1",
                Dates.parseRFC822("Sun, 23 May 2010 10:00:00 +0200"), "web-url");

        assertThat(status.xmlRepresentation()).isEqualTo("<Project name=\"name\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" " +
                        "lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"__SITE_URL_PREFIX__/web-url\" />");
    }

    @Test
    public void shouldProvideItsXmlRepresentation_WhenThereAreBreakers() {
        ProjectStatus status = new ProjectStatus("name", "activity1", "build-status-1", "build-label-1",
                Dates.parseRFC822("Sun, 23 May 2010 10:00:00 +0200"), "web-url", new LinkedHashSet<>(List.of("breaker1", "breaker2")));

        assertThat(status.xmlRepresentation()).isEqualTo("<Project name=\"name\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" " +
                        "lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"__SITE_URL_PREFIX__/web-url\">" +
                        "<messages><message text=\"breaker1, breaker2\" kind=\"Breakers\" /></messages></Project>");
    }

    @Test
    public void shouldAlwaysHaveEmptyStringAsXMLRepresentationOfANullProjectStatus() {
        assertThat(new ProjectStatus.NullProjectStatus("some-name").xmlRepresentation()).isEqualTo("");
        assertThat(new ProjectStatus.NullProjectStatus("some-other-name").xmlRepresentation()).isEqualTo("");
    }

    @Test
    public void shouldNotBeViewableByAnyoneTillViewersAreUpdated() {
        ProjectStatus status = new ProjectStatus("name", "activity", "web-url");

        assertThat(status.canBeViewedBy("abc")).isFalse();
        assertThat(status.canBeViewedBy("def")).isFalse();

        status.updateViewers(new AllowedUsers(Set.of("abc", "ghi"), Collections.emptySet()));

        assertThat(status.canBeViewedBy("abc")).isTrue();
        assertThat(status.canBeViewedBy("def")).isFalse();
        assertThat(status.canBeViewedBy("ghi")).isTrue();
    }
}
