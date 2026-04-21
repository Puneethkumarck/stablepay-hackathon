package com.stablepay.application.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.application.controller.auth.mapper.AuthResponseMapper;
import com.stablepay.application.dto.AuthResponse;
import com.stablepay.application.dto.ErrorResponse;
import com.stablepay.application.dto.RefreshTokenRequest;
import com.stablepay.application.dto.SocialLoginRequest;
import com.stablepay.domain.auth.handler.LogoutHandler;
import com.stablepay.domain.auth.handler.RefreshTokenHandler;
import com.stablepay.domain.auth.handler.SocialLoginHandler;
import com.stablepay.domain.auth.model.AuthPrincipal;
import com.stablepay.domain.auth.model.AuthTokenConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Social login, token refresh, and logout")
public class AuthController {

    private final SocialLoginHandler socialLoginHandler;
    private final RefreshTokenHandler refreshTokenHandler;
    private final LogoutHandler logoutHandler;
    private final AuthResponseMapper authResponseMapper;
    private final AuthTokenConfig authTokenConfig;

    @PostMapping("/social")
    @Operation(summary = "Social login", description = "Exchanges a Google ID token for app access and refresh tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "New user created and logged in"),
        @ApiResponse(responseCode = "200", description = "Returning user logged in"),
        @ApiResponse(responseCode = "400", description = "Unsupported auth provider",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or unverified ID token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest) {
        var ip = httpRequest.getRemoteAddr();
        var userAgent = httpRequest.getHeader("User-Agent");
        var result = socialLoginHandler.handle(request.provider(), request.idToken(), ip, userAgent);
        var expiresIn = (int) authTokenConfig.accessTtl().toSeconds();
        var response = authResponseMapper.toResponse(result, expiresIn);
        var status = result.newUser() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Rotates the refresh token and issues a new access token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AuthResponse refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        var ip = httpRequest.getRemoteAddr();
        var userAgent = httpRequest.getHeader("User-Agent");
        var session = refreshTokenHandler.handle(request.refreshToken(), ip, userAgent);
        var expiresIn = (int) authTokenConfig.accessTtl().toSeconds();
        return authResponseMapper.toRefreshResponse(session, expiresIn);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout", description = "Revokes all refresh tokens for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void logout(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        var ip = httpRequest.getRemoteAddr();
        var userAgent = httpRequest.getHeader("User-Agent");
        logoutHandler.handle(principal.id(), ip, userAgent);
    }
}
