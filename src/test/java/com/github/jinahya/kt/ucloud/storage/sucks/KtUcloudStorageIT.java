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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
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

    @BeforeMethod
    public void doBeforeMethod() {
        logger.debug("-------------------------------------------------------");
    }

    private int statusCode(final HttpURLConnection connection,
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
    private Object[][] mediaTypes() {
        return new Object[][]{
            {"text/plain"}, {"application/xml"}, {"application/json"}
        };
    }

    private void authenticateUser() throws IOException {
        logger.debug("authenticateUser()");
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

    @Test(dataProvider = "mediaTypes", invocationCount = 128)
    public void readContainers(final String mediaType) throws IOException {
        logger.debug("readContainers({})", mediaType);
        authenticateUser();
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
                    logger.debug("line: {}", l);
                });
            }
        } finally {
            connection.disconnect();
        }
    }

    private transient String authToken;

    private transient String storageUrl;

    private transient String resellerUrl;
}
