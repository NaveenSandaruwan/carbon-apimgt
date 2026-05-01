/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.apimgt.governance.rest.api.util;

/**
 * Utility class for masking sensitive header values.
 * This utility masks header values with asterisks for display purposes,
 * ensuring sensitive information is not exposed in outgoing responses.
 */
public class HeaderValueMaskingUtil {

    private HeaderValueMaskingUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Masks a header value by replacing it with asterisks.
     * The number of asterisks matches the length of the original value.
     *
     * @param value the header value to mask
     * @return masked value consisting of asterisks, or null if input is null
     */
    public static String maskValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return "*".repeat(value.length());
    }

    /**
     * Checks if the input string is null or empty.
     *
     * @param value the value to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
