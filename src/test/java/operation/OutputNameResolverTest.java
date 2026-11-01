package operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputNameResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void appendsEncryptionExtensionOnlyOnce() {
        assertEquals("notes.txt.enc", OutputNameResolver.forEncryption("notes.txt"));
        assertEquals("notes.txt.enc", OutputNameResolver.forEncryption("notes.txt.enc"));
    }

    @Test
    void removesEncryptionExtensionWhenDecrypting() {
        assertEquals("notes.txt", OutputNameResolver.forDecryption("notes.txt.enc"));
        assertEquals("notes.txt.dec", OutputNameResolver.forDecryption("notes.txt"));
        assertEquals("decrypted-file", OutputNameResolver.forDecryption(".enc"));
    }

    @Test
    void incrementsRestoredDirectoryName() throws IOException {
        Files.createDirectory(tempDir.resolve("archive-restored"));

        assertEquals(
                tempDir.resolve("archive-restored-2"),
                OutputNameResolver.nextRestoredDirectory(tempDir.resolve("archive"))
        );
    }
}
