package org.wso2.carbon.apimgt.governance.rest.api;

import org.wso2.carbon.apimgt.governance.rest.api.*;
import org.wso2.carbon.apimgt.governance.rest.api.dto.*;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;

import org.wso2.carbon.apimgt.governance.rest.api.dto.ErrorDTO;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceDTO;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceListDTO;

import java.util.List;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


public interface ExternalServicesApiService {
      public Response createExternalService(ExternalServiceDTO externalServiceDTO, MessageContext messageContext) throws APIMGovernanceException;
      public Response deleteExternalService(String serviceId, MessageContext messageContext) throws APIMGovernanceException;
      public Response getExternalServiceById(String serviceId, MessageContext messageContext) throws APIMGovernanceException;
      public Response getExternalServices(MessageContext messageContext) throws APIMGovernanceException;
      public Response updateExternalService(String serviceId, ExternalServiceDTO externalServiceDTO, MessageContext messageContext) throws APIMGovernanceException;
}
