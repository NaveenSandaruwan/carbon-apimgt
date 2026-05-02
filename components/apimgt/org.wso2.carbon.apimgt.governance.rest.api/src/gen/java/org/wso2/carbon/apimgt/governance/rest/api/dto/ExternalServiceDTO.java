package org.wso2.carbon.apimgt.governance.rest.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.apimgt.governance.rest.api.dto.ExternalServiceHeaderDTO;
import javax.validation.constraints.*;

/**
 * External service details
 **/

import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.Valid;

@ApiModel(description = "External service details")

public class ExternalServiceDTO   {
  
    private String id = null;
    private String name = null;
    private String url = null;
    private String prompt = null;
    private Integer timeoutMs = null;
    private Integer retryCount = null;
    private Boolean isLLM = null;
    private List<ExternalServiceHeaderDTO> headers = new ArrayList<ExternalServiceHeaderDTO>();

  /**
   * UUID of the external service
   **/
  public ExternalServiceDTO id(String id) {
    this.id = id;
    return this;
  }

  
  @ApiModelProperty(example = "123e4567-e89b-12d3-a456-426614174000", value = "UUID of the external service")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Name of the external service
   **/
  public ExternalServiceDTO name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(example = "MyAI Service", required = true, value = "Name of the external service")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * URL of the external service
   **/
  public ExternalServiceDTO url(String url) {
    this.url = url;
    return this;
  }

  
  @ApiModelProperty(example = "https://api.openai.com/v1/models", required = true, value = "URL of the external service")
  @JsonProperty("url")
  @NotNull
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Prompt to be used with the external service
   **/
  public ExternalServiceDTO prompt(String prompt) {
    this.prompt = prompt;
    return this;
  }

  
  @ApiModelProperty(example = "Review this schema:", value = "Prompt to be used with the external service")
  @JsonProperty("prompt")
  public String getPrompt() {
    return prompt;
  }
  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  /**
   * Timeout in milliseconds
   **/
  public ExternalServiceDTO timeoutMs(Integer timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

  
  @ApiModelProperty(example = "5000", value = "Timeout in milliseconds")
  @JsonProperty("timeoutMs")
  public Integer getTimeoutMs() {
    return timeoutMs;
  }
  public void setTimeoutMs(Integer timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  /**
   * Number of retries
   **/
  public ExternalServiceDTO retryCount(Integer retryCount) {
    this.retryCount = retryCount;
    return this;
  }

  
  @ApiModelProperty(example = "3", value = "Number of retries")
  @JsonProperty("retryCount")
  public Integer getRetryCount() {
    return retryCount;
  }
  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  /**
   * Whether this external service is an LLM provider
   **/
  public ExternalServiceDTO isLLM(Boolean isLLM) {
    this.isLLM = isLLM;
    return this;
  }

  
  @ApiModelProperty(example = "true", value = "Whether this external service is an LLM provider")
  @JsonProperty("isLLM")
  public Boolean isIsLLM() {
    return isLLM;
  }
  public void setIsLLM(Boolean isLLM) {
    this.isLLM = isLLM;
  }

  /**
   * List of headers for the external service
   **/
  public ExternalServiceDTO headers(List<ExternalServiceHeaderDTO> headers) {
    this.headers = headers;
    return this;
  }

  
  @ApiModelProperty(value = "List of headers for the external service")
      @Valid
  @JsonProperty("headers")
  public List<ExternalServiceHeaderDTO> getHeaders() {
    return headers;
  }
  public void setHeaders(List<ExternalServiceHeaderDTO> headers) {
    this.headers = headers;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalServiceDTO externalService = (ExternalServiceDTO) o;
    return Objects.equals(id, externalService.id) &&
        Objects.equals(name, externalService.name) &&
        Objects.equals(url, externalService.url) &&
        Objects.equals(prompt, externalService.prompt) &&
        Objects.equals(timeoutMs, externalService.timeoutMs) &&
        Objects.equals(retryCount, externalService.retryCount) &&
        Objects.equals(isLLM, externalService.isLLM) &&
        Objects.equals(headers, externalService.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, url, prompt, timeoutMs, retryCount, isLLM, headers);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalServiceDTO {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    prompt: ").append(toIndentedString(prompt)).append("\n");
    sb.append("    timeoutMs: ").append(toIndentedString(timeoutMs)).append("\n");
    sb.append("    retryCount: ").append(toIndentedString(retryCount)).append("\n");
    sb.append("    isLLM: ").append(toIndentedString(isLLM)).append("\n");
    sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

