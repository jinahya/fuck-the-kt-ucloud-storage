/*
 * Copyright 2016 Jin Kwon &lt;onacit_at_gmail.com&gt;.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jinahya.kt.ucloud.storage.sucks;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
public class KtUcloudStorageIT {

    private static final Logger logger = getLogger(KtUcloudStorageIT.class);

    private static String authUrl;

    private static String authUser;

    private static String resellerAccount;

    private static String authKey;

    private static final int INVOCATION_COUNT = 32;

    @BeforeClass
    public static void doBeforeClass() {
        authUrl = System.getProperty("authUrl");
        if (authUrl == null) {
            logger.error("missing property; authUrl; skipping...");
            throw new SkipException("missing property; authUrl");
        }
        logger.debug("authUrl: {}", authUrl);
        authUser = System.getProperty("authUser");
        if (authUser == null) {
            logger.error("missing property; authUser; skipping...");
            throw new SkipException("missing property; authUser");
        }
        logger.debug("authUser: {}", authUser);
        {
            final int index = authUser.indexOf(':');
            if (index != -1) {
                resellerAccount = authUser.substring(0, index);
            }
        }
        authKey = System.getProperty("authKey");
        if (authKey == null) {
            logger.error("missing proprety; authKey; skipping...");
            throw new SkipException("missing property; authKey");
        }
        logger.debug("authKey: {}", authKey);
    }

    private static int statusCode(final HttpURLConnection connection,
                                  final int... expectedStatusCodes)
            throws IOException {
        final int statusCode = connection.getResponseCode();
        final String reasonPhrase = connection.getResponseMessage();
        logger.debug("status: {} {}", statusCode, reasonPhrase);
        for (final int expecedStatuscode : expectedStatusCodes) {
            if (expecedStatuscode == statusCode) {
                return statusCode;
            }
        }
        throw new RuntimeException(
                "statusCode(" + statusCode + ") not in "
                + Arrays.toString(expectedStatusCodes));
    }

    @DataProvider(name = "mediaTypes")
    private static Object[][] mediaTypes() {
        return new Object[][]{
            {"text/plain"}, {"application/xml"}, {"application/json"}
        };
    }

    @BeforeMethod
    public void authenticateUser() throws IOException {
        logger.debug("------------------------------------ authenticateUser()");
        final URL url = new URL(authUrl);
        final HttpURLConnection connection
                = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-Auth-User", authUser);
        connection.setRequestProperty("X-Auth-Key", authKey);
        connection.setDoOutput(false);
        connection.setDoInput(true);
        try {
            connection.connect();
            final int statusCode = statusCode(connection, 200);
            assertEquals(statusCode, 200);
            authToken = connection.getHeaderField("X-Auth-Token");
            logger.debug("authToken: {}", authToken);
            assertNotNull(authToken, "null authToken");
            storageUrl = connection.getHeaderField("X-Storage-Url");
            logger.debug("storageUrl: {}", storageUrl);
            assertNotNull(storageUrl, "null storageUrl");
            if (resellerAccount != null) {
                final URL u = new URL(storageUrl);
                final String protocol = u.getProtocol();
                final String authority = u.getAuthority();
                resellerUrl = protocol + "://" + authority + "/auth/v2/"
                              + resellerAccount;
                logger.debug("resellerUrl: {}", resellerUrl);
            }
        } finally {
            connection.disconnect();
        }
    }

    @Test(invocationCount = INVOCATION_COUNT)
    public void readAllContainers() throws IOException {
        logger.debug("----------------------------------- readAllContainers()");
        final int limit = 512;
        final String[] marker = new String[1];
        while (true) {
            final StringBuilder spec = new StringBuilder(storageUrl);
            spec.append('?')
                    .append("limit").append('=')
                    .append(Integer.toString(limit));
            if (marker[0] != null) {
                spec.append('&')
                        .append("marker").append('=').append(marker[0]);
            }
            marker[0] = null;
            final URL url = new URL(spec.toString());
            final HttpURLConnection connection
                    = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/plain");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            try {
                connection.connect();
                final int statusCode = statusCode(connection, 200, 204);
                if (statusCode == 204) {
                    break;
                }
                assertEquals(statusCode, 200);
                try (InputStream in = connection.getInputStream();
                     InputStreamReader r = new InputStreamReader(in, UTF_8);
                     BufferedReader b = new BufferedReader(r)) {
                    b.lines().forEach(l -> {
//                        logger.debug("container: {}", l);
                        marker[0] = l;
                    });
                }
            } finally {
                connection.disconnect();
            }
        }
    }

    @Test(dataProvider = "mediaTypes", invocationCount = INVOCATION_COUNT)
    public void readContainers(final String mediaType) throws IOException {
        logger.debug("------------------------- readContainers({})", mediaType);
        final URL url = new URL(storageUrl);
        final HttpURLConnection connection
                = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", mediaType);
        connection.setRequestProperty("X-Auth-Token", authToken);
        connection.setDoOutput(false);
        connection.setDoInput(true);
        try {
            connection.connect();
            final int statusCode = statusCode(connection, 200, 204);
            if (statusCode == 204) {
                return;
            }
            assertEquals(statusCode, 200);
            try (InputStream in = connection.getInputStream();
                 InputStreamReader r = new InputStreamReader(in, UTF_8);
                 BufferedReader b = new BufferedReader(r)) {
                b.lines().forEach(l -> {
//                    logger.debug("body: {}", l);
                });
            }
        } finally {
            connection.disconnect();
        }
    }

    // ------------------------------------------------------ /account/container
    @Test(invocationCount = INVOCATION_COUNT)
    public void verifyContainer() throws IOException {
        logger.debug("------------------------------------- verifyContainer()");
        final String container = getClass().getName() + "_" + UUID.randomUUID();
        logger.debug("container: {}", container);
        final URL url = new URL(storageUrl + "/" + container);
        {
            logger.debug("----------------------------- updating container...");
            final HttpURLConnection connection
                    = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            try {
                connection.connect();
                final int statusCode = statusCode(connection, 201, 202);
                if (statusCode == 202) {
                    return;
                }
            } finally {
                connection.disconnect();
            }
        }
        {
            logger.debug("------------------------------ peeking container...");
            final HttpURLConnection connection
                    = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setRequestProperty("Accept", "*/*");
            connection.setDoOutput(false);

            try {
                connection.connect();
                final int statusCode = statusCode(connection, 204);
            } finally {
                connection.disconnect();
            }
        }
        {
            logger.debug("----------------------------- deleting container...");
            final HttpURLConnection connection
                    = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            try {
                connection.connect();
                final int statusCode = statusCode(connection, 204);
            } finally {
                connection.disconnect();
            }
        }
    }

    // ----------------------------------------------- /account/container/object
    @Test(invocationCount = INVOCATION_COUNT)
    public void verifyObject() throws IOException {
        logger.debug("---------------------------------------- verifyObject()");
        final String container = getClass().getName() + "_" + UUID.randomUUID();
        logger.debug("container: {}", container);
        final String object = getClass().getName() + "_" + UUID.randomUUID();
        logger.debug("object: {}", object);
        final URL containerUrl = new URL(storageUrl + "/" + container);
        final URL objectUrl = new URL(storageUrl + "/" + container + "/" + object);
        {
            logger.debug("----------------------------- updating container...");
            final HttpURLConnection connection
                    = (HttpURLConnection) containerUrl.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            try {
                connection.connect();
                final int statusCode = statusCode(connection, 201, 202);
            } finally {
                connection.disconnect();
            }
        }
        final byte[] requestBody
                = new byte[ThreadLocalRandom.current().nextInt(1024)];
        ThreadLocalRandom.current().nextBytes(requestBody);
        {
            logger.debug("-------------------------------- updating object...");
            final HttpURLConnection connection
                    = (HttpURLConnection) objectUrl.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setChunkedStreamingMode(128);
            try {
                connection.connect();
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBody);
                    output.flush();
                }
                final int statusCode = statusCode(connection, 201, 202);
            } finally {
                connection.disconnect();
            }
        }
        {
            logger.debug("--------------------------------- reading object...");
            final HttpURLConnection connection
                    = (HttpURLConnection) objectUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            try {
                connection.connect();
                final int statusCode = statusCode(connection, 200);
                final byte[] responseBody = new byte[requestBody.length];
                try (DataInputStream stream
                        = new DataInputStream(connection.getInputStream())) {
                    stream.readFully(responseBody);
                }
                assertEquals(responseBody, requestBody);
            } finally {
                connection.disconnect();
            }
        }
        {
            logger.debug("-------------------------------- deleting object...");
            final HttpURLConnection connection
                    = (HttpURLConnection) objectUrl.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            try {
                connection.connect();
                final int statusCode = statusCode(connection, 204);
            } finally {
                connection.disconnect();
            }
        }
        {
            logger.debug("----------------------------- deleting container...");
            final HttpURLConnection connection
                    = (HttpURLConnection) containerUrl.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            try {
                connection.connect();
                final int statusCode = statusCode(connection, 204);
            } finally {
                connection.disconnect();
            }
        }
    }

    private transient String authToken;

    private transient String storageUrl;

    private transient String resellerUrl;
}
