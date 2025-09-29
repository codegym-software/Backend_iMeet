
package com.example.iMeetBE.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cognito.hosted-ui")
public class CognitoHostedUIConfig {

    
    private String domain;
    private String clientId;
    private String redirectUri;
    private String responseType;
    private String scopes;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String buildLoginUrl() {
        return String.format(
            "https://%s/login?client_id=%s&response_type=%s&scope=%s&redirect_uri=%s",
            domain,
            clientId,
            responseType,
            scopes.replace(",", "+"),
            redirectUri
        );
    }

    public String buildLoginUrlWithPrompt() {
        // Luôn hiện màn hình chọn tài khoản khi đăng nhập lại
        return String.format(
            "https://%s/login?client_id=%s&response_type=%s&scope=%s&redirect_uri=%s&prompt=select_account&max_age=0",
            domain,
            clientId,
            responseType,
            scopes.replace(",", "+"),
            redirectUri
        );
    }

    public String buildLogoutUrl() {
        return String.format(
            "https://%s/logout?client_id=%s&logout_uri=%s",
            domain,
            clientId,
            redirectUri
        );
    }
}