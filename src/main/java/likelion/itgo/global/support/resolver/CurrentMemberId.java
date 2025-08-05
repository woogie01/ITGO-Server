package likelion.itgo.global.support.resolver;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(hidden = true)
public @interface CurrentMemberId {
}