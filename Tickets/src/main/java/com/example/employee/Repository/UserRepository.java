package com.example.employee.Repository;

import com.example.employee.entity.Roles;
import com.example.employee.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    List<User> findByRoles(Roles roles);
    
}

