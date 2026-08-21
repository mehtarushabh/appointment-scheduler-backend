package com.appointmentscheduler.common;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.appointmentscheduler.dto.DoctorLeaveDtos.LeaveConflictResponse;

/** Maps every error this API can produce to the structured ErrorResponse shape (Principle II). */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus())
			.body(ErrorResponse.of(ex.getStatus().value(), ex.getStatus().getReasonPhrase(), ex.getMessage()));
	}

	/**
	 * Safety net for a uniqueness race the upfront existsBy... checks can't catch (two concurrent
	 * requests both pass the check before either commits) — the database's own unique constraint
	 * is the real guarantee (FR-009, FR-018a), this just maps its failure to the same 409 shape.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "Conflict", "This value is already in use."));
	}

	@ExceptionHandler(LeaveConflictException.class)
	public ResponseEntity<LeaveConflictResponse> handleLeaveConflict(LeaveConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getResponse());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Forbidden", "You do not have access to this resource."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
			.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
			.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Validation failed.", fieldErrors));
	}
}
