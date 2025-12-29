package com.auth.jwtsecurity.repository;

import com.auth.jwtsecurity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    @Query(nativeQuery = true, value = "SELECT * FROM auth.application_user AS A WHERE A.username = :username AND A.tenant_id = :tenant_id")
    Optional<User> findByUsernameAndTenantId(String username, String tenant_id);
    Boolean existsByUsername(String username);
    @Query(nativeQuery = true, value = "SELECT * FROM auth.application_user AS A WHERE A.email = :email AND A.tenant_id = :tenant_id")
    Optional<User> findByEmailAndTenantId(String email, String tenant_id);
    @Query(nativeQuery = true, value = "SELECT * FROM auth.application_user AS A WHERE A.phone_number = :phoneNumber AND A.tenant_id = :tenant_id")
    Optional<User> findByPhoneNumberAndTenantId(String phoneNumber, String tenant_id);
}
