package com.example.employee.Controller;

import com.example.employee.dto.authDTO;
import com.example.employee.entity.Roles;
import com.example.employee.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/create")
    public ResponseEntity<authDTO> createAuth(@RequestBody authDTO authDto) {
        authDTO createdAuth = authService.createAuth(authDto);
        return new ResponseEntity<>(createdAuth, HttpStatus.CREATED);
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<authDTO> getAuthByEmployeeId(@PathVariable String employeeId) {
        authDTO authDto = authService.getAuthByEmployeeId(employeeId);
        return authDto != null ? ResponseEntity.ok(authDto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<authDTO>> getAllAuth() {
        List<authDTO> authDtos = authService.getAllAuth();
        return ResponseEntity.ok(authDtos);
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<authDTO>> getAuthByRole(@PathVariable Roles role) {
        List<authDTO> authDtos = authService.getAuthByRole(role);
        return ResponseEntity.ok(authDtos);
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<authDTO> updateAuth(@PathVariable String employeeId, @RequestBody authDTO authDto) {
        if (!employeeId.equals(authDto.getEmployeeId())) {
            return ResponseEntity.badRequest().build();
        }
        authDTO updatedAuth = authService.updateAuth(authDto);
        return updatedAuth != null ? ResponseEntity.ok(updatedAuth) : ResponseEntity.notFound().build();
    }



    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteAuth(@PathVariable String employeeId) {
        boolean isDeleted = authService.deleteAuth(employeeId);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}