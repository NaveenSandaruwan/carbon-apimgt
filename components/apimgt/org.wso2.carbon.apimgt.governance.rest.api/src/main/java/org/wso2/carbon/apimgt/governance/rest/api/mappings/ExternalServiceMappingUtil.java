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

package org.wso2.carbon.apimgt.governance.rest.api.mappings;

import org.wso2.carbon.apimgt.governance.api.model.ExternalService;
import org.wso2.carbon.apimgt.governance.api.model.ExternalServiceHeader;
import org.wso2.carbon.apimgt.governance.api.model.ExternalServiceList;
import org.wso2.carbon.apimgt.governance.api.model.HeaderCategory;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceDTO;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceHeaderDTO;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceListDTO;
import org.wso2.carbon.apimgt.governance.rest.api.util.HeaderValueMaskingUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapping util for External Services
 */
public class ExternalServiceMappingUtil {

    public static ExternalServiceDTO fromExternalServiceToDTO(ExternalService service) {
        if (service == null) {
            return null;
        }
        ExternalServiceDTO dto = new ExternalServiceDTO();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setUrl(service.getUrl());
        dto.setPrompt(service.getPrompt());
        dto.setTimeoutMs(service.getTimeoutMs());
        dto.setRetryCount(service.getRetryCount());
        dto.setIsLLM(getIsLLMFromService(service)); // ExternalServiceDTO.isIsLLM() returns this
        
        List<ExternalServiceHeaderDTO> headerDTOs = new ArrayList<>();
        if (service.getHeaders() != null) {
            for (ExternalServiceHeader header : service.getHeaders()) {
                ExternalServiceHeaderDTO headerDTO = new ExternalServiceHeaderDTO();
                // Convert model String id to DTO Integer if numeric, otherwise leave null
                Integer headerIdInt = null;
                try {
                    if (header.getId() != null) {
                        headerIdInt = Integer.valueOf(header.getId());
                    }
                } catch (NumberFormatException ignored) {
                    // keep null when id is not an integer (e.g., UUID)
                }
                headerDTO.setId(headerIdInt);
                headerDTO.setHeaderKey(header.getHeaderKey());
                
                // Mask header value if category is SECURITY
                String headerValue = header.getHeaderValue();
                if (header.getCategory() != null && 
                    header.getCategory() == HeaderCategory.SECURITY) {
                    headerValue = HeaderValueMaskingUtil.maskValue(headerValue);
                }
                headerDTO.setHeaderValue(headerValue);
                
                if (header.getCategory() != null) {
                    headerDTO.setCategory(ExternalServiceHeaderDTO.CategoryEnum.valueOf(header.getCategory().name()));
                }
                headerDTOs.add(headerDTO);
            }
        }
        dto.setHeaders(headerDTOs);
        return dto;
    }

    public static ExternalService fromDTOToExternalService(ExternalServiceDTO dto) {
        if (dto == null) {
            return null;
        }
        ExternalService service = new ExternalService();
        service.setId(dto.getId());
        service.setName(dto.getName());
        service.setUrl(dto.getUrl());
        service.setPrompt(dto.getPrompt());
        service.setTimeoutMs(dto.getTimeoutMs());
        service.setRetryCount(dto.getRetryCount());
        setIsLLMOnService(service, dto.isIsLLM() != null ? dto.isIsLLM() : false);
        
        List<ExternalServiceHeader> headers = new ArrayList<>();
        if (dto.getHeaders() != null) {
            for (ExternalServiceHeaderDTO headerDTO : dto.getHeaders()) {
                ExternalServiceHeader header = new ExternalServiceHeader();
                // Convert DTO Integer id to model String
                header.setId(headerDTO.getId() != null ? String.valueOf(headerDTO.getId()) : null);
                header.setHeaderKey(headerDTO.getHeaderKey());
                header.setHeaderValue(headerDTO.getHeaderValue());
                if (headerDTO.getCategory() != null) {
                    header.setCategory(HeaderCategory.valueOf(headerDTO.getCategory().name()));
                }
                headers.add(header);
            }
        }
        service.setHeaders(headers);
        return service;
    }

    public static ExternalServiceListDTO fromExternalServiceListToDTO(ExternalServiceList serviceList) {
        ExternalServiceListDTO listDTO = new ExternalServiceListDTO();
        if (serviceList == null) {
            listDTO.setCount(0);
            return listDTO;
        }
        listDTO.setCount(serviceList.getCount());
        List<ExternalServiceDTO> dtoList = new ArrayList<>();
        if (serviceList.getList() != null) {
            for (ExternalService service : serviceList.getList()) {
                dtoList.add(fromExternalServiceToDTO(service));
            }
        }
        listDTO.setList(dtoList);
        return listDTO;
    }

    private static Boolean getIsLLMFromService(ExternalService service) {
        if (service == null) {
            return null;
        }
        try {
            // try direct getter via reflection to avoid compile-time dependency on exact API
            java.lang.reflect.Method m = service.getClass().getMethod("getIsLLM");
            Object val = m.invoke(service);
            return (Boolean) val;
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Method m2 = service.getClass().getMethod("isIsLLM");
                Object val = m2.invoke(service);
                return (Boolean) val;
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void setIsLLMOnService(ExternalService service, boolean value) {
        if (service == null) {
            return;
        }
        try {
            java.lang.reflect.Method m = service.getClass().getMethod("setIsLLM", Boolean.class);
            m.invoke(service, Boolean.valueOf(value));
            return;
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Method m2 = service.getClass().getMethod("setIsLLM", boolean.class);
                m2.invoke(service, value);
                return;
            } catch (Exception ex) {
                // ignore
            }
        } catch (Exception e) {
            // ignore
        }
    }

}
