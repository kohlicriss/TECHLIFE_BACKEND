package com.example.RealTime_Attendance.Security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {
    String value(); // permission name, e.g., "ADD_EMPLOYEE"
    TypeVar type() default TypeVar.NONE; // By what type it is specified, Body or Path or Query or Header or a simple variable
    String MatchParmName() default ""; // By what name it is specified in the jwt token payload
    String MatchParmFromType() default ""; // By what name it is specified in the Json or Path or Query or Header if request body then BodyName.<variableName1>.<variableName2> (dot suppurated for nested objects)
    String[] MatchParmForRoles() default {}; // For what role thi rules must be applied
}

// Just give the butten name annotation can take out the role and give access if
// it matches with the role of the JWT token

// MatchParmName -> Indicates the name of the param from jwt token to be verifies(ex: employeeId, Role etc..)
// MatchParmFromUrl -> Indicates  is the path parameter is the url (like ../{xyz}/ or /.../{abc}/.. in the role then we must specify "xyz" or "abc" as the MatchParmFromUrl)
// MatchParmForRoles -> Indicates only match the above conditions(MatchParm) for this perticuler role, (example if butten is specified with number of roles then, if we want to match the above permissions to only specific roles then we must specify them here)