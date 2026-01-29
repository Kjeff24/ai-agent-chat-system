package com.aiagent.chatsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateModelConfigRequest {
    private String name;
    private String provider;
    private String model;
    private Map<String, Object> parameters;
    private Boolean isDefault;
    private Boolean isActive;
}
