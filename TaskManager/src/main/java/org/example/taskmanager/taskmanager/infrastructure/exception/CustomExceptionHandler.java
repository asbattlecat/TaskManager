package org.example.taskmanager.taskmanager.infrastructure.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class CustomExceptionHandler {
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ProblemDetail handleAccessDeniedException(AccessDeniedException ex,
                                                   HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());

    problem.setTitle("Access Denied");
    problem.setInstance(URI.create(request.getRequestURI()));

    return problem;
  }

  @ExceptionHandler(AlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ProblemDetail handleAlreadyExistsException(AlreadyExistsException ex,
                                                    HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

    problem.setTitle("Already Exists");
    problem.setInstance(URI.create(request.getRequestURI()));

    return problem;
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex,
                                                HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());

    problem.setTitle("Invalid Credentials");
    problem.setInstance(URI.create(request.getRequestURI()));

    return problem;
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ProblemDetail handleNotFoundException(NotFoundException ex,
                                               HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

    problem.setTitle("Resource Not Found");
    problem.setInstance(URI.create(request.getRequestURI()));

    return problem;
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ProblemDetail handleIllegalStateException(IllegalStateException ex,
                                                   HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

    problem.setTitle("Illegal state");
    problem.setInstance(URI.create(request.getRequestURI()));

    return problem;
  }
}
