package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.response.UserResponse;
import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService service;

    private User user(Role role) {
        User u = new User();
        u.setId(3L);
        u.setEmail("x@test.com");
        u.setFullName("X");
        u.setRole(role);
        return u;
    }

    @Test
    void disablingAStudentUpdatesTheFlag() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(Role.STUDENT)));

        UserResponse response = service.setEnabled(3L, false);

        assertThat(response.enabled()).isFalse();
    }

    @Test
    void adminAccountsCannotBeManagedHere() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(Role.ADMIN)));

        assertThatThrownBy(() -> service.setEnabled(3L, false)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.deleteStudent(3L)).isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deletingAStudentRemovesTheUser() {
        User student = user(Role.STUDENT);
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));

        service.deleteStudent(3L);

        verify(userRepository).delete(student);
    }

    @Test
    void unknownUserThrowsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteStudent(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
