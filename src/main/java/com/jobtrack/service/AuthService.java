package com.jobtrack.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.request.LoginRequest;
import com.jobtrack.dto.request.RegisterRequest;
import com.jobtrack.dto.response.LoginResponse;
import com.jobtrack.dto.response.UserResponse;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.StudentProfileRepository;
import com.jobtrack.repository.UserRepository;
import com.jobtrack.security.JwtUtil;
import com.jobtrack.security.UserPrincipal;

/** Registration and login. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       StudentProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user = userRepository.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profileRepository.save(profile);

        return EntityMapper.toUserResponse(user);
    }

    /**
     * The AuthenticationManager checks the password against the BCrypt hash and rejects
     * disabled accounts; failures surface as AuthenticationException (HTTP 401).
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new LoginResponse(jwtUtil.generateToken(principal), "Bearer", jwtUtil.getExpirationMs(),
                principal.getId(), principal.getUsername(), principal.getFullName(), principal.getRole());
    }
}
