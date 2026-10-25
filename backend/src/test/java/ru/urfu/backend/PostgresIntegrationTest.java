package ru.urfu.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Основа интеграционных тестов: настоящий PostgreSQL в Docker, миграции Flyway применяются при старте контекста.
 * Контейнер один на весь прогон (его закрывает Ryuk): Spring кэширует контексты между классами, и контейнер,
 * перезапущенный с новым портом, ломал бы закэшированное подключение. Без запущенного Docker тесты пропускаются.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

	static {
		POSTGRES.start();
	}
}
