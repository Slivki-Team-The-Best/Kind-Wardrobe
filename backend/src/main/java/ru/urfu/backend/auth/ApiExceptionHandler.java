package ru.urfu.backend.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail invalidCredentials(InvalidCredentialsException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	/** Роль не подходит (например, USER открывает эндпоинт организатора). */
	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail accessDenied(AccessDeniedException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Недостаточно прав");
	}

	/** Ошибки валидации тела запроса: общий текст + сообщения по полям в "errors" (для подсветки полей формы). */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), fieldMessage(error.getCode(), error.getDefaultMessage())));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Проверьте заполнение полей");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	private static String fieldMessage(String code, String defaultMessage) {
		return switch (code == null ? "" : code) {
			case "NotBlank", "NotEmpty", "NotNull" -> "Обязательное поле";
			case "Size" -> "Слишком длинное значение";
			case "Pattern" -> defaultMessage;
			default -> "Некорректное значение";
		};
	}
}
