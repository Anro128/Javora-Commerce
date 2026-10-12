package com.javora.commerce.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	ProblemDetail handleBusiness(BusinessException ex) {
		return problem(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
		return problem(HttpStatus.CONFLICT, "The data was changed by someone else, please reload and try again");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return problem(HttpStatus.CONFLICT, "The request conflicts with existing data");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed");
		body.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	private static ProblemDetail problem(HttpStatusCode status, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("about:blank"));
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}

}
