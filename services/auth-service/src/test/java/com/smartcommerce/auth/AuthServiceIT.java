package com.smartcommerce.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.auth.api.dto.*;
import com.smartcommerce.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthServiceIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("register_whenValidRequest_thenReturns201WithTokens")
    void register_whenValidRequest_thenReturns201WithTokens() throws Exception {
        var request = new RegisterRequest(
            "test_" + System.currentTimeMillis() + "@example.com",
            "Password123!",
            "Test", "User", null
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value(request.email()));
    }

    @Test
    @DisplayName("register_whenSellerRoleSubmitted_thenCreatesCustomerOnly")
    void register_whenSellerRoleSubmitted_thenCreatesCustomerOnly() throws Exception {
        var request = Map.of(
            "email", "customer_only_" + System.currentTimeMillis() + "@example.com",
            "password", "Password123!",
            "firstName", "Customer",
            "lastName", "Only",
            "roles", java.util.List.of("SELLER", "ADMIN")
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.roles[?(@ == 'CUSTOMER')]").exists())
            .andExpect(jsonPath("$.user.roles[?(@ == 'SELLER')]").doesNotExist())
            .andExpect(jsonPath("$.user.roles[?(@ == 'ADMIN')]").doesNotExist());
    }

    @Test
    @DisplayName("login_whenValidCredentials_thenReturnsTokens")
    void login_whenValidCredentials_thenReturnsTokens() throws Exception {
        var email = "login_" + System.currentTimeMillis() + "@example.com";
        var password = "Password123!";

        var registerRequest = new RegisterRequest(email, password, "Login", "Test", null);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("refresh_whenValidToken_thenReturnsNewTokens")
    void refresh_whenValidToken_thenReturnsNewTokens() throws Exception {
        var email = "refresh_" + System.currentTimeMillis() + "@example.com";
        var registerRequest = new RegisterRequest(email, "Password123!", "Refresh", "Test", null);

        var result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        var auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(auth.refreshToken()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("refresh_whenTokenReused_thenReturns401")
    void refresh_whenTokenReused_thenReturns401() throws Exception {
        var email = "reuse_" + System.currentTimeMillis() + "@example.com";
        var registerRequest = new RegisterRequest(email, "Password123!", "Reuse", "Test", null);

        var result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        var auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        // Use refresh token once
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(auth.refreshToken()))))
            .andExpect(status().isOk());

        // Use same refresh token again — should fail
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(auth.refreshToken()))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("register_whenDuplicateEmail_thenReturns409")
    void register_whenDuplicateEmail_thenReturns409() throws Exception {
        var email = "dup_" + System.currentTimeMillis() + "@example.com";
        var request = new RegisterRequest(email, "Password123!", "Dup", "Test", null);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login_whenWrongPassword_thenReturns401")
    void login_whenWrongPassword_thenReturns401() throws Exception {
        var email = "wrongpw_" + System.currentTimeMillis() + "@example.com";
        var registerRequest = new RegisterRequest(email, "Password123!", "Wrong", "PW", null);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPassword!"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("becomeSeller_whenCalledTwice_thenReturnsSellerSessionBothTimes")
    void becomeSeller_whenCalledTwice_thenReturnsSellerSessionBothTimes() throws Exception {
        var email = "seller_" + System.currentTimeMillis() + "@example.com";
        var registerRequest = new RegisterRequest(email, "Password123!", "Seller", "Test", null);

        var registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        var registered = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);

        var firstResult = mockMvc.perform(post("/api/auth/become-seller")
                .header("Authorization", "Bearer " + registered.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.roles[?(@ == 'SELLER')]").exists())
            .andReturn();
        var sellerSession = objectMapper.readValue(firstResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(post("/api/auth/become-seller")
                .header("Authorization", "Bearer " + sellerSession.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.roles[?(@ == 'SELLER')]").exists());
    }
}
