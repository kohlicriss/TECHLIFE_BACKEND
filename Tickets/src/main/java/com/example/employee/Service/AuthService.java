package com.example.employee.Service;


import com.example.employee.dto.authDTO;
import com.example.employee.entity.Roles;
import com.example.employee.entity.User;
import com.example.employee.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public authDTO createAuth(authDTO authDto) {
        User user = User.builder()
                .employeeId(authDto.getEmployeeId())
                .roles(authDto.getRoles())
                .build();

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    public authDTO getAuthByEmployeeId(String employeeId) {
        return userRepository.findById(employeeId)
                .map(this::convertToDto)
                .orElse(null);
    }

    public List<authDTO> getAllAuth() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<authDTO> getAuthByRole(Roles role) {
        return userRepository.findByRoles(role).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public authDTO updateAuth(authDTO authDto) {
        User existingUser = userRepository.findById(authDto.getEmployeeId())
                .orElse(null);

        if (existingUser == null) {
            return null;
        }

        existingUser.setRoles(authDto.getRoles());
        User updatedUser = userRepository.save(existingUser);
        return convertToDto(updatedUser);
    }

    public authDTO updateRole(String employeeId, Roles newRole) {
        User existingUser = userRepository.findById(employeeId)
                .orElse(null);

        if (existingUser == null) {
            return null;
        }

        existingUser.setRoles(newRole);
        User updatedUser = userRepository.save(existingUser);
        return convertToDto(updatedUser);
    }

    public boolean deleteAuth(String employeeId) {
        if (!userRepository.existsById(employeeId)) {
            return false;
        }
        userRepository.deleteById(employeeId);
        return true;
    }

    private authDTO convertToDto(User user) {
        return new authDTO(user.getEmployeeId(), user.getRoles());
    }
}