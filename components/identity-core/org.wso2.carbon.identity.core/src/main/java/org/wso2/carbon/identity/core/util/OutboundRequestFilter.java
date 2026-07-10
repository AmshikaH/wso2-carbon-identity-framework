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

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Server-level outbound (egress) request filter. Blocks a request only if its host is in the
 * {@code <OutboundRequestFilter>} blocked hosts list in {@code identity.xml}. No-op until
 * configured: an empty list permits every URL. Read once and cached (config is static per run).
 */
public final class OutboundRequestFilter {

    private static final Log log = LogFactory.getLog(OutboundRequestFilter.class);

    private static final String CONFIG_OUTBOUND_REQUEST_FILTER = "OutboundRequestFilter";
    private static final String CONFIG_BLOCKED_HOSTS = "BlockedHosts";
    private static final String CONFIG_HOST = "Host";

    static Set<String> blockedHosts = loadBlockedHosts();

    private OutboundRequestFilter() {

    }

    /**
     * Whether an outbound request to {@code url} is permitted: blocked only if its host is in the
     * configured blocked hosts list; an empty list permits every URL (no-op). Case-insensitive.
     * When the list is non-empty (filter active), a URL with no resolvable host is also blocked.
     *
     * @param url The destination URL.
     * @return {@code true} if permitted, {@code false} if blocked.
     */
    public static boolean isAllowed(String url) {

        if (blockedHosts.isEmpty()) {
            return true;
        }

        String host = extractHost(url);
        if (StringUtils.isBlank(host)) {
            if (log.isDebugEnabled()) {
                log.debug("OutboundRequestFilter blocked an outbound request: the destination URL has no "
                        + "resolvable host.");
            }
            return false;
        }
        if (blockedHosts.contains(host.toLowerCase(Locale.ENGLISH))) {
            if (log.isDebugEnabled()) {
                log.debug("OutboundRequestFilter blocked an outbound request to host [" + host
                        + "]: host is in the configured blocked hosts list.");
            }
            return false;
        }
        return true;
    }

    private static Set<String> loadBlockedHosts() {

        Set<String> hosts = new HashSet<>();
        try {
            OMElement filterConfig =
                    IdentityConfigParser.getInstance().getConfigElement(CONFIG_OUTBOUND_REQUEST_FILTER);
            if (filterConfig == null) {
                return Collections.unmodifiableSet(hosts);
            }
            OMElement container = filterConfig.getFirstChildWithName(
                    IdentityConfigParser.getInstance().getQNameWithIdentityNS(CONFIG_BLOCKED_HOSTS));
            if (container == null) {
                return Collections.unmodifiableSet(hosts);
            }
            Iterator<OMElement> hostElements = container.getChildrenWithName(
                    IdentityConfigParser.getInstance().getQNameWithIdentityNS(CONFIG_HOST));
            while (hostElements != null && hostElements.hasNext()) {
                String value = hostElements.next().getText();
                if (StringUtils.isNotBlank(value)) {
                    hosts.add(value.trim().toLowerCase(Locale.ENGLISH));
                }
            }
        } catch (RuntimeException e) {
            log.error("Failed to load OutboundRequestFilter blocked hosts; treating it as empty.", e);
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(hosts);
    }

    private static String extractHost(String url) {

        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
