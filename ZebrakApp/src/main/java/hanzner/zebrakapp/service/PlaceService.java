package hanzner.zebrakapp.service;

import hanzner.zebrakapp.dto.*;
import hanzner.zebrakapp.entity.*;
import hanzner.zebrakapp.exception.*;
import hanzner.zebrakapp.repository.PlaceImageRepository;
import hanzner.zebrakapp.repository.PlaceRepository;
import hanzner.zebrakapp.repository.PlaceVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository imageRepository;
    private final PlaceVerificationRepository verificationRepository;
    private final ImageStorageService imageStorageService;
    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    @Transactional
    public PlaceResponse createPlace(PlaceCreateRequest request, User currentUser) {
        if (currentUser.getRole() != Role.ROLE_ADMIN) {
            rateLimiterService.checkPlaceCreationRateLimit(currentUser.getId());
        }

        PlaceStatus initialStatus = currentUser.getRole() == Role.ROLE_ADMIN 
                ? PlaceStatus.APPROVED 
                : PlaceStatus.PENDING;

        Place place = Place.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .category(request.getCategory())
                .priceLevel(request.getPriceLevel())
                .discountType(request.getDiscountType())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .postalCode(request.getPostalCode() != null ? request.getPostalCode().trim() : "")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingHours(request.getOpeningHours() != null ? request.getOpeningHours().trim() : "")
                .status(initialStatus)
                .votesActive(1) // autor má automaticky 1 aktivní hlas
                .votesClosed(0)
                .user(currentUser)
                .images(new ArrayList<>())
                .verifications(new ArrayList<>())
                .build();

        place = placeRepository.save(place);
        return mapToPlaceResponse(place, currentUser, null);
    }

    @Transactional
    public PlaceResponse updatePlace(Long id, PlaceUpdateRequest request, User currentUser) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new PlaceNotFoundException(id));

        boolean isAuthor = place.getUser() != null && place.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedActionException("Nemáte oprávnění upravovat toto místo.");
        }

        place.setTitle(request.getTitle().trim());
        place.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        place.setCategory(request.getCategory());
        place.setPriceLevel(request.getPriceLevel());
        place.setDiscountType(request.getDiscountType());
        place.setAddress(request.getAddress().trim());
        place.setCity(request.getCity().trim());
        place.setPostalCode(request.getPostalCode() != null ? request.getPostalCode().trim() : "");
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setOpeningHours(request.getOpeningHours() != null ? request.getOpeningHours().trim() : "");

        place = placeRepository.save(place);
        return mapToPlaceResponse(place, currentUser, null);
    }

    @Transactional(readOnly = true)
    public PlaceResponse getPlaceById(Long id, User currentUser, String ipAddress) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new PlaceNotFoundException(id));

        boolean isAuthor = currentUser != null && place.getUser() != null && place.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ROLE_ADMIN;

        if (place.getStatus() != PlaceStatus.APPROVED && !isAuthor && !isAdmin) {
            throw new PlaceNotApprovedException("Místo zatím nebylo schváleno.");
        }

        return mapToPlaceResponse(place, currentUser, ipAddress);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PlaceResponse> searchApprovedPlaces(
            Category category,
            PriceLevel priceLevel,
            DiscountType discountType,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String query,
            User currentUser,
            String ipAddress,
            org.springframework.data.domain.Pageable pageable
    ) {
        String cleanedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        org.springframework.data.jpa.domain.Specification<Place> spec = hanzner.zebrakapp.repository.PlaceSpecification.filterPlaces(
                PlaceStatus.APPROVED,
                category,
                priceLevel,
                discountType,
                minLat,
                maxLat,
                minLng,
                maxLng,
                cleanedQuery
        );

        org.springframework.data.domain.Pageable effectivePageable = (pageable != null)
                ? pageable
                : org.springframework.data.domain.PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        org.springframework.data.domain.Page<Place> placesPage = placeRepository.findAll(spec, effectivePageable);

        return PagedResponse.of(placesPage, p -> mapToPlaceResponse(p, currentUser, ipAddress));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PlaceResponse> getUserPlaces(User currentUser, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Pageable effectivePageable = (pageable != null)
                ? pageable
                : org.springframework.data.domain.PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        org.springframework.data.domain.Page<Place> placesPage = placeRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), effectivePageable);
        return PagedResponse.of(placesPage, p -> mapToPlaceResponse(p, currentUser, null));
    }

    @Transactional
    public List<PlaceImageDto> uploadImages(Long placeId, List<MultipartFile> files, User currentUser) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(placeId));

        boolean isAuthor = place.getUser() != null && place.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedActionException("Nemáte oprávnění nahrávat obrázky k tomuto místu.");
        }

        List<PlaceImage> uploadedImages = new ArrayList<>();
        boolean hasPrimary = place.getImages() != null && place.getImages().stream().anyMatch(PlaceImage::isPrimary);

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String filename = imageStorageService.store(file);
                boolean isFirstImage = !hasPrimary && uploadedImages.isEmpty();

                PlaceImage image = PlaceImage.builder()
                        .place(place)
                        .filename(filename)
                        .originalFilename(file.getOriginalFilename())
                        .mimeType(file.getContentType())
                        .fileSize(file.getSize())
                        .isPrimary(isFirstImage)
                        .build();

                uploadedImages.add(imageRepository.save(image));
            }
        }

        return uploadedImages.stream()
                .map(this::mapToImageDto)
                .toList();
    }

    @Transactional
    public void deleteImage(Long placeId, Long imageId, User currentUser) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(placeId));

        boolean isAuthor = place.getUser() != null && place.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedActionException("Nemáte oprávnění mazat fotografie tohoto místa.");
        }

        PlaceImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));

        if (!image.getPlace().getId().equals(placeId)) {
            throw new ImageDoesNotBelongToPlaceException("Obrázek nepatří k tomuto místu.");
        }

        imageStorageService.delete(image.getFilename());
        imageRepository.delete(image);
    }

    @Transactional
    public VerificationResponse verifyPlace(Long placeId, VoteType vote, User currentUser, String ipAddress) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(placeId));

        Optional<PlaceVerification> existingVote = Optional.empty();
        if (currentUser != null) {
            existingVote = verificationRepository.findByPlaceIdAndUserId(placeId, currentUser.getId());
        } else if (ipAddress != null) {
            existingVote = verificationRepository.findByPlaceIdAndIpAddress(placeId, ipAddress);
        }

        if (existingVote.isPresent()) {
            PlaceVerification v = existingVote.get();
            if (v.getVote() == vote) {
                return VerificationResponse.builder()
                        .placeId(placeId)
                        .votesActive(place.getVotesActive())
                        .votesClosed(place.getVotesClosed())
                        .userVote(vote)
                        .message("Váš hlas již byl dříve započítán.")
                        .build();
            }

            // Změna hlasu
            if (v.getVote() == VoteType.STILL_OPEN) {
                place.setVotesActive(Math.max(0, place.getVotesActive() - 1));
            } else {
                place.setVotesClosed(Math.max(0, place.getVotesClosed() - 1));
            }

            v.setVote(vote);
            verificationRepository.save(v);
        } else {
            PlaceVerification newVerification = PlaceVerification.builder()
                    .place(place)
                    .user(currentUser)
                    .ipAddress(ipAddress)
                    .vote(vote)
                    .build();
            verificationRepository.save(newVerification);
        }

        if (vote == VoteType.STILL_OPEN) {
            place.setVotesActive(place.getVotesActive() + 1);
        } else {
            place.setVotesClosed(place.getVotesClosed() + 1);
        }

        placeRepository.save(place);

        return VerificationResponse.builder()
                .placeId(placeId)
                .votesActive(place.getVotesActive())
                .votesClosed(place.getVotesClosed())
                .userVote(vote)
                .message("Děkujeme! Váš hlas byl úspěšně zaznamenán.")
                .build();
    }

    public PlaceResponse mapToPlaceResponse(Place place, User currentUser, String ipAddress) {
        String userVote = null;
        if (currentUser != null) {
            userVote = verificationRepository.findByPlaceIdAndUserId(place.getId(), currentUser.getId())
                    .map(v -> v.getVote().name())
                    .orElse(null);
        } else if (ipAddress != null) {
            userVote = verificationRepository.findByPlaceIdAndIpAddress(place.getId(), ipAddress)
                    .map(v -> v.getVote().name())
                    .orElse(null);
        }

        List<PlaceImageDto> images = place.getImages() != null
                ? place.getImages().stream().map(this::mapToImageDto).toList()
                : List.of();

        return PlaceResponse.builder()
                .id(place.getId())
                .title(place.getTitle())
                .description(place.getDescription())
                .category(place.getCategory())
                .categoryLabel(place.getCategory().getLabel())
                .priceLevel(place.getPriceLevel())
                .priceLevelLabel(place.getPriceLevel().getLabel())
                .discountType(place.getDiscountType())
                .discountTypeLabel(place.getDiscountType().getLabel())
                .address(place.getAddress())
                .city(place.getCity())
                .postalCode(place.getPostalCode())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .openingHours(place.getOpeningHours())
                .status(place.getStatus())
                .votesActive(place.getVotesActive())
                .votesClosed(place.getVotesClosed())
                .userVote(userVote)
                .rejectionReason(place.getRejectionReason())
                .author(authService.mapToUserDto(place.getUser()))
                .images(images)
                .createdAt(place.getCreatedAt())
                .updatedAt(place.getUpdatedAt())
                .build();
    }

    private PlaceImageDto mapToImageDto(PlaceImage image) {
        return PlaceImageDto.builder()
                .id(image.getId())
                .filename(image.getFilename())
                .url("/uploads/" + image.getFilename())
                .isPrimary(image.isPrimary())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
