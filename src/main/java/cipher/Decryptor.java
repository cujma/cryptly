package cipher;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import operation.OperationErrorCode;
import operation.OperationException;
import java.util.Objects;
import java.util.function.DoubleConsumer;

public class Decryptor extends FileCipher {
    private static final int HEADER_LENGTH = MAGIC.length + 2 + SALT_LENGTH + NONCE_PREFIX_LENGTH;
    private PayloadType payloadType;

    public Decryptor(Path inputPath, Path outputPath) {
        super(inputPath, outputPath);
    }

    public PayloadType payloadType() {
        if (payloadType == null) {
            throw new IllegalStateException("Encrypted header has not been read.");
        }
        return payloadType;
    }

    public void decrypt(char[] password, SaveMode saveMode) throws IOException, GeneralSecurityException {
        decrypt(password, saveMode, progress -> {});
    }

    public void decrypt(char[] password, SaveMode saveMode, DoubleConsumer progress) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(saveMode, "Save mode cannot be null");
        Objects.requireNonNull(progress, "Progress callback cannot be null");

        Path tempFile = createTempOutputFile(saveMode);
        long totalBytes = Files.size(inputPath);
        long processedBytes;

        byte[] header = new byte[HEADER_LENGTH];
        byte[] salt = null;
        byte[] noncePrefix = null;

        try {
            try (DataInputStream input = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(inputPath)))) {
                try {
                    input.readFully(header);
                } catch (EOFException e) {
                    throw new OperationException(OperationErrorCode.ENCRYPTED_FILE_INCOMPLETE, e);
                }

                validateHeader(header);
                processedBytes = HEADER_LENGTH;
                progress.accept(Math.min(1.0, (double) processedBytes / totalBytes));

                int offset = MAGIC.length + 2;

                salt = Arrays.copyOfRange(header, offset, offset + SALT_LENGTH);
                offset += SALT_LENGTH;

                noncePrefix = Arrays.copyOfRange(header, offset, offset + NONCE_PREFIX_LENGTH);
                SecretKey key = KeyDerivation.deriveKey(password, salt);

                try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                    long counter = 0;

                    while (true) {
                        int length;

                        try {
                            length = input.readInt();
                        } catch (EOFException e) {
                            throw new OperationException(OperationErrorCode.ENCRYPTED_FILE_TRUNCATED, e);
                        }

                        if (length < 0 || length > CHUNK_SIZE) {
                            throw new OperationException(OperationErrorCode.INVALID_ENCRYPTED_BLOCK);
                        }

                        if (counter > 0xFFFF_FFFFL) {
                            throw new OperationException(OperationErrorCode.TOO_MANY_BLOCKS);
                        }

                        if (length == 0) {
                            byte[] finalTag = new byte[TAG_LENGTH_BYTES];

                            try {
                                input.readFully(finalTag);

                                if (decryptChunk(finalTag, key, noncePrefix, header, counter, 0).length != 0) {
                                    throw new OperationException(OperationErrorCode.ENCRYPTED_FILE_END_MARKER_INVALID);
                                }
                            } catch (EOFException e) {
                                throw new OperationException(OperationErrorCode.ENCRYPTED_FILE_INCOMPLETE, e);
                            } finally {
                                Arrays.fill(finalTag, (byte) 0);
                            }

                            if (input.read() != -1) {
                                throw new OperationException(OperationErrorCode.ENCRYPTED_FILE_HAS_TRAILING_DATA);
                            }
                            break;
                        }

                        byte[] encrypted = new byte[length + TAG_LENGTH_BYTES];

                        try {
                            input.readFully(encrypted);

                            byte[] plaintext = decryptChunk(encrypted, key, noncePrefix, header, counter, length);

                            try {
                                if (plaintext.length != length) {
                                    throw new OperationException(OperationErrorCode.INVALID_ENCRYPTED_BLOCK);
                                }

                                output.write(plaintext);
                            } finally {
                                Arrays.fill(plaintext, (byte) 0);
                            }
                        } catch (EOFException e) {
                            throw new OperationException(OperationErrorCode.ENCRYPTED_FILE_BLOCK_INCOMPLETE, e);
                        } finally {
                            Arrays.fill(encrypted, (byte) 0);
                        }
                        processedBytes += Integer.BYTES + encrypted.length;
                        progress.accept(Math.min(1.0, (double) processedBytes / totalBytes));
                        counter++;
                    }
                }
            }

            commitOutput(tempFile, saveMode);
            progress.accept(1.0);

        } finally {
            Files.deleteIfExists(tempFile);
            Arrays.fill(header, (byte) 0);

            if (salt != null) {
                Arrays.fill(salt, (byte) 0);
            }

            if (noncePrefix != null) {
                Arrays.fill(noncePrefix, (byte) 0);
            }
        }
    }

    private void validateHeader(byte[] header) throws IOException {
        for (int i = 0; i < MAGIC.length; i++) {
            if (header[i] != MAGIC[i]) {
                throw new OperationException(OperationErrorCode.INVALID_ENCRYPTED_FILE);
            }
        }

        if (header[MAGIC.length] != VERSION) {
            throw new OperationException(OperationErrorCode.UNSUPPORTED_FORMAT);
        }
        payloadType = PayloadType.fromCode(header[PAYLOAD_TYPE_OFFSET]);
    }

    private byte[] decryptChunk(byte[] encrypted, SecretKey key, byte[] noncePrefix, byte[] header, long counter, int length) throws java.io.IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] nonce = createNonce(noncePrefix, counter);

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            cipher.updateAAD(header);
            cipher.updateAAD(intToBytes((int) counter));
            cipher.updateAAD(intToBytes(length));

            return cipher.doFinal(encrypted);

        } catch (AEADBadTagException e) {
            throw new AEADBadTagException(OperationErrorCode.AUTHENTICATION_FAILED.name());
        } finally {
            Arrays.fill(nonce, (byte) 0);
        }
    }

}
