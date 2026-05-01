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

package org.wso2.carbon.apimgt.governance.impl;

import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;
import org.wso2.carbon.apimgt.governance.api.model.ExternalService;
import org.wso2.carbon.apimgt.governance.api.model.ExternalServiceList;
import org.wso2.carbon.apimgt.governance.impl.dao.ExternalServiceMgtDAO;
import org.wso2.carbon.apimgt.governance.impl.dao.impl.ExternalServiceMgtDAOImpl;

/**
 * Manager class for External Services.
 */
public class ExternalServiceManager {

    private final ExternalServiceMgtDAO externalServiceMgtDAO;

    public ExternalServiceManager() {
        this.externalServiceMgtDAO = new ExternalServiceMgtDAOImpl();
    }

    /**
     * Add an External Service.
     *
     * @param externalService External Service object
     * @return Added ExternalService
     * @throws APIMGovernanceException If an error occurs
     */
    public ExternalService addExternalService(ExternalService externalService) throws APIMGovernanceException {
        return externalServiceMgtDAO.addExternalService(externalService);
    }

    /**
     * Update an External Service.
     *
     * @param externalService External Service object
     * @return Updated ExternalService
     * @throws APIMGovernanceException If an error occurs
     */
    public ExternalService updateExternalService(ExternalService externalService) throws APIMGovernanceException {
        return externalServiceMgtDAO.updateExternalService(externalService);
    }

    /**
     * Delete an External Service by ID.
     *
     * @param id External Service ID
     * @throws APIMGovernanceException If an error occurs
     */
    public void deleteExternalService(String id) throws APIMGovernanceException {
        externalServiceMgtDAO.deleteExternalService(id);
    }

    /**
     * Get an External Service by ID.
     *
     * @param id External Service ID
     * @return ExternalService
     * @throws APIMGovernanceException If an error occurs
     */
    public ExternalService getExternalServiceById(String id) throws APIMGovernanceException {
        return externalServiceMgtDAO.getExternalServiceById(id);
    }

    /**
     * Get all External Services.
     *
     * @return ExternalServiceList
     * @throws APIMGovernanceException If an error occurs
     */
    public ExternalServiceList getExternalServices() throws APIMGovernanceException {
        return externalServiceMgtDAO.getExternalServices();
    }
}
