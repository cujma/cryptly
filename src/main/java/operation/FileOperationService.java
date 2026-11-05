package operation;

import cipher.Decryptor;
import cipher.DirectoryArchive;
import cipher.Encryptor;
import cipher.PayloadType;
import cipher.SaveMode;
import javafx.concurrent.Task;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.DoubleConsumer;

public final class FileOperationService implements FileOperationHandler {
    private enum Operation {
        ENCRYPT,
        DECRYPT
    }

    @Override
    public void encrypt(Path input, Path output, char[] password, SaveMode saveMode, OperationCallbacks callbacks) {
        start(input, output, password, saveMode, Operation.ENCRYPT, callbacks);
    }

    @Override
    public void decrypt(Path input, Path output, char[] password, SaveMode saveMode, OperationCallbacks callbacks) {
        start(input, output, password, saveMode, Operation.DECRYPT, callbacks);
    }

    void executeSynchronously(Path input, Path output, char[] password, SaveMode saveMode, boolean encrypt) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(input, "Input path cannot be null");
        Objects.requireNonNull(output, "Output path cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(saveMode, "Save mode cannot be null");

        char[] passwordChars = password.clone();

        try {
            executeOperation(input, output, passwordChars, saveMode, encrypt ? Operation.ENCRYPT : Operation.DECRYPT, progress -> {});
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    private void start(Path input, Path output, char[] password, SaveMode saveMode, Operation operation, OperationCallbacks callbacks) {
        Objects.requireNonNull(input, "Input path cannot be null");
        Objects.requireNonNull(output, "Output path cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(saveMode, "Save mode cannot be null");
        Objects.requireNonNull(callbacks, "Callbacks cannot be null");

        char[] passwordChars = password.clone();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage(startMessage(operation));

                    try {
                        executeOperation(input, output, passwordChars, saveMode, operation, progress -> updateProgress(progress, 1.0));
                    } catch (OperationException exception) {
                        throw exception;
                    } catch (IOException | GeneralSecurityException exception) {
                        throw new OperationException(
                                operation == Operation.DECRYPT
                                        ? OperationErrorCode.INVALID_ENCRYPTED_FILE
                                        : OperationErrorCode.UNKNOWN_ERROR,
                                exception
                        );
                    }
                    return null;
                } finally {
                    Arrays.fill(passwordChars, '\0');
                }
            }
        };

        task.messageProperty().addListener((observable, oldMessage, newMessage) -> {
            if (newMessage != null && !newMessage.isBlank()) {
                callbacks.onStatus().accept(newMessage);
            }
        });
        task.progressProperty().addListener((observable, oldProgress, newProgress) ->
                callbacks.onProgress().accept(newProgress.doubleValue()));

        task.setOnSucceeded(event -> {
            callbacks.onStatus().accept(completedMessage(operation));
            callbacks.onFinished().run();
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();
            callbacks.onError().accept(errorMessage(error));
            callbacks.onFinished().run();
        });

        Thread thread = new Thread(task, operation == Operation.ENCRYPT ? "file-encryption-task" : "file-decryption-task");
        thread.setDaemon(true);
        thread.start();
    }

    private String startMessage(Operation operation) {
        return operation == Operation.ENCRYPT ? "Encrypting file..." : "Decrypting file...";
    }

    private String completedMessage(Operation operation) {
        return operation == Operation.ENCRYPT ? "Encryption completed successfully." : "Decryption completed successfully.";
    }

    private String errorMessage(Throwable error) {
        if (error instanceof OperationException operationException) {
            return operationException.code().name();
        }
        return OperationErrorCode.UNKNOWN_ERROR.name();
    }

    private void executeOperation(Path input, Path output, char[] password, SaveMode saveMode, Operation operation, DoubleConsumer progress) throws IOException, GeneralSecurityException {
        if (operation == Operation.DECRYPT && !Files.isRegularFile(input)) {
            throw new OperationException(OperationErrorCode.DIRECTORY_CANNOT_BE_DECRYPTED);
        }

        if (operation == Operation.ENCRYPT && Files.isDirectory(input)) {
            Path archive = Files.createTempFile(output.getParent(), ".file-archive-", ".zip");
            try {
                DirectoryArchive.create(input, archive);
                new Encryptor(archive, output, PayloadType.DIRECTORY).encrypt(password, SaveMode.SEPARATE_FILE, progress);
                if (saveMode == SaveMode.REPLACE_ORIGINAL) {
                    deleteDirectory(input);
                }
            } finally {
                Files.deleteIfExists(archive);
            }
            return;
        }

        if (operation == Operation.DECRYPT) {
            Path temporaryDirectory = Files.createTempDirectory(output.getParent(), ".file-archive-");
            Path archive = temporaryDirectory.resolve("content.zip");
            Path restored = Files.createTempDirectory(output.getParent(), ".file-restored-");
            try {
                Decryptor decryptor = new Decryptor(input, archive);
                decryptor.decrypt(password, SaveMode.SEPARATE_FILE, progress);
                if (decryptor.payloadType() == PayloadType.DIRECTORY) {
                    DirectoryArchive.extract(archive, restored);
                    Path directoryOutput = saveMode == SaveMode.SEPARATE_FILE ? output : directoryOutput(input);
                    if (Files.exists(directoryOutput)) {
                        throw new OperationException(OperationErrorCode.OUTPUT_ALREADY_EXISTS);
                    }
                    moveDirectory(restored, directoryOutput);
                    if (saveMode == SaveMode.REPLACE_ORIGINAL) {
                        Files.delete(input);
                    }
                } else {
                    if (Files.exists(output)) {
                        throw new OperationException(OperationErrorCode.OUTPUT_ALREADY_EXISTS);
                    }
                    Files.move(archive, output);
                    if (saveMode == SaveMode.REPLACE_ORIGINAL) {
                        Files.delete(input);
                    }
                }
            } finally {
                Files.deleteIfExists(archive);
                deleteDirectory(temporaryDirectory);
                deleteDirectory(restored);
            }

            return;
        }

        if (operation == Operation.ENCRYPT) {
            new Encryptor(input, output).encrypt(password, saveMode, progress);
        } else {
            new Decryptor(input, output).decrypt(password, saveMode, progress);
        }
    }

    private Path directoryOutput(Path encryptedFile) throws IOException {
        String name = encryptedFile.getFileName().toString();
        if (!name.endsWith(".enc")) {
            throw new OperationException(OperationErrorCode.INVALID_ENCRYPTED_FILE);
        }
        String directoryName = name.substring(0, name.length() - ".enc".length());
        if (directoryName.isBlank()) {
            throw new OperationException(OperationErrorCode.INVALID_ENCRYPTED_FILE);
        }
        return encryptedFile.resolveSibling(directoryName);
    }

    private void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }
}
