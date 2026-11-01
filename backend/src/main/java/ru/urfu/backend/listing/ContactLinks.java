package ru.urfu.backend.listing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

final class ContactLinks {

	private ContactLinks() {
	}

	static String telegram(String raw, String field, Map<String, String> errors) {
		String value = trim(raw);
		if (value == null) {
			return null;
		}
		if (value.startsWith("@")) {
			value = "t.me/" + value.substring(1);
		}
		return url(value, field, "Ссылка вида t.me/username или @username", errors);
	}

	static String whatsapp(String raw, String field, Map<String, String> errors) {
		String value = trim(raw);
		if (value == null) {
			return null;
		}
		if (value.matches("[+\\d\\s()-]+")) {
			value = "wa.me/" + value.replaceAll("\\D", "");
		}
		return url(value, field, "Ссылка вида wa.me/79991234567 или номер телефона", errors);
	}

	static String max(String raw, String field, Map<String, String> errors) {
		return url(trim(raw), field, "Укажите ссылку на профиль MAX", errors);
	}

	private static String url(String value, String field, String hint, Map<String, String> errors) {
		if (value == null) {
			return null;
		}
		String withScheme = value.matches("(?i)^https?://.*") ? value : "https://" + value;
		try {
			URI uri = new URI(withScheme);
			if (uri.getHost() == null || !uri.getHost().contains(".")) {
				errors.put(field, hint);
				return null;
			}
		} catch (URISyntaxException e) {
			errors.put(field, hint);
			return null;
		}
		if (withScheme.length() > 255) {
			errors.put(field, "Слишком длинная ссылка");
			return null;
		}
		return withScheme;
	}

	private static String trim(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
