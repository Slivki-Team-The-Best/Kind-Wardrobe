package ru.urfu.backend.common;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final Map<String, String> errors;

	public ApiException(HttpStatus status, String message) {
		this(status, message, Map.of());
	}

	public ApiException(HttpStatus status, String message, Map<String, String> errors) {
		super(message);
		this.status = status;
		this.errors = Map.copyOf(errors);
	}

	public static ApiException badRequest(Map<String, String> errors) {
		return new ApiException(HttpStatus.BAD_REQUEST, "Проверьте заполнение полей", errors);
	}

	public static ApiException badRequest(String field, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message, Map.of(field, message));
	}

	public static ApiException conflict(String field, String message) {
		return new ApiException(HttpStatus.CONFLICT, message, Map.of(field, message));
	}

	public static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, message);
	}

	public static ApiException forbidden(String message) {
		return new ApiException(HttpStatus.FORBIDDEN, message);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public Map<String, String> getErrors() {
		return errors;
	}
}
