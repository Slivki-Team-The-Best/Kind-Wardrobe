package ru.urfu.backend.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.backend.common.ApiException;

@Service
public class FileStorageService {

	public static final String URL_PREFIX = "/uploads/";

	private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G'};

	private final Path root;

	public FileStorageService(@Value("${app.storage.dir:uploads}") String dir) throws IOException {
		this.root = Path.of(dir).toAbsolutePath().normalize();
		Files.createDirectories(root);
	}

	public Path getRoot() {
		return root;
	}

	public String storeImage(MultipartFile file, String folder, long maxBytes, String field) {
		if (file == null || file.isEmpty()) {
			throw ApiException.badRequest(field, "Файл пустой");
		}
		if (file.getSize() > maxBytes) {
			throw ApiException.badRequest(field, "Файл больше " + maxBytes / (1024 * 1024) + " МБ");
		}
		String extension = detectExtension(file, field);
		String name = UUID.randomUUID() + extension;
		Path target = root.resolve(folder).resolve(name);
		try (InputStream in = file.getInputStream()) {
			Files.createDirectories(target.getParent());
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить файл");
		}
		String url = URL_PREFIX + folder + "/" + name;
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status != STATUS_COMMITTED) {
						deleteNow(url);
					}
				}
			});
		}
		return url;
	}

	public void deleteAfterCommit(String url) {
		if (url == null) {
			return;
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					deleteNow(url);
				}
			});
		} else {
			deleteNow(url);
		}
	}

	private void deleteNow(String url) {
		if (!url.startsWith(URL_PREFIX)) {
			return;
		}
		Path path = root.resolve(url.substring(URL_PREFIX.length())).normalize();
		if (!path.startsWith(root)) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
		}
	}

	private String detectExtension(MultipartFile file, String field) {
		byte[] head;
		try (InputStream in = file.getInputStream()) {
			head = in.readNBytes(4);
		} catch (IOException e) {
			throw ApiException.badRequest(field, "Не удалось прочитать файл");
		}
		if (startsWith(head, JPEG_SIGNATURE)) {
			return ".jpg";
		}
		if (startsWith(head, PNG_SIGNATURE)) {
			return ".png";
		}
		throw ApiException.badRequest(field, "Можно загружать только JPG или PNG");
	}

	private static boolean startsWith(byte[] data, byte[] prefix) {
		return data.length >= prefix.length && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
	}
}
