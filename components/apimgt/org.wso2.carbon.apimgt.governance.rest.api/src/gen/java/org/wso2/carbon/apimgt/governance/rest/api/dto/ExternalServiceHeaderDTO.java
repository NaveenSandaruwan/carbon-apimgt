package org.wso2.carbon.apimgt.governance.rest.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;

/**
 * Header details for external service
 **/

import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.Valid;

@ApiModel(description = "Header details for external service")

public class ExternalServiceHeaderDTO   {
  
    private Integer id = null;
    private String headerKey = null;
    private String headerValue = null;

          @XmlType(name="CategoryEnum")
    @XmlEnum(String.class)
    public enum CategoryEnum {
        STANDARD("Standard"),
        SECURITY("Security");
        private String value;

        CategoryEnum (String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static CategoryEnum fromValue(String v) {
            for (CategoryEnum b : CategoryEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
return null;
        }
    } 
    private CategoryEnum category = null;

  /**
   * ID of the header
   **/
  public ExternalServiceHeaderDTO id(Integer id) {
    this.id = id;
    return this;
  }

  
  @ApiModelProperty(example = "1", value = "ID of the header")
  @JsonProperty("id")
  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Header Key
   **/
  public ExternalServiceHeaderDTO headerKey(String headerKey) {
    this.headerKey = headerKey;
    return this;
  }

  
  @ApiModelProperty(example = "Authorization", required = true, value = "Header Key")
  @JsonProperty("headerKey")
  @NotNull
  public String getHeaderKey() {
    return headerKey;
  }
  public void setHeaderKey(String headerKey) {
    this.headerKey = headerKey;
  }

  /**
   * Header Value
   **/
  public ExternalServiceHeaderDTO headerValue(String headerValue) {
    this.headerValue = headerValue;
    return this;
  }

  
  @ApiModelProperty(example = "Bearer {TOKEN}", required = true, value = "Header Value")
  @JsonProperty("headerValue")
  @NotNull
  public String getHeaderValue() {
    return headerValue;
  }
  public void setHeaderValue(String headerValue) {
    this.headerValue = headerValue;
  }

  /**
   * Header category (Standard/Security)
   **/
  public ExternalServiceHeaderDTO category(CategoryEnum category) {
    this.category = category;
    return this;
  }

  
  @ApiModelProperty(example = "Security", value = "Header category (Standard/Security)")
  @JsonProperty("category")
  public CategoryEnum getCategory() {
    return category;
  }
  public void setCategory(CategoryEnum category) {
    this.category = category;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalServiceHeaderDTO externalServiceHeader = (ExternalServiceHeaderDTO) o;
    return Objects.equals(id, externalServiceHeader.id) &&
        Objects.equals(headerKey, externalServiceHeader.headerKey) &&
        Objects.equals(headerValue, externalServiceHeader.headerValue) &&
        Objects.equals(category, externalServiceHeader.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, headerKey, headerValue, category);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalServiceHeaderDTO {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    headerKey: ").append(toIndentedString(headerKey)).append("\n");
    sb.append("    headerValue: ").append(toIndentedString(headerValue)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
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

