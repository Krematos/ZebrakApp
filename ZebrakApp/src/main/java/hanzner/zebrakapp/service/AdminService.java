package hanzner.zebrakapp.service;

import hanzner.zebrakapp.dto.PagedResponse;
import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.Place;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.exception.PlaceNotFoundException;
import hanzner.zebrakapp.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final PlaceRepository placeRepository;
    private final PlaceService placeService;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public PagedResponse<PlaceResponse> getPendingPlaces(Pageable pageable) {
        Pageable effectivePageable = (pageable != null)
                ? pageable
                : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Place> page = placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.PENDING, effectivePageable);
        return PagedResponse.of(page, p -> placeService.mapToPlaceResponse(p, null, null));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PlaceResponse> getAllPlaces(PlaceStatus status, Pageable pageable) {
        Pageable effectivePageable = (pageable != null)
                ? pageable
                : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Place> page = (status != null)
                ? placeRepository.findByStatusOrderByCreatedAtDesc(status, effectivePageable)
                : placeRepository.findAll(effectivePageable);

        return PagedResponse.of(page, p -> placeService.mapToPlaceResponse(p, null, null));
    }

    @Transactional
    public PlaceResponse approvePlace(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new PlaceNotFoundException(id));

        place.setStatus(PlaceStatus.APPROVED);
        place.setRejectionReason(null);
        place = placeRepository.save(place);

        log.info("Místo id={} ('{}') bylo schváleno administrátorem.", id, place.getTitle());
        return placeService.mapToPlaceResponse(place, null, null);
    }

    @Transactional
    public PlaceResponse rejectPlace(Long id, String reason) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new PlaceNotFoundException(id));

        place.setStatus(PlaceStatus.REJECTED);
        place.setRejectionReason(reason);
        place = placeRepository.save(place);

        log.info("Místo id={} ('{}') bylo zamítnuto: {}", id, place.getTitle(), reason);
        return placeService.mapToPlaceResponse(place, null, null);
    }

    @Transactional
    public void deletePlace(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new PlaceNotFoundException(id));

        if (place.getImages() != null) {
            place.getImages().forEach(img -> imageStorageService.delete(img.getFilename()));
        }

        placeRepository.delete(place);
        log.info("Místo id={} ('{}') bylo trvale smazáno administrátorem.", id, place.getTitle());
    }
}
