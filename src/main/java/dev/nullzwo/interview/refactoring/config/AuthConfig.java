package dev.nullzwo.interview.refactoring.config;

import lombok.Value;

@Value
public class AuthConfig {
    String authServerUrl;
    String serviceUrl;
}
