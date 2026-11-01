package ru.urfu.backend.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.backend.common.ApiException;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.ListingStatus;
import ru.urfu.backend.profile.ProfileResponse;
import ru.urfu.backend.repository.ListingRepository;
import ru.urfu.backend.repository.UserRepository;
import ru.urfu.backend.storage.FileStorageService;

@Service
public class UserAccountService {

	public static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

	private final UserRepository users;
	private final ListingRepository listings;
	private final FileStorageService storage;

	public UserAccountService(UserRepository users, ListingRepository listings, FileStorageService storage) {
		this.users = users;
		this.listings = listings;
		this.storage = storage;
	}

	@Transactional(readOnly = true)
	public UserStatsResponse stats(Long userId) {
		return new UserStatsResponse(
				listings.sumSavedKgByAuthor(userId),
				listings.countByAuthorIdAndStatus(userId, ListingStatus.TRANSFERRED),
				listings.countByAuthorIdAndStatus(userId, ListingStatus.ACTIVE),
				listings.countByAuthorId(userId));
	}

	@Transactional
	public ProfileResponse uploadAvatar(Long userId, MultipartFile file) {
		User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
		String previous = user.getAvatarUrl();
		user.setAvatarUrl(storage.storeImage(file, "avatars", MAX_AVATAR_BYTES, "avatar"));
		users.flush();
		storage.deleteAfterCommit(previous);
		return ProfileResponse.from(user);
	}
}
