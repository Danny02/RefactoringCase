package dev.nullzwo.interview.refactoring;

import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.nullzwo.interview.refactoring.config.AuthConfig;
import dev.nullzwo.interview.refactoring.config.ServiceRealmConfigs;
import dev.nullzwo.interview.refactoring.errors.ApplicationException;

public class IssuerBasedConfigResolver {
    private static final String BEARER_JWT_REGEX = "^Bearer\\s(?<token>[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*)$";

    private final ServiceRealmConfigs configs;

    public IssuerBasedConfigResolver(ServiceRealmConfigs configs) {
        this.configs = configs;
    }

    public AuthConfig resolve(HttpRequest request) {
        Optional<String> authHeader = request.headers().firstValue("Authorization");

        return authHeader.map(header -> {
            String tokenString = retrieveTokenFromHeader(header);
            String realm = getRealmFromToken(tokenString);

            return lookupConfigByRealm(realm);
        }).orElseGet(() -> lookupConfigByRealm(configs.getDefaultRealm()));
    }

    private String retrieveTokenFromHeader(String authHeader) {
        Pattern bearerTokenPattern = Pattern.compile(BEARER_JWT_REGEX);
        Matcher bearerTokenMatcher = bearerTokenPattern.matcher(authHeader);

        if (!bearerTokenMatcher.matches()) {
            throw unauthorized("Bad request: Invalid token provided");
        }

        return bearerTokenMatcher.group("token");
    }

    private String getRealmFromToken(String tokenString) {
        DecodedJWT decodedJWT = JWT.decode(tokenString);
        String issuer = decodedJWT.getIssuer();
        return getRealmFromIssuer(issuer).orElseThrow(() -> unauthorized("Realm can not be resolved from issuer: " + issuer));
    }

    private AuthConfig lookupConfigByRealm(String realm) {
        AuthConfig config = this.configs.getAuthConfigOfRealm().get(realm);
        if (config == null) {
            throw unauthorized("Config for realm " + realm + " not found");
        }

        return config;
    }

    private ApplicationException unauthorized(String reason) {
        String realms = String.join(",", configs.getAuthConfigOfRealm().keySet());
        return new ApplicationException(401, reason, Map.of("WWW-Authenticate", "Bearer realm=\"" + realms + "\""));
    }

    private Optional<String> getRealmFromIssuer(String issuer) {
        // Create a Pattern object
        Pattern issuerUrlPattern = Pattern.compile(".*\\/realms\\/([a-z0-9\\-]+)\\/?.*");

        // Now create matcher object.
        Matcher m = issuerUrlPattern.matcher(issuer);

        if (m.find()) {
            return Optional.ofNullable(m.group(1));
        } else {
            return Optional.empty();
        }
    }
}
