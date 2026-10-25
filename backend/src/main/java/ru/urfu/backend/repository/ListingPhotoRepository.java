package ru.urfu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.backend.entity.ListingPhoto;

import java.util.List;

public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, Long> {

    List<ListingPhoto> findByListingIdOrderByPosition(Long listingId);

    long countByListingId(Long listingId);
}
