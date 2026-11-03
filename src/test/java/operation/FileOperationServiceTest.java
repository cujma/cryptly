package operation;

import cipher.SaveMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOperationServiceTest {
    private static final char[] PASSWORD = "test-password".toCharArray();

    @TempDir
    Path tempDir;

    @Test
    void encryptsAndDecryptsDirectoryThroughService() throws Exception {
        Path source = Files.createDirectory(tempDir.resolve("documents"));
        Files.createDirectories(source.resolve("nested"));
        Files.writeString(source.resolve("readme.txt"), "hello");
        Files.writeString(source.resolve("nested/note.txt"), "private");

        Path encrypted = tempDir.resolve("documents.enc");
        FileOperationService service = new FileOperationService();
        service.executeSynchronously(source, encrypted, PASSWORD, SaveMode.SEPARATE_FILE, true);

        Path restored = tempDir.resolve("restored-documents");
        service.executeSynchronously(encrypted, restored, PASSWORD, SaveMode.SEPARATE_FILE, false);

        assertEquals("hello", Files.readString(restored.resolve("readme.txt")));
        assertEquals("private", Files.readString(restored.resolve("nested/note.txt")));
    }

    @Test
    void replacingDirectoryRemovesSourceOnlyAfterSuccessfulEncryption() throws Exception {
        Path source = Files.createDirectory(tempDir.resolve("documents"));
        Files.writeString(source.resolve("readme.txt"), "hello");
        Path encrypted = tempDir.resolve("documents.enc");

        new FileOperationService().executeSynchronously(
                source, encrypted, PASSWORD, SaveMode.REPLACE_ORIGINAL, true
        );

        assertFalse(Files.exists(source));
        assertTrue(Files.exists(encrypted));
    }

    @Test
    void decryptingWithReplacementRemovesEncExtension() throws Exception {
        Path source = Files.createFile(tempDir.resolve("notes.txt"));
        Files.writeString(source, "hello");
        Path encrypted = tempDir.resolve("notes.txt.enc");

        FileOperationService service = new FileOperationService();
        service.executeSynchronously(source, encrypted, PASSWORD, SaveMode.SEPARATE_FILE, true);
        Files.delete(source);
        service.executeSynchronously(
                encrypted,
                tempDir.resolve("notes.txt"),
                PASSWORD,
                SaveMode.REPLACE_ORIGINAL,
                false
        );

        assertFalse(Files.exists(encrypted));
        assertEquals("hello", Files.readString(source));
    }
}
