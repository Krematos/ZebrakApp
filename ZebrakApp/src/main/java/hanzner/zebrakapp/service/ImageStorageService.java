package hanzner.zebrakapp.service;

import hanzner.zebrakapp.exception.FileStorageException;
import hanzner.zebrakapp.exception.InvalidImageFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    private final Path rootLocation;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    public ImageStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        init();
    }

    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Nelze inicializovat adresář pro ukládání obrázků: " + rootLocation, e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageFileException("Nelze uložit prázdný soubor");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageFileException("Nepodporovaný formát obrázku. Povolené formáty: JPG, PNG, WEBP, GIF");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        } else {
            extension = ".jpg";
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path destinationFile = this.rootLocation.resolve(filename).normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new FileStorageException("Nelze uložit soubor mimo cílový adresář.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new FileStorageException("Chyba při ukládání souboru: " + filename, e);
        }
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        String cleanFilename = StringUtils.cleanPath(filename);
        if (cleanFilename.contains("..") || cleanFilename.contains("/") || cleanFilename.contains("\\")) {
            log.warn("Detekován pokus o path traversal při mazání souboru: '{}'", filename);
            throw new FileStorageException("Neplatný název souboru pro smazání (detekován path traversal): " + filename);
        }

        Path destinationFile = this.rootLocation.resolve(cleanFilename).normalize().toAbsolutePath();
        if (!destinationFile.startsWith(this.rootLocation) || !destinationFile.getParent().equals(this.rootLocation)) {
            log.warn("Pokus o smazání souboru mimo povolený adresář: '{}'", filename);
            throw new FileStorageException("Nelze smazat soubor mimo cílový adresář: " + filename);
        }

        try {
            Files.deleteIfExists(destinationFile);
        } catch (IOException e) {
            log.warn("Nepodařilo se smazat soubor: {}", filename, e);
        }
    }

    public Path load(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new FileStorageException("Název souboru nesmí být prázdný.");
        }

        String cleanFilename = StringUtils.cleanPath(filename);
        if (cleanFilename.contains("..") || cleanFilename.contains("/") || cleanFilename.contains("\\")) {
            log.warn("Detekován pokus o path traversal při načítání souboru: '{}'", filename);
            throw new FileStorageException("Neplatný název souboru (detekován path traversal): " + filename);
        }

        Path destinationFile = this.rootLocation.resolve(cleanFilename).normalize().toAbsolutePath();
        if (!destinationFile.startsWith(this.rootLocation) || !destinationFile.getParent().equals(this.rootLocation)) {
            log.warn("Pokus o přístup k souboru mimo povolený adresář: '{}'", filename);
            throw new FileStorageException("Nelze načíst soubor mimo cílový adresář: " + filename);
        }

        return destinationFile;
    }
}
