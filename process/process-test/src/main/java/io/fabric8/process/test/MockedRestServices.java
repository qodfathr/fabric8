/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.process.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import java.util.HashMap;
import java.util.Map;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockedRestServices {

    private Map<String, ClientAndServer> mockServers = new HashMap<>();

    private final MockRestServiceDirectory[] mockRestServiceDirectories;

    private final ObjectMapper jackson = new ObjectMapper();

    public MockedRestServices(MockRestServiceDirectory... mockRestServiceDirectories) {
        this.mockRestServiceDirectories = mockRestServiceDirectories;
    }

    public MockedRestServices() {
        this(new SystemPropertiesMockRestServiceDirectory());
    }

    public void stop() {
        for (ClientAndServer server : mockServers.values()) {
            server.stop();
        }
    }

    public Configure symbol(String serviceSymbol) {
        ClientAndServer server = mockServers.get(serviceSymbol);
        if (server == null) {
            int port = PortFactory.findFreePort();
            server = new ClientAndServer(port);
            mockServers.put(serviceSymbol, server);
            for (MockRestServiceDirectory directory : mockRestServiceDirectories) {
                directory.publish(serviceSymbol, "http://localhost:" + port);
            }
        }
        return new Configure(server);
    }

    class Configure {

        private final MockServerClient server;

        Configure(MockServerClient server) {
            this.server = server;
        }

        public MockServerClient configure() {
            return server;
        }

        public Configure json(Object pojo) {
            try {
                server.when(request()).respond(response().withBody(jackson.writeValueAsString(pojo)));
                return this;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public Configure json(String pathFragment, Object pojo) {
            try {
                server.when(request().withPath(".*?" + pathFragment + ".*")).respond(response().withBody(jackson.writeValueAsString(pojo)));
                return this;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
