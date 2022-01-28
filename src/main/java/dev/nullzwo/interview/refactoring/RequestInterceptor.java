package dev.nullzwo.interview.refactoring;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.nullzwo.interview.refactoring.config.AuthConfig;
import dev.nullzwo.interview.refactoring.config.ServiceRealmConfigs;
import dev.nullzwo.interview.refactoring.errors.ApplicationException;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestInterceptor {
    private static Pattern ISSUER_URL_PATTERN = Pattern.compile(".*/realms/([a-z0-9\\-]+)/?.*");
    private static final Pattern BEARER_JWT_PATTERN =
            Pattern.compile("^Bearer\\s(?<token>[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*)$");

    private final ServiceRealmConfigs configs;

    public RequestInterceptor(ServiceRealmConfigs configs) {
        this.configs = configs;
    }

    public void apply(HttpRequest.Builder requestBuilder) {
        Optional<String> authHeader = requestBuilder.build().headers().firstValue("Authorization");
        String realm = getRealmFromHeader(authHeader.orElse(null));

        String serviceUrl =
                Optional.ofNullable(configs.getAuthConfigOfRealm().get(realm)).map(AuthConfig::getServiceUrl)
                        .orElseThrow(() -> new ApplicationException(500, "no service url", Map.of()));

        requestBuilder.uri(URI.create(serviceUrl));
    }

    private String getRealmFromHeader(String authHeader) {
        String realm = configs.getDefaultRealm();

        if (authHeader != null && !authHeader.isBlank()) {
            Matcher bearerTokenMatcher = BEARER_JWT_PATTERN.matcher(authHeader);
            realm = bearerTokenMatcher.matches() ? getRealmFromToken(bearerTokenMatcher.group("token")) : realm;
        }
        return realm;
    }

    private String getRealmFromToken(String tokenString) {
        try {
            DecodedJWT decodedJWT = JWT.decode(tokenString);
            String issuer = decodedJWT.getIssuer();
            return this.getRealmFromIssuer(issuer)
                    .orElseThrow(() -> new IllegalArgumentException("Realm can not be resolved from issuer: " + issuer));
        } catch (JWTDecodeException ex) {
            return configs.getDefaultRealm();
        }
    }

    private Optional<String> getRealmFromIssuer(String issuer) {
        Matcher m = ISSUER_URL_PATTERN.matcher(issuer);
        return m.find() ? Optional.ofNullable(m.group(1)) : Optional.empty();
    }
}
