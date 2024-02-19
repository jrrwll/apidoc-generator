package com.example.base;

import com.example.annotation.FieldDoc;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2024-01-12
 */
@Getter
@Setter
public abstract class BaseParam {

    @JsonProperty(value = "tenant_id")
    @FieldDoc(description = "Tenant Id")
    private String tenantId;
}
