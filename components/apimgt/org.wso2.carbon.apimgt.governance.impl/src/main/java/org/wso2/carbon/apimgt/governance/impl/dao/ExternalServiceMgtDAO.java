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

package org.wso2.carbon.apimgt.governance.impl.dao;

import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;
import org.wso2.carbon.apimgt.governance.api.model.ExternalService;
import org.wso2.carbon.apimgt.governance.api.model.ExternalServiceList;

/**
 * Interface for External Service Management DAO.
 */
public interface ExternalServiceMgtDAO {

    /**
     * Add an External Service.
     *
     * @param externalService ExternalService object
     * @return ExternalService
     * @throws APIMGovernanceException If an error occurs while adding the external service
     */
    ExternalService addExternalService(ExternalService externalService) throws APIMGovernanceException;

    /**
     * Update an External Service.
     *
     * @param externalService ExternalService object
     * @return ExternalService
     * @throws APIMGovernanceException If an error occurs while updating the external service
     */
    ExternalService updateExternalService(ExternalService externalService) throws APIMGovernanceException;

    /**
     * Delete an External Service by ID.
     *
     * @param id External Service ID
     * @throws APIMGovernanceException If an error occurs while deleting the external service
     */
    void deleteExternalService(String id) throws APIMGovernanceException;

    /**
     * Get an External Service by ID.
     *
     * @param id External Service ID
     * @return ExternalService
     * @throws APIMGovernanceException If an error occurs while retrieving the external service
     */
    ExternalService getExternalServiceById(String id) throws APIMGovernanceException;

    /**
     * Get all External Services.
     *
     * @return ExternalServiceList
     * @throws APIMGovernanceException If an error occurs while retrieving external services
     */
    ExternalServiceList getExternalServices() throws APIMGovernanceException;
}
