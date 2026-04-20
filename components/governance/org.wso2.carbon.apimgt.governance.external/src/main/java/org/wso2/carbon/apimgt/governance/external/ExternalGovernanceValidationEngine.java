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

package org.wso2.carbon.apimgt.governance.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.governance.api.ValidationEngine;
import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;
import org.wso2.carbon.apimgt.governance.api.model.APIMGovernanceOptions;
import org.wso2.carbon.apimgt.governance.api.model.Rule;
import org.wso2.carbon.apimgt.governance.api.model.RuleSeverity;
import org.wso2.carbon.apimgt.governance.api.model.RuleViolation;
import org.wso2.carbon.apimgt.governance.api.model.Ruleset;
import org.wso2.carbon.apimgt.governance.api.model.RulesetContent;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * External governance engine that delegates validation to a remote HTTP service.
 */
public class ExternalGovernanceValidationEngine implements ValidationEngine {

    private static final Log log = LogFactory.getLog(ExternalGovernanceValidationEngine.class);
    private static final String LOG_PREFIX = "###===### [External Governance] ";
    private static final String DEFAULT_TARGET_PATH = "$";
    private static final String DEFAULT_HTTP_METHOD = "POST";
    private static final String DEFAULT_RESULT_PATH = "$.flagged";
    private static final Pattern FULL_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([^}]+?)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Configuration JSON_PATH_CONFIGURATION = Configuration.defaultConfiguration();
    private static final Configuration JSON_PATH_PATH_CONFIGURATION = Configuration.builder()
            .options(Option.AS_PATH_LIST)
            .build();
    private static final List<String> SUPPORTED_METHODS = Arrays.asList("POST");

    @Override
    public void validateRulesetContent(Ruleset ruleset) throws APIMGovernanceException {
        ExternalRulesetDefinition rulesetDefinition = parseRulesetDefinition(ruleset);
        for (ExternalRuleDefinition ruleDefinition : rulesetDefinition.getRules()) {
            validateRuleDefinition(ruleDefinition);
        }
        log.debug(LOG_PREFIX + "Validated EXTERNAL ruleset content for ruleset " + ruleset.getName()
                + " with " + rulesetDefinition.getRules().size() + " rules.");
    }

    @Override
    public List<Rule> extractRulesFromRuleset(Ruleset ruleset) throws APIMGovernanceException {
        ExternalRulesetDefinition rulesetDefinition = parseRulesetDefinition(ruleset);
        List<Rule> rules = new ArrayList<>();
        for (ExternalRuleDefinition ruleDefinition : rulesetDefinition.getRules()) {
            Rule rule = new Rule();
            rule.setId(UUID.randomUUID().toString());
            rule.setName(ruleDefinition.getName());
            rule.setDescription(truncate(ruleDefinition.getDescription(), 1024));
            rule.setSeverity(ruleDefinition.getSeverity());
            try {
                rule.setContent(YAML_MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(ruleDefinition.getRawRule()));
            } catch (JsonProcessingException e) {
                throw new APIMGovernanceException("Failed to serialize EXTERNAL rule content for rule "
                        + ruleDefinition.getName(), e);
            }
            rules.add(rule);
        }
        log.debug(LOG_PREFIX + "Extracted " + rules.size() + " EXTERNAL rules from ruleset "
                + ruleset.getName());
        return rules;
    }

    @Override
    public List<RuleViolation> validate(String target, Ruleset ruleset) throws APIMGovernanceException {
        return validate(target, ruleset, null);
    }

    @Override
    public List<RuleViolation> validate(String target, Ruleset ruleset, APIMGovernanceOptions governanceOptions)
            throws APIMGovernanceException {

        ExternalRulesetDefinition rulesetDefinition = parseRulesetDefinition(ruleset);
        Object documentObject = parsePayload(target, "target document");
        String documentJson = toJson(documentObject, "target document");
        List<RuleViolation> violations = new ArrayList<>();

        log.debug(LOG_PREFIX + "Starting validation for EXTERNAL ruleset " + ruleset.getName()
                + " against target content.");

        for (ExternalRuleDefinition ruleDefinition : rulesetDefinition.getRules()) {
            List<SelectedTarget> selectedTargets = selectTargets(documentJson, ruleDefinition);
            log.debug(LOG_PREFIX + "Rule " + ruleDefinition.getName() + " resolved "
                    + selectedTargets.size() + " targets using path " + ruleDefinition.getTargetPath());

            for (SelectedTarget selectedTarget : selectedTargets) {
                List<ValidationItem> validationItems;
                try {
                    validationItems = extractValidationItems(selectedTarget, ruleDefinition.getContentPath());
                    log.debug(LOG_PREFIX + "Rule " + ruleDefinition.getName() + " expanded target path "
                            + selectedTarget.getPath() + " into " + validationItems.size()
                            + " validation items.");
                } catch (Exception e) {
                    String errorMessage = "External validation failed for rule '" + ruleDefinition.getName()
                            + "' at path '" + selectedTarget.getPath() + "': " + e.getMessage();
                    log.warn(LOG_PREFIX + errorMessage);
                    if (log.isDebugEnabled()) {
                        log.debug(LOG_PREFIX + "Stack trace for EXTERNAL validation failure.", e);
                    }
                    violations.add(createViolation(ruleset, ruleDefinition, selectedTarget.getPath(),
                            errorMessage, RuleSeverity.WARN));
                    continue;
                }

                for (ValidationItem validationItem : validationItems) {
                    try {
                        ValidationContext requestContext = new ValidationContext(ruleset, ruleDefinition,
                                documentObject, selectedTarget, validationItem, null);

                        Object requestPayload = buildRequestPayload(requestContext);
                        String requestPayloadJson = requestPayload == null ? null
                                : toJson(requestPayload, "external request payload");

                        log.debug(LOG_PREFIX + "Invoking external service for rule "
                                + ruleDefinition.getName() + " at artifact path "
                                + validationItem.getPath() + " using method "
                                + ruleDefinition.getMethod() + " and URL "
                                + ruleDefinition.getServiceUrl());

                        ExternalServiceResponse serviceResponse =
                                invokeExternalService(ruleDefinition, requestPayloadJson);
                        Object responsePayload =
                                parsePayload(serviceResponse.getBody(), "external service response");
                        ValidationContext responseContext = requestContext.withResponsePayload(responsePayload);

                        boolean violated = isViolation(ruleDefinition, responsePayload);
                        log.debug(LOG_PREFIX + "Rule " + ruleDefinition.getName() + " at path "
                                + validationItem.getPath() + " returned violation=" + violated);

                        if (violated) {
                            String violationMessage = buildViolationMessage(responseContext);
                            violations.add(createViolation(ruleset, ruleDefinition, validationItem.getPath(),
                                    violationMessage, ruleDefinition.getSeverity()));
                        }
                    } catch (Exception e) {
                        String errorMessage = "External validation failed for rule '"
                                + ruleDefinition.getName() + "' at path '" + validationItem.getPath()
                                + "': " + e.getMessage();
                        log.warn(LOG_PREFIX + errorMessage);
                        if (log.isDebugEnabled()) {
                            log.debug(LOG_PREFIX + "Stack trace for EXTERNAL validation failure.", e);
                        }
                        violations.add(createViolation(ruleset, ruleDefinition, validationItem.getPath(),
                                errorMessage, RuleSeverity.WARN));
                    }
                }
            }
        }

        return violations;
    }

    private ExternalRulesetDefinition parseRulesetDefinition(Ruleset ruleset) throws APIMGovernanceException {
        RulesetContent rulesetContent = ruleset.getRulesetContent();
        if (rulesetContent == null || rulesetContent.getContent() == null) {
            throw new APIMGovernanceException("EXTERNAL ruleset content cannot be empty for ruleset "
                    + ruleset.getName());
        }

        String content = new String(rulesetContent.getContent(), StandardCharsets.UTF_8);
        Map<String, Object> rulesetMap = readYamlAsMap(content, "ruleset " + ruleset.getName());
        Map<String, Object> rules = asMap(rulesetMap.get("rules"), "rules");
        if (rules.isEmpty()) {
            throw new APIMGovernanceException("EXTERNAL ruleset " + ruleset.getName()
                    + " must define at least one rule under 'rules'.");
        }

        List<ExternalRuleDefinition> ruleDefinitions = new ArrayList<>();
        for (Map.Entry<String, Object> entry : rules.entrySet()) {
            String ruleName = entry.getKey();
            Map<String, Object> ruleMap = asMap(entry.getValue(), "rule " + ruleName);
            ruleDefinitions.add(buildRuleDefinition(ruleName, ruleMap));
        }

        return new ExternalRulesetDefinition(ruleDefinitions);
    }

    private ExternalRuleDefinition buildRuleDefinition(String ruleName, Map<String, Object> ruleMap)
            throws APIMGovernanceException {

        Map<String, Object> payload = asMap(ruleMap.get("payload"), "payload");
        Map<String, Object> response = asMap(ruleMap.get("response"), "response");
        Map<String, Object> then = asMap(ruleMap.get("then"), "then");
        Map<String, Object> functionOptions = asMap(then.get("functionOptions"), "then.functionOptions");

        String function = asString(then.get("function"));
        if (function != null && !function.isEmpty() && !"external".equalsIgnoreCase(function)) {
            throw new APIMGovernanceException("Unsupported function '" + function + "' found in EXTERNAL rule "
                    + ruleName + ". Only 'external' is supported.");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        mergeDirectHeaders(headers, asMap(ruleMap.get("headers"), "headers"));
        mergeNestedHeaders(headers, payload);
        mergeNestedHeaders(headers, functionOptions);

        String contentPath = firstNonBlank(asString(payload.get("contentPath")),
                normalizeRelativePath(asString(payload.get("field"))),
                normalizeRelativePath(asString(ruleMap.get("field"))),
                normalizeRelativePath(asString(functionOptions.get("field"))));

        String resultPath = firstNonBlank(asString(response.get("resultPath")),
                normalizeResponsePath(asString(response.get("responseField"))),
                normalizeResponsePath(asString(ruleMap.get("responseField"))),
                normalizeResponsePath(asString(functionOptions.get("responseField"))),
                DEFAULT_RESULT_PATH);

        Object expectedValue = response.containsKey("expectedValue") ? response.get("expectedValue")
                : functionOptions.containsKey("expectedValue") ? functionOptions.get("expectedValue")
                : Boolean.TRUE;

        Object payloadTemplate = payload.get("template") != null ? payload.get("template")
                : ruleMap.get("payloadTemplate") != null ? ruleMap.get("payloadTemplate")
                : functionOptions.get("payloadTemplate");

        String serviceUrl = firstNonBlank(asString(ruleMap.get("serviceUrl")),
                asString(ruleMap.get("serviceURL")), asString(then.get("serviceUrl")),
                asString(then.get("serviceURL")), asString(functionOptions.get("serviceUrl")),
                asString(functionOptions.get("serviceURL")));

        String targetPath = firstNonBlank(asString(ruleMap.get("targetPath")),
                asString(ruleMap.get("targetpath")), asString(then.get("targetPath")),
                asString(then.get("targetpath")), asString(functionOptions.get("targetPath")),
                asString(functionOptions.get("targetpath")), DEFAULT_TARGET_PATH);

        String messagePath = firstNonBlank(asString(response.get("messagePath")),
                asString(ruleMap.get("messagePath")), asString(functionOptions.get("messagePath")));

        return new ExternalRuleDefinition(
                ruleName,
                truncate(asString(ruleMap.get("description")), 1024),
                truncate(asString(ruleMap.get("message")), 1024),
                parseSeverity(asString(ruleMap.get("severity"))),
                serviceUrl,
                targetPath,
                normalizeMethod(firstNonBlank(asString(payload.get("method")),
                        asString(functionOptions.get("method")), asString(ruleMap.get("method")),
                        DEFAULT_HTTP_METHOD)),
                contentPath,
                headers,
                payloadTemplate,
                resultPath,
                expectedValue,
                messagePath,
                ruleMap);
    }

    private void validateRuleDefinition(ExternalRuleDefinition ruleDefinition) throws APIMGovernanceException {
        if (ruleDefinition.getName() == null || ruleDefinition.getName().isEmpty()) {
            throw new APIMGovernanceException("EXTERNAL rules must have a name.");
        }
        if (ruleDefinition.getName().length() > 256) {
            throw new APIMGovernanceException("EXTERNAL rule name '" + ruleDefinition.getName()
                    + "' exceeds the maximum allowed length of 256 characters.");
        }
        if (ruleDefinition.getServiceUrl() == null || ruleDefinition.getServiceUrl().isEmpty()) {
            throw new APIMGovernanceException("EXTERNAL rule '" + ruleDefinition.getName()
                    + "' must define 'serviceUrl'.");
        }
        if (!SUPPORTED_METHODS.contains(ruleDefinition.getMethod())) {
            throw new APIMGovernanceException("Unsupported HTTP method '" + ruleDefinition.getMethod()
                    + "' configured for EXTERNAL rule '" + ruleDefinition.getName()
                    + "'. Only POST is supported.");
        }
        try {
            new URL(ruleDefinition.getServiceUrl());
        } catch (MalformedURLException e) {
            throw new APIMGovernanceException("Invalid serviceUrl '" + ruleDefinition.getServiceUrl()
                    + "' configured for EXTERNAL rule '" + ruleDefinition.getName() + "'.", e);
        }
        if (ruleDefinition.getSeverity() == null) {
            throw new APIMGovernanceException("Invalid severity configured for EXTERNAL rule '"
                    + ruleDefinition.getName() + "'.");
        }
        if (ruleDefinition.getTargetPath() == null || ruleDefinition.getTargetPath().isEmpty()) {
            throw new APIMGovernanceException("EXTERNAL rule '" + ruleDefinition.getName()
                    + "' must define a valid targetPath.");
        }
        if (ruleDefinition.getResultPath() == null || ruleDefinition.getResultPath().isEmpty()) {
            throw new APIMGovernanceException("EXTERNAL rule '" + ruleDefinition.getName()
                    + "' must define a response result path.");
        }
        log.debug(LOG_PREFIX + "Validated EXTERNAL rule '" + ruleDefinition.getName()
                + "' with serviceUrl=" + ruleDefinition.getServiceUrl()
                + ", targetPath=" + ruleDefinition.getTargetPath()
                + ", resultPath=" + ruleDefinition.getResultPath());
    }

    private List<SelectedTarget> selectTargets(String documentJson, ExternalRuleDefinition ruleDefinition)
            throws APIMGovernanceException {

        try {
            Object value = JsonPath.using(JSON_PATH_CONFIGURATION).parse(documentJson)
                    .read(ruleDefinition.getTargetPath());
            List<Object> values = normalizeToList(value);
            List<String> paths = JsonPath.using(JSON_PATH_PATH_CONFIGURATION).parse(documentJson)
                    .read(ruleDefinition.getTargetPath());

            if (values.isEmpty()) {
                return Collections.emptyList();
            }

            if (paths.isEmpty()) {
                paths = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) {
                    paths.add(ruleDefinition.getTargetPath());
                }
            }

            List<SelectedTarget> selectedTargets = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                String path = i < paths.size() ? paths.get(i) : ruleDefinition.getTargetPath();
                selectedTargets.add(new SelectedTarget(path, values.get(i)));
            }
            return selectedTargets;
        } catch (PathNotFoundException e) {
            log.debug(LOG_PREFIX + "No targets matched path " + ruleDefinition.getTargetPath()
                    + " for rule " + ruleDefinition.getName());
            return Collections.emptyList();
        } catch (Exception e) {
            throw new APIMGovernanceException("Failed to resolve targetPath '" + ruleDefinition.getTargetPath()
                    + "' for EXTERNAL rule '" + ruleDefinition.getName() + "'.", e);
        }
    }

    private List<ValidationItem> extractValidationItems(SelectedTarget selectedTarget, String contentPath)
            throws APIMGovernanceException {

        if (contentPath == null || contentPath.isEmpty()) {
            return Collections.singletonList(new ValidationItem(selectedTarget.getPath(), selectedTarget.getValue()));
        }

        try {
            String targetJson = toJson(selectedTarget.getValue(), "selected target");
            Object extractedValue = JsonPath.using(JSON_PATH_CONFIGURATION).parse(targetJson).read(contentPath);
            List<Object> extractedValues = normalizeToList(extractedValue);
            List<String> relativePaths = JsonPath.using(JSON_PATH_PATH_CONFIGURATION).parse(targetJson)
                    .read(contentPath);

            if (extractedValues.isEmpty()) {
                return Collections.singletonList(new ValidationItem(selectedTarget.getPath(), null));
            }

            List<ValidationItem> validationItems = new ArrayList<>();
            for (int i = 0; i < extractedValues.size(); i++) {
                String relativePath = i < relativePaths.size() ? relativePaths.get(i) : contentPath + "[" + i + "]";
                validationItems.add(new ValidationItem(mergePaths(selectedTarget.getPath(), relativePath),
                        extractedValues.get(i)));
            }
            return validationItems;
        } catch (PathNotFoundException e) {
            return Collections.singletonList(new ValidationItem(selectedTarget.getPath(), null));
        } catch (Exception e) {
            throw new APIMGovernanceException("Failed to extract contentPath '" + contentPath
                    + "' from selected target.", e);
        }
    }

    private Object buildRequestPayload(ValidationContext validationContext) {
        if (validationContext.getRuleDefinition().getPayloadTemplate() != null) {
            log.debug(LOG_PREFIX + "Building payload from user-defined template for rule "
                    + validationContext.getRuleDefinition().getName());
            return resolveTemplateValue(validationContext.getRuleDefinition().getPayloadTemplate(),
                    validationContext);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleName", validationContext.getRuleDefinition().getName());
        payload.put("severity", validationContext.getRuleDefinition().getSeverity().name());
        payload.put("rulesetId", validationContext.getRuleset().getId());
        payload.put("artifactType", validationContext.getRuleset().getArtifactType() != null
                ? validationContext.getRuleset().getArtifactType().name() : null);
        payload.put("targetPath", validationContext.getValidationPath());
        payload.put("target", validationContext.getSelectedTarget().getValue());
        payload.put("value", validationContext.getExtractedValue());

        log.debug(LOG_PREFIX + "Built default payload for rule "
                + validationContext.getRuleDefinition().getName());
        return payload;
    }

    private ExternalServiceResponse invokeExternalService(ExternalRuleDefinition ruleDefinition, String payloadJson)
            throws APIMGovernanceException {

        try (CloseableHttpClient httpClient =
                     (CloseableHttpClient) APIUtil.getHttpClient(ruleDefinition.getServiceUrl())) {
            HttpRequestBase request = buildRequest(ruleDefinition, payloadJson);
            for (Map.Entry<String, String> headerEntry : ruleDefinition.getHeaders().entrySet()) {
                request.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
            if (!request.containsHeader("Content-Type")) {
                request.setHeader("Content-Type",
                        ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());
            }
            request.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : 0;
            HttpEntity entity = response.getEntity();
            String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);

            log.debug(LOG_PREFIX + "Received response from external service for rule "
                    + ruleDefinition.getName() + " with status " + statusCode);

            if (statusCode < 200 || statusCode >= 300) {
                throw new APIMGovernanceException("External service returned HTTP " + statusCode
                        + " for rule '" + ruleDefinition.getName() + "'. Response body: " + body);
            }
            return new ExternalServiceResponse(body);
        } catch (APIManagementException e) {
            throw new APIMGovernanceException("Failed to create HTTP client for EXTERNAL rule '"
                    + ruleDefinition.getName() + "'.", e);
        } catch (APIMGovernanceException e) {
            throw e;
        } catch (Exception e) {
            throw new APIMGovernanceException("Failed to invoke external service for EXTERNAL rule '"
                    + ruleDefinition.getName() + "'.", e);
        }
    }

    private HttpRequestBase buildRequest(ExternalRuleDefinition ruleDefinition, String payloadJson)
            throws APIMGovernanceException {

        String serviceUrl = ruleDefinition.getServiceUrl();
        String method = ruleDefinition.getMethod();

        if ("POST".equals(method)) {
            HttpPost request = new HttpPost(serviceUrl);
            if (payloadJson != null) {
                request.setEntity(new StringEntity(payloadJson, StandardCharsets.UTF_8));
            }
            return request;
        }
        throw new APIMGovernanceException("Unsupported HTTP method '" + method + "'. Only POST is supported.");
    }

    private boolean isViolation(ExternalRuleDefinition ruleDefinition, Object responsePayload)
            throws APIMGovernanceException {
        Object resultValue = extractResponseValue(responsePayload, ruleDefinition.getResultPath());
        return matchesExpectedValue(resultValue, ruleDefinition.getExpectedValue());
    }

    private Object extractResponseValue(Object responsePayload, String resultPath) throws APIMGovernanceException {
        if (responsePayload == null) {
            return null;
        }
        if (resultPath == null || resultPath.isEmpty()) {
            return responsePayload;
        }
        if (responsePayload instanceof String) {
            return responsePayload;
        }
        try {
            String responseJson = toJson(responsePayload, "external response");
            return JsonPath.using(JSON_PATH_CONFIGURATION).parse(responseJson).read(resultPath);
        } catch (PathNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new APIMGovernanceException("Failed to extract resultPath '" + resultPath
                    + "' from external service response.", e);
        }
    }

    private boolean matchesExpectedValue(Object actualValue, Object expectedValue) {
        if (actualValue instanceof List) {
            for (Object item : (List<?>) actualValue) {
                if (matchesExpectedValue(item, expectedValue)) {
                    return true;
                }
            }
            return false;
        }
        if (expectedValue == null) {
            expectedValue = Boolean.TRUE;
        }
        if (actualValue == null) {
            return false;
        }
        if (expectedValue instanceof Boolean) {
            return parseBoolean(actualValue) == (Boolean) expectedValue;
        }
        return String.valueOf(expectedValue).equalsIgnoreCase(String.valueOf(actualValue));
    }

    private String buildViolationMessage(ValidationContext validationContext) {
        String detailMessage = resolveViolationDetailMessage(validationContext);
        return truncate(buildGeneralViolationMessage(detailMessage, validationContext), 1024);
    }

    private String resolveViolationDetailMessage(ValidationContext validationContext) {
        String configuredMessage = validationContext.getRuleDefinition().getMessage();
        if (configuredMessage != null && !configuredMessage.isEmpty()) {
            return String.valueOf(resolveTemplateValue(configuredMessage, validationContext));
        }

        String responseMessagePath = validationContext.getRuleDefinition().getMessagePath();
        if (responseMessagePath == null || validationContext.getResponsePayload() == null) {
            return null;
        }
        try {
            Object responseMessage = extractResponseValue(validationContext.getResponsePayload(), responseMessagePath);
            return responseMessage == null ? null : String.valueOf(responseMessage);
        } catch (APIMGovernanceException e) {
            log.debug(LOG_PREFIX + "Failed to resolve response message path '" + responseMessagePath
                    + "' for rule " + validationContext.getRuleDefinition().getName());
            return null;
        }
    }

    private String buildGeneralViolationMessage(String detailMessage, ValidationContext validationContext) {
        String contentSnippet = buildContentSnippet(validationContext.getExtractedValue());
        StringBuilder messageBuilder = new StringBuilder();

        if (contentSnippet != null) {
            messageBuilder.append("Content '").append(contentSnippet).append("' violated rule '")
                    .append(validationContext.getRuleDefinition().getName()).append("'.");
        } else {
            messageBuilder.append("Selected content violated rule '")
                    .append(validationContext.getRuleDefinition().getName()).append("'.");
        }

        if (detailMessage != null && !detailMessage.trim().isEmpty()) {
            String normalizedDetailMessage = detailMessage.trim();
            messageBuilder.append(" ").append(normalizedDetailMessage);
            if (!normalizedDetailMessage.endsWith(".") && !normalizedDetailMessage.endsWith("!")
                    && !normalizedDetailMessage.endsWith("?")) {
                messageBuilder.append(".");
            }
        }
        return messageBuilder.toString();
    }

    private String buildContentSnippet(Object value) {
        if (value == null) {
            return null;
        }

        String contentSnippet;
        if (value instanceof Map || value instanceof List) {
            try {
                contentSnippet = toJson(value, "violation content");
            } catch (APIMGovernanceException e) {
                contentSnippet = String.valueOf(value);
            }
        } else {
            contentSnippet = String.valueOf(value);
        }
        contentSnippet = contentSnippet.trim().replaceAll("\\s+", " ");
        if (contentSnippet.isEmpty()) {
            return null;
        }
        return truncate(contentSnippet, 160);
    }

    private String mergePaths(String basePath, String relativePath) {
        if (relativePath == null || relativePath.isEmpty() || "$".equals(relativePath)) {
            return basePath;
        }
        if (basePath == null || basePath.isEmpty()) {
            return relativePath;
        }
        if (relativePath.startsWith("$")) {
            return basePath + relativePath.substring(1);
        }
        return basePath + relativePath;
    }

    private RuleViolation createViolation(Ruleset ruleset, ExternalRuleDefinition ruleDefinition, String violatedPath,
                                          String message, RuleSeverity severity) {
        RuleViolation violation = new RuleViolation();
        violation.setRulesetId(ruleset.getId());
        violation.setRuleName(ruleDefinition.getName());
        violation.setViolatedPath(truncate(violatedPath, 1024));
        violation.setSeverity(severity);
        violation.setRuleMessage(truncate(message, 1024));
        return violation;
    }

    private Object resolveTemplateValue(Object template, ValidationContext validationContext) {
        if (template instanceof Map) {
            Map<String, Object> resolvedMap = new LinkedHashMap<>();
            Map<?, ?> templateMap = (Map<?, ?>) template;
            for (Map.Entry<?, ?> entry : templateMap.entrySet()) {
                resolvedMap.put(String.valueOf(entry.getKey()),
                        resolveTemplateValue(entry.getValue(), validationContext));
            }
            return resolvedMap;
        }
        if (template instanceof List) {
            List<Object> resolvedList = new ArrayList<>();
            for (Object item : (List<?>) template) {
                resolvedList.add(resolveTemplateValue(item, validationContext));
            }
            return resolvedList;
        }
        if (!(template instanceof String)) {
            return template;
        }

        String templateString = (String) template;
        Matcher fullMatcher = FULL_PLACEHOLDER_PATTERN.matcher(templateString);
        if (fullMatcher.matches()) {
            Object resolved = resolveExpression(fullMatcher.group(1), validationContext);
            return resolved == null ? "" : resolved;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateString);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object resolved = resolveExpression(matcher.group(1), validationContext);
            matcher.appendReplacement(buffer,
                    Matcher.quoteReplacement(resolved == null ? "" : String.valueOf(resolved)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private Object resolveExpression(String expression, ValidationContext validationContext) {
        String trimmedExpression = expression == null ? "" : expression.trim();
        if (trimmedExpression.isEmpty()) {
            return "";
        }

        if ("targetPath".equals(trimmedExpression)) {
            return validationContext.getValidationPath();
        }
        if ("value".equals(trimmedExpression)) {
            return validationContext.getExtractedValue();
        }

        Map<String, Object> roots = new LinkedHashMap<>();
        roots.put("rule", validationContext.getRuleMap());
        roots.put("ruleset", validationContext.getRulesetMap());
        roots.put("target", validationContext.getSelectedTarget().getValue());
        roots.put("document", validationContext.getDocument());
        roots.put("response", validationContext.getResponsePayload());
        roots.put("value", validationContext.getExtractedValue());
        roots.put("targetPath", validationContext.getValidationPath());

        if (!trimmedExpression.contains(".")) {
            Object targetValue = readProperty(validationContext.getSelectedTarget().getValue(), trimmedExpression);
            if (targetValue != null) {
                return targetValue;
            }
            if (roots.containsKey(trimmedExpression)) {
                return roots.get(trimmedExpression);
            }
        }

        String[] parts = trimmedExpression.split("\\.");
        Object current = roots.get(parts[0]);
        if (current == null) {
            current = readProperty(validationContext.getSelectedTarget().getValue(), trimmedExpression);
            if (current != null) {
                return current;
            }
            return null;
        }

        for (int i = 1; i < parts.length; i++) {
            current = readProperty(current, parts[i]);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Object readProperty(Object current, String propertyPath) {
        if (current == null || propertyPath == null || propertyPath.isEmpty()) {
            return null;
        }
        if (propertyPath.contains(".")) {
            Object nested = current;
            for (String segment : propertyPath.split("\\.")) {
                nested = readProperty(nested, segment);
                if (nested == null) {
                    return null;
                }
            }
            return nested;
        }
        if (current instanceof Map) {
            return ((Map<?, ?>) current).get(propertyPath);
        }
        if (current instanceof List) {
            try {
                int index = Integer.parseInt(propertyPath);
                List<?> list = (List<?>) current;
                return index >= 0 && index < list.size() ? list.get(index) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private RuleSeverity parseSeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return RuleSeverity.WARN;
        }

        String normalizedSeverity = severity.trim().toUpperCase(Locale.ENGLISH);
        if ("WARNING".equals(normalizedSeverity)) {
            return RuleSeverity.WARN;
        }
        return RuleSeverity.fromString(normalizedSeverity);
    }

    private Map<String, Object> readYamlAsMap(String content, String label) throws APIMGovernanceException {
        try {
            Map<String, Object> map = YAML_MAPPER.readValue(content, Map.class);
            return map == null ? new LinkedHashMap<String, Object>() : map;
        } catch (Exception e) {
            throw new APIMGovernanceException("Failed to parse " + label + " as YAML/JSON content.", e);
        }
    }

    private Map<String, Object> asMap(Object value, String label) throws APIMGovernanceException {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (!(value instanceof Map)) {
            throw new APIMGovernanceException("Expected '" + label + "' to be a map.");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            map.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return map;
    }

    private void mergeDirectHeaders(Map<String, String> headers, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            headers.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private void mergeNestedHeaders(Map<String, String> headers, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        Object headerValue = source.get("headers");
        if (!(headerValue instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) headerValue).entrySet()) {
            headers.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
    }

    private Object parsePayload(String payload, String label) throws APIMGovernanceException {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            return YAML_MAPPER.readValue(payload, Object.class);
        } catch (Exception e) {
            throw new APIMGovernanceException("Failed to parse " + label + ".", e);
        }
    }

    private String toJson(Object value, String label) throws APIMGovernanceException {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new APIMGovernanceException("Failed to serialize " + label + " to JSON.", e);
        }
    }

    private List<Object> normalizeToList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            return new ArrayList<>((List<?>) value);
        }
        List<Object> values = new ArrayList<>();
        values.add(value);
        return values;
    }

    private String normalizeMethod(String method) {
        return method == null ? DEFAULT_HTTP_METHOD : method.trim().toUpperCase(Locale.ENGLISH);
    }

    private String normalizeRelativePath(String fieldPath) {
        if (fieldPath == null || fieldPath.trim().isEmpty()) {
            return null;
        }
        String trimmed = fieldPath.trim();
        if (trimmed.startsWith("$")) {
            return trimmed;
        }
        return "$." + trimmed;
    }

    private String normalizeResponsePath(String fieldPath) {
        if (fieldPath == null || fieldPath.trim().isEmpty()) {
            return null;
        }
        String trimmed = fieldPath.trim();
        if (trimmed.startsWith("$")) {
            return trimmed;
        }
        return "$." + trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static final class ExternalRulesetDefinition {
        private final List<ExternalRuleDefinition> rules;

        private ExternalRulesetDefinition(List<ExternalRuleDefinition> rules) {
            this.rules = rules;
        }

        private List<ExternalRuleDefinition> getRules() {
            return rules;
        }
    }

    private static final class ExternalRuleDefinition {
        private final String name;
        private final String description;
        private final String message;
        private final RuleSeverity severity;
        private final String serviceUrl;
        private final String targetPath;
        private final String method;
        private final String contentPath;
        private final Map<String, String> headers;
        private final Object payloadTemplate;
        private final String resultPath;
        private final Object expectedValue;
        private final String messagePath;
        private final Map<String, Object> rawRule;

        private ExternalRuleDefinition(String name, String description, String message, RuleSeverity severity,
                                       String serviceUrl, String targetPath, String method, String contentPath,
                                       Map<String, String> headers, Object payloadTemplate,
                                       String resultPath, Object expectedValue, String messagePath,
                                       Map<String, Object> rawRule) {
            this.name = name;
            this.description = description;
            this.message = message;
            this.severity = severity;
            this.serviceUrl = serviceUrl;
            this.targetPath = targetPath;
            this.method = method;
            this.contentPath = contentPath;
            this.headers = headers;
            this.payloadTemplate = payloadTemplate;
            this.resultPath = resultPath;
            this.expectedValue = expectedValue;
            this.messagePath = messagePath;
            this.rawRule = rawRule;
        }

        private String getName() {
            return name;
        }

        private String getDescription() {
            return description;
        }

        private String getMessage() {
            return message;
        }

        private RuleSeverity getSeverity() {
            return severity;
        }

        private String getServiceUrl() {
            return serviceUrl;
        }

        private String getTargetPath() {
            return targetPath;
        }

        private String getMethod() {
            return method;
        }

        private String getContentPath() {
            return contentPath;
        }

        private Map<String, String> getHeaders() {
            return headers;
        }

        private Object getPayloadTemplate() {
            return payloadTemplate;
        }

        private String getResultPath() {
            return resultPath;
        }

        private Object getExpectedValue() {
            return expectedValue;
        }

        private String getMessagePath() {
            return messagePath;
        }

        private Map<String, Object> getRawRule() {
            return rawRule;
        }
    }

    private static final class SelectedTarget {
        private final String path;
        private final Object value;

        private SelectedTarget(String path, Object value) {
            this.path = path;
            this.value = value;
        }

        private String getPath() {
            return path;
        }

        private Object getValue() {
            return value;
        }
    }

    private static final class ValidationItem {
        private final String path;
        private final Object value;

        private ValidationItem(String path, Object value) {
            this.path = path;
            this.value = value;
        }

        private String getPath() {
            return path;
        }

        private Object getValue() {
            return value;
        }
    }

    private static final class ExternalServiceResponse {
        private final String body;

        private ExternalServiceResponse(String body) {
            this.body = body;
        }

        private String getBody() {
            return body;
        }
    }

    private static final class ValidationContext {
        private final Ruleset ruleset;
        private final ExternalRuleDefinition ruleDefinition;
        private final Object document;
        private final SelectedTarget selectedTarget;
        private final ValidationItem validationItem;
        private final Object responsePayload;

        private ValidationContext(Ruleset ruleset, ExternalRuleDefinition ruleDefinition, Object document,
                                  SelectedTarget selectedTarget, ValidationItem validationItem,
                                  Object responsePayload) {
            this.ruleset = ruleset;
            this.ruleDefinition = ruleDefinition;
            this.document = document;
            this.selectedTarget = selectedTarget;
            this.validationItem = validationItem;
            this.responsePayload = responsePayload;
        }

        private ValidationContext withResponsePayload(Object responsePayload) {
            return new ValidationContext(ruleset, ruleDefinition, document, selectedTarget, validationItem,
                    responsePayload);
        }

        private Ruleset getRuleset() {
            return ruleset;
        }

        private ExternalRuleDefinition getRuleDefinition() {
            return ruleDefinition;
        }

        private Object getDocument() {
            return document;
        }

        private SelectedTarget getSelectedTarget() {
            return selectedTarget;
        }

        private Object getExtractedValue() {
            return validationItem != null ? validationItem.getValue() : null;
        }

        private String getValidationPath() {
            return validationItem != null ? validationItem.getPath() : selectedTarget.getPath();
        }

        private Object getResponsePayload() {
            return responsePayload;
        }

        private Map<String, Object> getRuleMap() {
            Map<String, Object> ruleMap = new LinkedHashMap<>();
            ruleMap.put("name", ruleDefinition.getName());
            ruleMap.put("description", ruleDefinition.getDescription());
            ruleMap.put("severity", ruleDefinition.getSeverity() != null
                    ? ruleDefinition.getSeverity().name() : null);
            ruleMap.put("serviceUrl", ruleDefinition.getServiceUrl());
            ruleMap.put("targetPath", ruleDefinition.getTargetPath());
            return ruleMap;
        }

        private Map<String, Object> getRulesetMap() {
            Map<String, Object> rulesetMap = new LinkedHashMap<>();
            rulesetMap.put("id", ruleset.getId());
            rulesetMap.put("name", ruleset.getName());
            rulesetMap.put("artifactType",
                    ruleset.getArtifactType() != null ? ruleset.getArtifactType().name() : null);
            rulesetMap.put("ruleType", ruleset.getRuleType() != null ? ruleset.getRuleType().name() : null);
            rulesetMap.put("ruleCategory",
                    ruleset.getRuleCategory() != null ? ruleset.getRuleCategory().name() : null);
            return rulesetMap;
        }
    }
}
