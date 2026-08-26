package com.portfolio.onthisday.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps application exceptions to RFC-7807 {@link ProblemDetail} responses for the REST API.
 * Only applies to {@code @RestController}s, so it doesn't interfere with the Thymeleaf views.
 */
@RestControllerAdvice(basePackages = "com.portfolio.onthisday.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public ProblemDetail handleNotFound(EventNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Event not found");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler({IllegalArgumentException.class, jakarta.validation.ConstraintViolationException.class})
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return problem;
    }
}
