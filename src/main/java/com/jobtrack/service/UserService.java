package com.jobtrack.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.dto.response.UserResponse;
import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.UserRepository;

/** Admin operations on student accounts. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listStudents(Pageable pageable) {
        return PageResponse.from(userRepository.findByRole(Role.STUDENT, pageable)
                .map(EntityMapper::toUserResponse));
    }

    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User user = findStudent(userId);
        user.setEnabled(enabled);
        return EntityMapper.toUserResponse(user);
    }

    /** The database cascades the delete to profile, applications, interviews and saved jobs. */
    @Transactional
    public void deleteStudent(Long userId) {
        userRepository.delete(findStudent(userId));
    }

    private User findStudent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getRole() != Role.STUDENT) {
            throw new BusinessRuleException("Only student accounts can be managed here");
        }
        return user;
    }
}
