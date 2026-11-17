package com.example.accountservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AccountControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AccountRepository repository;
    @MockBean JwtService jwtService;

    @Test
    void retrievesAccountWithValidJwt() throws Exception {
        Account account = new Account(1L, "Asha", new BigDecimal("1000.00"));
        when(jwtService.extractUsername("valid")).thenReturn("admin");
        when(jwtService.isTokenValid("valid", "admin")).thenReturn(true);
        when(repository.findById(1L)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/internal/accounts/1").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
    }

    @Test
    void createsAccountWithValidJwt() throws Exception {
        Account account = new Account(null, "Ravi", new BigDecimal("500.00"));
        when(jwtService.extractUsername("valid")).thenReturn("admin");
        when(jwtService.isTokenValid("valid", "admin")).thenReturn(true);
        when(repository.save(any(Account.class))).thenReturn(new Account(2L, "Ravi", new BigDecimal("500.00")));

        mockMvc.perform(post("/internal/accounts").header("Authorization", "Bearer valid")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(account)))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsMissingJwt() throws Exception {
        when(repository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/internal/accounts")).andExpect(status().isUnauthorized());
    }
}
