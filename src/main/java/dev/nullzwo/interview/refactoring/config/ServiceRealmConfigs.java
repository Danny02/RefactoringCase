package dev.nullzwo.interview.refactoring.config;

import java.util.Map;

import lombok.Value;

@Value
public class ServiceRealmConfigs {
    String defaultRealm;
    Map<String, AuthConfig> authConfigOfRealm;
}
