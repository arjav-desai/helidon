/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.webserver.staticcontent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import io.helidon.common.testing.http.junit5.HttpHeaderMatcher;
import io.helidon.common.testing.junit5.OptionalMatcher;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.WebServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

class StaticContentConfigTest {
    private static Http1Client testClient;
    private static WebServer server;
    private static Path tmpPath;

    @BeforeAll
    static void setUp() throws IOException {
        // current directory
        Path path = Paths.get(".");
        path = path.resolve("target/helidon/tmp");
        // we need to have this file ready
        Files.createDirectories(path);
        tmpPath = path;

        Config config = Config.just(ConfigSources.classpath("/config-unit-test-1.yaml"));
        server = WebServer.builder()
                .config(config.get("server"))
                .port(0)
                .build()
                .start();

        testClient = Http1Client.builder()
                .baseUri("http://localhost:" + server.port())
                .shareConnectionCache(false)
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testClasspathFavicon() {
        try (Http1ClientResponse response = testClient.get("/classpath/favicon.ico")
                .request()) {

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers(), HttpHeaderMatcher.hasHeader(HeaderNames.CONTENT_TYPE, "image/x-icon"));
        }
    }

    @Test
    void testClasspathNested() {
        try (Http1ClientResponse response = testClient.get("/classpath/nested/resource.txt")
                .request()) {

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers(), HttpHeaderMatcher.hasHeader(HeaderNames.CONTENT_TYPE, "text/plain"));
            assertThat(response.as(String.class), is("Nested content"));
        }
    }

    @Test
    void testClasspathFromJar() throws IOException {
        String serviceName = "io.helidon.webserver.testing.junit5.spi.ServerJunitExtension";
        ClientResponseTyped<String> response = testClient.get("/jar/" + serviceName)
                .request(String.class);
        assertThat(response.status(), is(Status.OK_200));
        assertThat(response.headers(), HttpHeaderMatcher.hasHeader(HeaderNames.CONTENT_TYPE, "application/octet-stream"));
        assertThat(response.entity(), startsWith("# This file was generated by Helidon services Maven plugin."));

        // when run in maven, we have jar, but from IDE we get a file, so we have to check it correctly
        URL resource = Thread.currentThread().getContextClassLoader().getResource("META-INF/services/" + serviceName);
        assertThat(resource, notNullValue());
        if (resource.getProtocol().equals("jar")) {
            // we can validate the temporary file exists and is correct
            try (var stream = Files.list(tmpPath)) {
                Optional<Path> tmpFile = stream.findAny();

                assertThat("There should be a single temporary file created in " + tmpPath,
                           tmpFile,
                           OptionalMatcher.optionalPresent());
                String fileName = tmpFile.get().getFileName().toString();
                assertThat(fileName, startsWith("helidon-custom"));
                assertThat(fileName, endsWith(".cache"));
            }
        }
    }

    @Test
    void testClasspathSingleFile() {
        try (Http1ClientResponse response = testClient.get("/singleclasspath")
                .request()) {

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers(), HttpHeaderMatcher.hasHeader(HeaderNames.CONTENT_TYPE, "text/plain"));
            assertThat(response.as(String.class), is("Content"));
        }
    }

    @Test
    void testFileSystemFavicon() {
        try (Http1ClientResponse response = testClient.get("/path/favicon.ico")
                .request()) {

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers(), HttpHeaderMatcher.hasHeader(HeaderNames.CONTENT_TYPE, "image/my-icon"));
        }
    }

    @Test
    void testFileSystemNested() {
        try (Http1ClientResponse response = testClient.get("/path/nested/resource.txt")
                .request()) {

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers(), HttpHeaderMatcher.hasHeader(HeaderNames.CONTENT_TYPE, "text/plain"));
            assertThat(response.as(String.class), is("Nested content"));
        }
    }

    @Test
    void testFileSystemSingleFile() {
        try (Http1ClientResponse response = testClient.get("/singlepath")
                .request()) {

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers(), HttpHeaderMatcher.hasHeader(HeaderNames.CONTENT_TYPE, "text/plain"));
            assertThat(response.as(String.class), is("Content"));
        }
    }
}
