package cipher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import operation.OperationErrorCode;
import operation.OperationException;

public abstract class FileCipher {
    protected static final String ALGORITHM = "AES/GCM/NoPadding";
    protected static final int TAG_LENGTH_BITS = 128;
    protected static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;
    protected static final int CHUNK_SIZE = 64 * 1024;
    protected static final int SALT_LENGTH = 16;
    protected static final int NONCE_PREFIX_LENGTH = 8;
    protected static final int NONCE_LENGTH = 12;
    protected static final byte[] MAGIC = {'F', 'E', 'N', 'C', 'S', 'T', 'R', 'M'};
    protected static final byte VERSION = 2;
    protected static final int PAYLOAD_TYPE_OFFSET = MAGIC.length + 1;
    protected final Path inputPath;
    protected final Path outputPath;

    protected FileCipher(Path inputPath, Path outputPath) {
        this.inputPath = Objects.requireNonNull(inputPath, "Input path cannot be null").toAbsolutePath().normalize();
        this.outputPath = Objects.requireNonNull(outputPath, "Output path cannot be null").toAbsolutePath().normalize();

    }

    public static boolean hasEncryptedMagic(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return Arrays.equals(input.readNBytes(MAGIC.length), MAGIC);
        }
    }

    protected void validateInput() throws IOException {
        if (!Files.exists(inputPath)) {
            throw new OperationException(OperationErrorCode.INPUT_FILE_MISSING);
        }

        if (!Files.isRegularFile(inputPath)) {
            throw new OperationException(OperationErrorCode.INPUT_NOT_REGULAR_FILE);
        }

        if (!Files.isReadable(inputPath)) {
            throw new OperationException(OperationErrorCode.INPUT_NOT_ACCESSIBLE);
        }
    }

    protected Path createTempOutputFile(SaveMode saveMode) throws IOException {
        validateInput();
        Objects.requireNonNull(saveMode, "Save mode cannot be null");

        Path parent = outputPath.getParent();

        if (parent == null || !Files.isDirectory(parent)) {
            throw new OperationException(OperationErrorCode.OUTPUT_DIRECTORY_MISSING);
        }

        if (saveMode == SaveMode.SEPARATE_FILE && Files.exists(outputPath)) {
            throw new OperationException(OperationErrorCode.OUTPUT_ALREADY_EXISTS);
        }

        return Files.createTempFile(parent, ".file-cipher-", ".tmp");
    }

    protected void commitOutput(Path tempFile, SaveMode saveMode) throws IOException {
        Objects.requireNonNull(tempFile, "Temporary file cannot be null");
        Objects.requireNonNull(saveMode, "Save mode cannot be null");

        Path normalizedTemp = tempFile.toAbsolutePath().normalize();

        if (!Files.isRegularFile(normalizedTemp)) {
            throw new OperationException(OperationErrorCode.TEMPORARY_OUTPUT_MISSING);
        }

        Path destination = saveMode == SaveMode.REPLACE_ORIGINAL ? inputPath : outputPath;

        if (saveMode == SaveMode.SEPARATE_FILE && Files.exists(destination)) {
            throw new OperationException(OperationErrorCode.OUTPUT_ALREADY_EXISTS);
        }

        try {
            Files.move(normalizedTemp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(normalizedTemp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    protected byte[] createNonce(byte[] prefix, long counter) {
        byte[] nonce = new byte[NONCE_LENGTH];
        System.arraycopy(prefix, 0, nonce, 0, prefix.length);

        int value = (int) counter;
        nonce[8] = (byte) (value >>> 24);
        nonce[9] = (byte) (value >>> 16);
        nonce[10] = (byte) (value >>> 8);
        nonce[11] = (byte) value;
        return nonce;
    }

    protected byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }
}
