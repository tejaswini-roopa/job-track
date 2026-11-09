package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jobtrack.dto.request.LoginRequest;
import com.jobtrack.dto.request.RegisterRequest;
import com.jobtrack.dto.response.LoginResponse;
import com.jobtrack.dto.response.UserResponse;
import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.repository.StudentProfileRepository;
import com.jobtrack.repository.UserRepository;
import com.jobtrack.security.JwtUtil;
import com.jobtrack.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerHashesPasswordAndCreatesStudent() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("Asha", "  A@B.com ", "Passw0rd!"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.email()).isEqualTo("a@b.com");
        verify(profileRepository).save(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Asha", "a@b.com", "Passw0rd!")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        UserPrincipal principal = new UserPrincipal(7L, "a@b.com", "HASH", "Asha", Role.STUDENT, true);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtUtil.generateToken(principal)).thenReturn("jwt-token");
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.login(new LoginRequest(" A@B.com ", "Passw0rd!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.role()).isEqualTo(Role.STUDENT);
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtUtil, never()).generateToken(any());
    }
}
