package operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PathResolverTest {
    private final PathResolver resolver = new PathResolver();

    @TempDir
    Path tempDir;

    @Test
    void resolvesExistingFileForEncryption() throws IOException {
        Path input = Files.createFile(tempDir.resolve("notes.txt"));

        assertEquals(input.toAbsolutePath(), resolver.resolveInput(input.toString(), true));
    }

    @Test
    void rejectsDirectoryForDecryption() throws IOException {
        Path input = Files.createDirectory(tempDir.resolve("archive"));

        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveInput(input.toString(), false));
    }

    @Test
    void rejectsEncryptingAlreadyEncryptedFile() throws IOException {
        Path input = Files.createFile(tempDir.resolve("notes.txt.enc"));
        Files.write(input, new byte[]{'F', 'E', 'N', 'C', 'S', 'T', 'R', 'M'});

        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveInput(input.toString(), true));
    }

    @Test
    void allowsRegularFileWithEncExtension() throws IOException {
        Path input = tempDir.resolve("notes.txt.enc");
        Files.writeString(input, "regular file");

        assertEquals(input.toAbsolutePath(), resolver.resolveInput(input.toString(), true));
    }

    @Test
    void createsEncryptedNameForSeparateOutput() throws IOException {
        Path input = Files.createFile(tempDir.resolve("notes.txt"));

        Path output = resolver.resolveOutput(input, tempDir.toString(), true, true);

        assertEquals(tempDir.resolve("notes.txt.enc"), output);
    }

    @Test
    void rejectsExistingSeparateOutput() throws IOException {
        Path input = Files.createFile(tempDir.resolve("notes.txt"));
        Files.createFile(tempDir.resolve("notes.txt.enc"));

        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveOutput(input, tempDir.toString(), true, true));
    }

    @Test
    void choosesNextRestoredDirectoryWhenNameIsTaken() throws IOException {
        Path input = Files.createFile(tempDir.resolve("archive.enc"));
        Files.createDirectory(tempDir.resolve("archive"));
        Files.createDirectory(tempDir.resolve("archive-restored"));

        Path output = resolver.resolveOutput(input, tempDir.toString(), false, true);

        assertEquals(tempDir.resolve("archive-restored-2"), output);
    }

    @Test
    void usesEncryptedFileAsReplacementOutputForFileEncryption() throws IOException {
        Path input = Files.createFile(tempDir.resolve("notes.txt"));

        assertEquals(input, resolver.resolveOutput(input, "", true, false));
    }

    @Test
    void removesEncryptedExtensionWhenReplacingWithDecryptedFile() throws IOException {
        Path input = Files.createFile(tempDir.resolve("notes.txt.enc"));

        assertEquals(
                tempDir.resolve("notes.txt"),
                resolver.resolveOutput(input, "", false, false)
        );
    }
}
