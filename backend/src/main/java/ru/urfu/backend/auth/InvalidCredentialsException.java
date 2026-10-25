package ru.urfu.backend.auth;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Неверные учётные данные");
	}
}
