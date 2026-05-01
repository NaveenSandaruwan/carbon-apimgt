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

package org.wso2.carbon.apimgt.governance.rest.api.impl;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;
import org.wso2.carbon.apimgt.governance.api.model.ExternalService;
import org.wso2.carbon.apimgt.governance.api.model.ExternalServiceList;
import org.wso2.carbon.apimgt.governance.impl.ExternalServiceManager;
import org.wso2.carbon.apimgt.governance.rest.api.ExternalServicesApiService;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceDTO;
import org.wso2.carbon.apimgt.governance.rest.api.mappings.ExternalServiceMappingUtil;
import org.wso2.carbon.apimgt.governance.rest.api.util.APIMGovernanceAPIUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation of ExternalServicesApiService.
 */
public class ExternalServicesApiServiceImpl implements ExternalServicesApiService {

    private final ExternalServiceManager externalServiceManager;

    public ExternalServicesApiServiceImpl() {
        this.externalServiceManager = new ExternalServiceManager();
    }

    @Override
    public Response createExternalService(ExternalServiceDTO externalServiceDTO, MessageContext messageContext) throws APIMGovernanceException {
        ExternalService service = ExternalServiceMappingUtil.fromDTOToExternalService(externalServiceDTO);
        ExternalService createdService = externalServiceManager.addExternalService(service);
        ExternalServiceDTO responseDTO = ExternalServiceMappingUtil.fromExternalServiceToDTO(createdService);
        return Response.status(Response.Status.CREATED).entity(responseDTO).build();
    }

    @Override
    public Response deleteExternalService(String serviceId, MessageContext messageContext) throws APIMGovernanceException {
        externalServiceManager.deleteExternalService(serviceId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response getExternalServiceById(String serviceId, MessageContext messageContext) throws APIMGovernanceException {
        ExternalService service = externalServiceManager.getExternalServiceById(serviceId);
        if (service == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        ExternalServiceDTO responseDTO = ExternalServiceMappingUtil.fromExternalServiceToDTO(service);
        return Response.ok().entity(responseDTO).build();
    }

    @Override
    public Response getExternalServices(MessageContext messageContext) throws APIMGovernanceException {
        ExternalServiceList serviceList = externalServiceManager.getExternalServices();
        return Response.ok().entity(ExternalServiceMappingUtil.fromExternalServiceListToDTO(serviceList)).build();
    }

    @Override
    public Response updateExternalService(String serviceId, ExternalServiceDTO externalServiceDTO, 
                                          MessageContext messageContext) throws APIMGovernanceException {
        ExternalService service = ExternalServiceMappingUtil.fromDTOToExternalService(externalServiceDTO);
        service.setId(serviceId);
        ExternalService updatedService = externalServiceManager.updateExternalService(service);
        ExternalServiceDTO responseDTO = ExternalServiceMappingUtil.fromExternalServiceToDTO(updatedService);
        return Response.ok().entity(responseDTO).build();
    }
}
