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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.newsecurity.handlers.ResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Set;

import static com.thoughtworks.go.server.security.GoAuthority.*;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(ClearSingleton.class)
public class VerifyAuthorityFilterTest {

    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ResponseHandler responseHandler;

    @BeforeEach
    void setUp() {
        request = HttpRequestBuilder.GET("/foo").build();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        responseHandler = mock(ResponseHandler.class);
    }

    @Nested
    class AuthenticatedRequest {
        @Test
        void shouldCallResponseHandlerWhenUserDoesNotHaveAuthorityToAccessResource() throws Exception {
            SessionUtilsHelper.setCurrentUser(request, "foo", ROLE_ANONYMOUS.asAuthority(), ROLE_USER.asAuthority());

            new VerifyAuthorityFilter(Set.of(ROLE_AGENT.asAuthority()), responseHandler)
                    .doFilter(request, response, filterChain);

            verify(responseHandler).handle(request, response, SC_FORBIDDEN, "You are not authorized to access this resource!");
            verifyNoInteractions(filterChain);
        }

        @Test
        void shouldContinueIfAnyAuthorityInSessionMatches() throws Exception {
            SessionUtilsHelper.setCurrentUser(request, "foo", ROLE_GROUP_SUPERVISOR.asAuthority(), ROLE_TEMPLATE_SUPERVISOR.asAuthority());

            new VerifyAuthorityFilter(Set.of(ROLE_TEMPLATE_SUPERVISOR.asAuthority()), responseHandler)
                    .doFilter(request, response, filterChain);

            verifyNoInteractions(responseHandler);
            assertThat(response.getStatus()).isEqualTo(200);
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class AnonymouslyAuthenticatedRequest {
        @Test
        void shouldSend401WhenRequestIsNotAuthenticated() throws ServletException, IOException {
            SessionUtilsHelper.setCurrentUser(request, "anonymous", ROLE_ANONYMOUS.asAuthority());
            new VerifyAuthorityFilter(Set.of(ROLE_AGENT.asAuthority()), responseHandler)
                    .doFilter(request, response, filterChain);

            verify(responseHandler).handle(request, response, SC_UNAUTHORIZED, "You are not authenticated!");
            verifyNoInteractions(filterChain);
        }
    }
}
