package com.mcp.client.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class McpOAuth2HybridExchangeFilterFunction implements ExchangeFilterFunction {

    private final OAuth2AuthorizedClientManager clientManager;
    private final String userRegistrationId;
    private final String systemRegistrationId;

    public McpOAuth2HybridExchangeFilterFunction(
            OAuth2AuthorizedClientManager clientManager,
            String userRegistrationId,
            String systemRegistrationId) {
        this.clientManager = clientManager;
        this.userRegistrationId = userRegistrationId;
        this.systemRegistrationId = systemRegistrationId;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Si l'utilisateur est authentifié, utiliser authorization_code
        // Sinon, utiliser client_credentials pour les appels système
        String registrationId = (authentication != null && authentication.isAuthenticated()) 
            ? userRegistrationId 
            : systemRegistrationId;

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId(registrationId)
            .principal(authentication != null ? authentication : createSystemPrincipal())
            .build();

        OAuth2AuthorizedClient authorizedClient = clientManager.authorize(authorizeRequest);
        
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return next.exchange(request);
        }

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        
        // Vérifier si le token est expiré
        if (accessToken.getExpiresAt() != null && accessToken.getExpiresAt().isBefore(Instant.now())) {
            // Le token est expiré, le client manager devrait le rafraîchir automatiquement
            // Réessayer avec une nouvelle autorisation
            authorizedClient = clientManager.authorize(authorizeRequest);
            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                return next.exchange(request);
            }
            accessToken = authorizedClient.getAccessToken();
        }

        ClientRequest authenticatedRequest = ClientRequest.from(request)
            .header("Authorization", "Bearer " + accessToken.getTokenValue())
            .build();

        return next.exchange(authenticatedRequest);
    }

    private Authentication createSystemPrincipal() {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "system",
            null,
            java.util.Collections.emptyList()
        );
    }
}

