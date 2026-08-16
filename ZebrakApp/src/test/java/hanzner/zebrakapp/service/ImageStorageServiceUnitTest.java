package hanzner.zebrakapp.service;

import hanzner.zebrakapp.exception.FileStorageException;
import hanzner.zebrakapp.exception.InvalidImageFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageStorageService Unit Testy")
class ImageStorageServiceUnitTest {

    @TempDir
    Path tempDir;

    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        imageStorageService = new ImageStorageService(tempDir.toString());
    }

    @Nested
    @DisplayName("Testy pro store()")
    class StoreTests {

        @Test
        @DisplayName("Úspěšně uloží JPEG soubor a vrátí platný vygenerovaný název")
        void testStore_ValidJpeg_Success() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "fotka.jpg",
                    "image/jpeg",
                    "fake-image-bytes".getBytes()
            );

            String savedFilename = imageStorageService.store(file);

            assertNotNull(savedFilename);
            assertTrue(savedFilename.endsWith(".jpg"));
            assertTrue(Files.exists(tempDir.resolve(savedFilename)));
        }

        @Test
        @DisplayName("Úspěšně uloží PNG, WEBP a GIF formáty")
        void testStore_OtherValidFormats_Success() {
            MockMultipartFile pngFile = new MockMultipartFile("file", "image.png", "image/png", "png-content".getBytes());
            MockMultipartFile webpFile = new MockMultipartFile("file", "image.webp", "image/webp", "webp-content".getBytes());
            MockMultipartFile gifFile = new MockMultipartFile("file", "image.gif", "image/gif", "gif-content".getBytes());

            String pngSaved = imageStorageService.store(pngFile);
            String webpSaved = imageStorageService.store(webpFile);
            String gifSaved = imageStorageService.store(gifFile);

            assertTrue(pngSaved.endsWith(".png"));
            assertTrue(webpSaved.endsWith(".webp"));
            assertTrue(gifSaved.endsWith(".gif"));
        }

        @Test
        @DisplayName("Výchozí přípona .jpg při absenci tečky v původním názvu")
        void testStore_FilenameWithoutExtension_DefaultsToJpg() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "fotka_bez_pripony",
                    "image/jpeg",
                    "data".getBytes()
            );

            String savedFilename = imageStorageService.store(file);
            assertTrue(savedFilename.endsWith(".jpg"));
        }

        @Test
        @DisplayName("Vyhodí InvalidImageFileException při prázdném souboru")
        void testStore_EmptyFile_ThrowsException() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.jpg",
                    "image/jpeg",
                    new byte[0]
            );

            assertThrows(InvalidImageFileException.class, () -> imageStorageService.store(emptyFile));
        }

        @Test
        @DisplayName("Vyhodí InvalidImageFileException při nepodporovaném MIME typu (např. text/plain, application/pdf)")
        void testStore_UnsupportedMimeType_ThrowsException() {
            MockMultipartFile textFile = new MockMultipartFile("file", "text.txt", "text/plain", "data".getBytes());
            MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());
            MockMultipartFile nullMime = new MockMultipartFile("file", "doc.jpg", null, "data".getBytes());

            assertThrows(InvalidImageFileException.class, () -> imageStorageService.store(textFile));
            assertThrows(InvalidImageFileException.class, () -> imageStorageService.store(pdfFile));
            assertThrows(InvalidImageFileException.class, () -> imageStorageService.store(nullMime));
        }
    }

    @Nested
    @DisplayName("Testy pro delete()")
    class DeleteTests {

        @Test
        @DisplayName("Úspěšně smaže existující soubor")
        void testDelete_ExistingFile_Success() throws IOException {
            Path testFile = tempDir.resolve("to_delete.jpg");
            Files.writeString(testFile, "test-data");
            assertTrue(Files.exists(testFile));

            imageStorageService.delete("to_delete.jpg");

            assertFalse(Files.exists(testFile));
        }

        @Test
        @DisplayName("Ignoruje null nebo prázdný název souboru bez chyby")
        void testDelete_NullOrBlank_DoesNothing() {
            assertDoesNotThrow(() -> imageStorageService.delete(null));
            assertDoesNotThrow(() -> imageStorageService.delete(""));
            assertDoesNotThrow(() -> imageStorageService.delete("   "));
        }

        @Test
        @DisplayName("Vyhodí FileStorageException při pokusu o path traversal (obsahuje '..', '/' nebo '\\')")
        void testDelete_PathTraversal_ThrowsException() {
            assertThrows(FileStorageException.class, () -> imageStorageService.delete("../secret.txt"));
            assertThrows(FileStorageException.class, () -> imageStorageService.delete("subdir/file.jpg"));
            assertThrows(FileStorageException.class, () -> imageStorageService.delete("subdir\\file.jpg"));
        }
    }

    @Nested
    @DisplayName("Testy pro load()")
    class LoadTests {

        @Test
        @DisplayName("Úspěšně vrátí cestu k souboru v cílovém adresáři")
        void testLoad_ValidFilename_ReturnsPath() {
            Path loadedPath = imageStorageService.load("test.jpg");

            assertNotNull(loadedPath);
            assertEquals(tempDir.resolve("test.jpg").toAbsolutePath().normalize(), loadedPath);
        }

        @Test
        @DisplayName("Vyhodí FileStorageException při null nebo prázdném názvu")
        void testLoad_NullOrBlank_ThrowsException() {
            assertThrows(FileStorageException.class, () -> imageStorageService.load(null));
            assertThrows(FileStorageException.class, () -> imageStorageService.load(""));
            assertThrows(FileStorageException.class, () -> imageStorageService.load("   "));
        }

        @Test
        @DisplayName("Vyhodí FileStorageException při pokusu o path traversal")
        void testLoad_PathTraversal_ThrowsException() {
            assertThrows(FileStorageException.class, () -> imageStorageService.load("../config.json"));
            assertThrows(FileStorageException.class, () -> imageStorageService.load("sub/test.jpg"));
            assertThrows(FileStorageException.class, () -> imageStorageService.load("sub\\test.jpg"));
        }
    }
}
