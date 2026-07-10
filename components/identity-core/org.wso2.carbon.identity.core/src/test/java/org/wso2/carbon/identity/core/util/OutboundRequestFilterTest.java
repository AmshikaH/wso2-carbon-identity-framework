/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.core.util;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link OutboundRequestFilter}. The blocked hosts set is injected directly into the
 * package-private field (it is otherwise loaded once from config at class-init).
 */
public class OutboundRequestFilterTest {

    private static Set<String> hosts(String... values) {

        Set<String> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }

    @BeforeMethod
    @AfterMethod
    public void resetList() {

        OutboundRequestFilter.blockedHosts = Collections.emptySet();
    }

    @Test
    public void testEmptyBlocklistAllowsEverything() {

        OutboundRequestFilter.blockedHosts = Collections.emptySet();
        assertTrue(OutboundRequestFilter.isAllowed("http://127.0.0.1:8899/jwks.json"));
        assertTrue(OutboundRequestFilter.isAllowed("http://169.254.169.254/latest/meta-data/"));
        assertTrue(OutboundRequestFilter.isAllowed("http://localhost/internal"));
        assertTrue(OutboundRequestFilter.isAllowed("not-a-valid-url"));
    }

    @Test
    public void testBlocklistBlocksOnlyListedHosts() {

        OutboundRequestFilter.blockedHosts = hosts("169.254.169.254", "localhost");
        assertFalse(OutboundRequestFilter.isAllowed("http://169.254.169.254/latest/meta-data/"));
        assertFalse(OutboundRequestFilter.isAllowed("http://localhost/internal"));
        assertTrue(OutboundRequestFilter.isAllowed("https://jwks.example.com/keys"));
    }

    @Test
    public void testBlocklistMatchIsCaseInsensitive() {

        OutboundRequestFilter.blockedHosts = hosts("internal.example.com");
        assertFalse(OutboundRequestFilter.isAllowed("https://INTERNAL.EXAMPLE.COM/keys"));
    }

    @Test
    public void testActiveBlocklistBlocksBlankOrMalformedUrl() {

        OutboundRequestFilter.blockedHosts = hosts("169.254.169.254");
        assertFalse(OutboundRequestFilter.isAllowed(null));
        assertFalse(OutboundRequestFilter.isAllowed(""));
        assertFalse(OutboundRequestFilter.isAllowed("not-a-valid-url"));
    }
}
