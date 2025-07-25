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
package com.thoughtworks.go.server.service;

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RailsAssetsServiceTest {

    RailsAssetsService railsAssetsService;
    @TempDir
    File assetsDir;
    private ServletContext context;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    public void setup() {
        context = mock(ServletContext.class);
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        railsAssetsService = new RailsAssetsService(systemEnvironment);
        railsAssetsService.setServletContext(context);
    }

    @Test
    public void shouldNotInitializeAssetManifestWhenUsingRails4InDevelopmentMode() throws IOException {
        when(systemEnvironment.useCompressedJs()).thenReturn(false);
        railsAssetsService = new RailsAssetsService(systemEnvironment);
        railsAssetsService.setServletContext(context);
        railsAssetsService.initialize();
        assertThat(railsAssetsService.getRailsAssetsManifest()).isNull();
    }

    @Test
    public void shouldGetAssetPathFromManifestJson() throws IOException {
        Files.writeString(new File(assetsDir, ".sprockets-manifest-digest.json").toPath(), json, UTF_8);
        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath(any(String.class))).thenReturn(assetsDir.getAbsolutePath());
        railsAssetsService.initialize();

        assertThat(railsAssetsService.getAssetPath("application.js")).isEqualTo("assets/application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js");
        assertThat(railsAssetsService.getAssetPath("junk.js")).isNull();
    }

    @Test
    public void shouldThrowExceptionIfManifestFileIsNotFoundInAssetsDir() {
        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath(any(String.class))).thenReturn(assetsDir.getAbsolutePath());
        try {
            railsAssetsService.initialize();
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Manifest json file was not found at " + assetsDir.getAbsolutePath());
        }
    }

    @Test
    public void shouldThrowExceptionIfAssetsDirDoesNotExist() {
        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath(any(String.class))).thenReturn("DoesNotExist");
        try {
            railsAssetsService.initialize();
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Assets directory does not exist DoesNotExist");
        }
    }

    @Test
    public void shouldNotIncludeDigestPathInDevelopmentEnvironment() throws IOException {
        when(systemEnvironment.useCompressedJs()).thenReturn(false);
        railsAssetsService.initialize();
        assertThat(railsAssetsService.getAssetPath("junk.js")).isEqualTo("assets/junk.js");
    }

    @Test
    public void shouldHaveAssetsAsTheSerializedNameForAssetsMapInRailsAssetsManifest_ThisIsRequiredSinceManifestFileGeneratedBySprocketsHasAMapOfAssetsWhichThisServiceNeedsAccessTo() {
        List<Field> fields = new ArrayList<>(List.of(RailsAssetsService.RailsAssetsManifest.class.getDeclaredFields()));
        List<Field> fieldsAnnotatedWithSerializedNameAsAssets = fields.stream().filter(field -> {
            if (field.isAnnotationPresent(SerializedName.class)) {
                SerializedName annotation = field.getAnnotation(SerializedName.class);
                return annotation.value().equals("assets");
            }
            return false;
        }).toList();
        assertThat(fieldsAnnotatedWithSerializedNameAsAssets.isEmpty()).isFalse();
        assertThat(fieldsAnnotatedWithSerializedNameAsAssets.size()).isEqualTo(1);
        assertThat(fieldsAnnotatedWithSerializedNameAsAssets.get(0).getType().getCanonicalName()).isEqualTo(Map.class.getCanonicalName());
    }

    private String json = """
        {
            "files": {
                "application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js": {
                    "logical_path": "application.js",
                    "mtime": "2014-08-26T12:39:43+05:30",
                    "size": 1091366,
                    "digest": "bfdbd4fff63b0cd45c50ce7a79fe0f53"
                },
                "application-4b25c82f986c0bef78151a4ab277c3e4.css": {
                    "logical_path": "application.css",
                    "mtime": "2014-08-26T13:45:30+05:30",
                    "size": 513,
                    "digest": "4b25c82f986c0bef78151a4ab277c3e4"
                }
            },
            "assets": {
                "application.js": "application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js",
                "application.css": "application-4b25c82f986c0bef78151a4ab277c3e4.css"
            }
        }""";


}
