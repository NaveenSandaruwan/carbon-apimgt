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

package org.wso2.carbon.apimgt.governance.external.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.apimgt.governance.api.model.RuleCategory;
import org.wso2.carbon.apimgt.governance.external.ExternalGovernanceValidationEngine;
import org.wso2.carbon.apimgt.governance.impl.validator.ValidationEngineFactory;

/**
 * OSGi component for the External governance engine.
 */
@Component(
        name = "org.wso2.carbon.apimgt.governance.external",
        immediate = true
)
public class ExternalGovernanceComponent {

    private static final Log log = LogFactory.getLog(ExternalGovernanceComponent.class);
    private static final String LOG_PREFIX = "###===### [External Governance] ";

    @Activate
    protected void activate(ComponentContext componentContext) {
        ValidationEngineFactory.registerValidationEngine(RuleCategory.EXTERNAL,
                new ExternalGovernanceValidationEngine());
        log.info(LOG_PREFIX + "Registered ExternalGovernanceValidationEngine for EXTERNAL rule category.");
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        ValidationEngineFactory.unregisterValidationEngine(RuleCategory.EXTERNAL);
        log.info(LOG_PREFIX + "Unregistered ExternalGovernanceValidationEngine for EXTERNAL rule category.");
    }
}
