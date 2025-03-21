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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.ConsoleConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ConsoleOutViewTest {
    private static final List<String> CHARSETS = List.of("utf-8", "utf-16", "ISO-8859-7", "IBM00858", "KOI8-R", "US-ASCII");

    private Charset randomCharset;

    @Test
    public void setsUpContentEncoding() throws Exception {
        ConsoleOutView view = new ConsoleOutView(mock(ConsoleConsumer.class), randomCharset());

        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(null, null, response);
        assertThat(response.getCharacterEncoding()).isEqualTo(randomCharset().toString());
    }

    @Test
    public void setsUpCharset() throws Exception {
        ConsoleOutView view = new ConsoleOutView(mock(ConsoleConsumer.class), randomCharset());

        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(null, null, response);
        assertThat(response.getContentType()).isEqualTo(view.getContentType());
    }

    @Test
    public void getsContentType() {
        ConsoleOutView view = new ConsoleOutView(null, randomCharset());
        assertThat(view.getContentType()).isEqualTo("text/plain; charset=" + randomCharset());
    }

    private Charset randomCharset() {
        if (randomCharset == null) {
            List<String> charsets = new ArrayList<>(CHARSETS);
            Collections.shuffle(charsets);
            randomCharset = Charset.forName(charsets.get(new Random().nextInt(charsets.size())));
        }

        return randomCharset;
    }
}
