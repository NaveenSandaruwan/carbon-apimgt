package org.wso2.carbon.apimgt.governance.rest.api;

import org.wso2.carbon.apimgt.governance.rest.api.dto.ErrorDTO;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceDTO;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceListDTO;
import org.wso2.carbon.apimgt.governance.rest.api.ExternalServicesApiService;
import org.wso2.carbon.apimgt.governance.rest.api.impl.ExternalServicesApiServiceImpl;
import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.inject.Inject;

import io.swagger.annotations.*;
import java.io.InputStream;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import java.util.Map;
import java.util.List;
import javax.validation.constraints.*;
@Path("/external-services")

@Api(description = "the external-services API")




public class ExternalServicesApi  {

  @Context MessageContext securityContext;

ExternalServicesApiService delegate = new ExternalServicesApiServiceImpl();


    @POST
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create a new external service.", notes = "Creates a new external service.", response = ExternalServiceDTO.class, authorizations = {
        @Authorization(value = "OAuth2Security", scopes = {
            @AuthorizationScope(scope = "apim:gov_rule_manage", description = "Manage governance rulesets")
        })
    }, tags={ "External Services",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Created. Successful response with the newly created object as entity in the body.", response = ExternalServiceDTO.class),
        @ApiResponse(code = 400, message = "Bad Request. Invalid request or validation error.", response = ErrorDTO.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorDTO.class) })
    public Response createExternalService(@ApiParam(value = "External service object that needs to be added" ,required=true) ExternalServiceDTO externalServiceDTO) throws APIMGovernanceException{
        return delegate.createExternalService(externalServiceDTO, securityContext);
    }

    @DELETE
    @Path("/{serviceId}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Delete an external service.", notes = "Deletes an existing external service using the service ID.", response = Void.class, authorizations = {
        @Authorization(value = "OAuth2Security", scopes = {
            @AuthorizationScope(scope = "apim:gov_rule_manage", description = "Manage governance rulesets")
        })
    }, tags={ "External Services",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "No Content. Resource successfully deleted.", response = Void.class),
        @ApiResponse(code = 404, message = "Not Found. Requested external service does not exist.", response = ErrorDTO.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorDTO.class) })
    public Response deleteExternalService(@ApiParam(value = "**UUID** of the External Service. ",required=true) @PathParam("serviceId") String serviceId) throws APIMGovernanceException{
        return delegate.deleteExternalService(serviceId, securityContext);
    }

    @GET
    @Path("/{serviceId}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Get details of an external service.", notes = "Retrieves the details of an external service using the service ID.", response = ExternalServiceDTO.class, authorizations = {
        @Authorization(value = "OAuth2Security", scopes = {
            @AuthorizationScope(scope = "apim:gov_rule_read", description = "Read governance rulesets")
        })
    }, tags={ "External Services",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK. Successful response with the external service details.", response = ExternalServiceDTO.class),
        @ApiResponse(code = 404, message = "Not Found. Requested external service does not exist.", response = ErrorDTO.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorDTO.class) })
    public Response getExternalServiceById(@ApiParam(value = "**UUID** of the External Service. ",required=true) @PathParam("serviceId") String serviceId) throws APIMGovernanceException{
        return delegate.getExternalServiceById(serviceId, securityContext);
    }

    @GET
    
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Retrieves a list of external services.", notes = "Returns a list of all external services.", response = ExternalServiceListDTO.class, authorizations = {
        @Authorization(value = "OAuth2Security", scopes = {
            @AuthorizationScope(scope = "apim:gov_rule_read", description = "Read governance rulesets")
        })
    }, tags={ "External Services",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK. Successful response with a list of external services.", response = ExternalServiceListDTO.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorDTO.class) })
    public Response getExternalServices() throws APIMGovernanceException{
        return delegate.getExternalServices(securityContext);
    }

    @PUT
    @Path("/{serviceId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update details of an external service.", notes = "Updates the details of an existing external service using the service ID.", response = ExternalServiceDTO.class, authorizations = {
        @Authorization(value = "OAuth2Security", scopes = {
            @AuthorizationScope(scope = "apim:gov_rule_manage", description = "Manage governance rulesets")
        })
    }, tags={ "External Services" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK. Successful response with the updated external service.", response = ExternalServiceDTO.class),
        @ApiResponse(code = 400, message = "Bad Request. Invalid request or validation error.", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Not Found. Requested external service does not exist.", response = ErrorDTO.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorDTO.class) })
    public Response updateExternalService(@ApiParam(value = "**UUID** of the External Service. ",required=true) @PathParam("serviceId") String serviceId, @ApiParam(value = "External service object that needs to be updated" ,required=true) ExternalServiceDTO externalServiceDTO) throws APIMGovernanceException{
        return delegate.updateExternalService(serviceId, externalServiceDTO, securityContext);
    }
}
