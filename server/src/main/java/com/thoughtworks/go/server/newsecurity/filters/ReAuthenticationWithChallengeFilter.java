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

import com.thoughtworks.go.server.newsecurity.handlers.BasicAuthenticationWithChallengeFailureResponseHandler;
import com.thoughtworks.go.server.newsecurity.providers.AnonymousAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Component
public class ReAuthenticationWithChallengeFilter extends AbstractReAuthenticationFilter {
    private final BasicAuthenticationWithChallengeFailureResponseHandler basicAuthenticationWithChallengeFailureResponseHandler;

    @Autowired
    public ReAuthenticationWithChallengeFilter(SecurityService securityService,
                                               SystemEnvironment systemEnvironment,
                                               Clock clock,
                                               BasicAuthenticationWithChallengeFailureResponseHandler basicAuthenticationWithChallengeFailureResponseHandler,
                                               PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider,
                                               WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider,
                                               AnonymousAuthenticationProvider anonymousAuthenticationProvider) {
        super(securityService, systemEnvironment, clock, passwordBasedPluginAuthenticationProvider, webBasedPluginAuthenticationProvider, anonymousAuthenticationProvider);
        this.basicAuthenticationWithChallengeFailureResponseHandler = basicAuthenticationWithChallengeFailureResponseHandler;
    }

    @Override
    protected void onAuthenticationFailure(HttpServletRequest request,
                                           HttpServletResponse response,
                                           String errorMessage) throws IOException {
        basicAuthenticationWithChallengeFailureResponseHandler.handle(request, response, SC_UNAUTHORIZED, errorMessage);
    }
}

