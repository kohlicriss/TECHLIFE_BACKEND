package com.example.employee.security;

import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckEmployeeAccess {
    String param() default "employeeId";   // PathVariable name
    String[] roles() default {};           // Allowed roles
}
