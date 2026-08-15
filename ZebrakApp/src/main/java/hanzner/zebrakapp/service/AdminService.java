package hanzner.zebrakapp.service;

import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.Place;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.exception.PlaceNotFoundException;
import hanzner.zebrakapp.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final PlaceRepository placeRepository;
    private final PlaceService placeService;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public List<PlaceResponse> getPendingPlaces() {
        return placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.PENDING)
                .stream()
                .map(p -> placeService.mapToPlaceResponse(p, null, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse> getAllPlaces(PlaceStatus status) {
        List<Place> places = status != null 
                ? placeRepository.findByStatusOrderByCreatedAtDesc(status) 
                : placeRepository.findAll();

        return places.stream()
                .map(p -> placeService.mapToPlaceResponse(p, null, null))
                .toList();
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
