package com.example.employee.dto;

import com.example.employee.entity.Roles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class authDTO {
        private String employeeId;
        private Roles roles;

}
