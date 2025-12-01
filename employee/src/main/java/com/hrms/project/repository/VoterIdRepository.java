package com.hrms.project.repository;


import com.hrms.project.entity.VoterDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoterIdRepository extends JpaRepository<VoterDetails,String> {


    Optional<VoterDetails> findByEmployee_EmployeeId(String employeeId);

    //boolean existsByVoterIdNumber(String );
}

