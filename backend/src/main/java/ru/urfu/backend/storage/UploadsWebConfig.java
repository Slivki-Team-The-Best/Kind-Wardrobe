package ru.urfu.backend.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadsWebConfig implements WebMvcConfigurer {

	private final FileStorageService storage;

	public UploadsWebConfig(FileStorageService storage) {
		this.storage = storage;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String location = storage.getRoot().toUri().toString();
		registry.addResourceHandler(FileStorageService.URL_PREFIX + "**")
				.addResourceLocations(location.endsWith("/") ? location : location + "/");
	}
}
