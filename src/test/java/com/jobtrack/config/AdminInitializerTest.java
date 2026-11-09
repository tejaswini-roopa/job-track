package com.jobtrack.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.repository.UserRepository;

class AdminInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void createsAdminWhenMissing() {
        when(userRepository.findByEmail("admin@jobtrack.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass1")).thenReturn("$2a$hash");
        AdminInitializer initializer = new AdminInitializer(userRepository, passwordEncoder,
                "Admin@JobTrack.local", "StrongPass1", "Placement Admin");

        initializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@jobtrack.local");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$hash");
    }

    @Test
    void doesNothingWithoutCredentials() {
        new AdminInitializer(userRepository, passwordEncoder, "", "", "Placement Admin").run(null);

        verifyNoInteractions(userRepository);
        verify(passwordEncoder, never()).encode(any());
    }
}
