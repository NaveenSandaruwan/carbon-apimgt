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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.governance.api.ValidationEngine;
import org.wso2.carbon.apimgt.governance.api.error.APIMGovExceptionCodes;
import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;
import org.wso2.carbon.apimgt.governance.api.model.Rule;
import org.wso2.carbon.apimgt.governance.api.model.RuleSeverity;
import org.wso2.carbon.apimgt.governance.api.model.RuleViolation;
import org.wso2.carbon.apimgt.governance.api.model.Ruleset;
import org.wso2.carbon.apimgt.governance.api.model.RulesetContent;
import org.wso2.carbon.apimgt.governance.impl.util.APIMGovernanceUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validation engine skeleton for EXTERNAL governance rulesets.
 * This currently supports ruleset persistence only. Runtime validation will be added later.
 */
public class ExternalGovernanceValidationEngine implements ValidationEngine {

    private static final Log log = LogFactory.getLog(ExternalGovernanceValidationEngine.class);

    @Override
    public void validateRulesetContent(Ruleset ruleset) throws APIMGovernanceException {
        RulesetContent content = ruleset.getRulesetContent();
        if (content == null || content.getContent() == null) {
            throw new APIMGovernanceException(APIMGovExceptionCodes.INVALID_RULESET_CONTENT,
                    ruleset.getName());
        }

        Map<String, Object> contentMap = APIMGovernanceUtil.getMapFromYAMLStringContent(
                new String(content.getContent(), StandardCharsets.UTF_8));
        Object rules = getRules(contentMap);
        if (!(rules instanceof Map) || ((Map<?, ?>) rules).isEmpty()) {
            throw new APIMGovernanceException(APIMGovExceptionCodes.INVALID_RULESET_CONTENT,
                    ruleset.getName());
        }
    }

    @Override
    public List<Rule> extractRulesFromRuleset(Ruleset ruleset) throws APIMGovernanceException {
        Map<String, Object> contentMap = APIMGovernanceUtil.getMapFromYAMLStringContent(
                new String(ruleset.getRulesetContent().getContent(), StandardCharsets.UTF_8));
        Object rulesObject = getRules(contentMap);
        List<Rule> rulesList = new ArrayList<>();

        if (!(rulesObject instanceof Map)) {
            return rulesList;
        }

        Map<String, Object> rules = (Map<String, Object>) rulesObject;
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        for (Map.Entry<String, Object> entry : rules.entrySet()) {
            Rule rule = new Rule();
            rule.setId(APIMGovernanceUtil.generateUUID());
            rule.setName(entry.getKey());

            if (entry.getValue() instanceof Map) {
                Map<String, Object> ruleDetails = (Map<String, Object>) entry.getValue();
                rule.setDescription(ruleDetails.get("description") != null
                        ? String.valueOf(ruleDetails.get("description")) : "");
                rule.setSeverity(resolveSeverity(ruleDetails.get("severity")));
                try {
                    rule.setContent(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ruleDetails));
                } catch (JsonProcessingException e) {
                    throw new APIMGovernanceException(
                            APIMGovExceptionCodes.ERROR_WHILE_EXTRACTING_RULE_CONTENT, e);
                }
            } else {
                rule.setDescription("");
                rule.setSeverity(RuleSeverity.ERROR);
                rule.setContent(String.valueOf(entry.getValue()));
            }
            rulesList.add(rule);
        }

        if (log.isDebugEnabled()) {
            log.debug("Extracted " + rulesList.size() + " rule(s) from EXTERNAL ruleset: " + ruleset.getName());
        }
        return rulesList;
    }

    @Override
    public List<RuleViolation> validate(String target, Ruleset ruleset) throws APIMGovernanceException {
        throw new APIMGovernanceException("External governance runtime validation is not implemented yet.");
    }

    private Object getRules(Map<String, Object> contentMap) {
        Object rules = contentMap.get("rules");
        if (!(rules instanceof Map) && contentMap.get("rulesetContent") instanceof Map) {
            rules = ((Map<String, Object>) contentMap.get("rulesetContent")).get("rules");
        }
        return rules;
    }

    private RuleSeverity resolveSeverity(Object severityValue) {
        if (severityValue == null) {
            return RuleSeverity.ERROR;
        }
        RuleSeverity severity = RuleSeverity.fromString(String.valueOf(severityValue));
        return severity != null ? severity : RuleSeverity.ERROR;
    }
}
