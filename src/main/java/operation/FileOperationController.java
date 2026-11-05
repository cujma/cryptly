package operation;

import cipher.SaveMode;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

public final class FileOperationController {
    private final FileOperationHandler handler;
    private final PathResolver pathResolver;

    public FileOperationController(FileOperationHandler handler) {
        this(handler, new PathResolver());
    }

    FileOperationController(FileOperationHandler handler, PathResolver pathResolver) {
        this.handler = Objects.requireNonNull(handler, "Handler cannot be null");
        this.pathResolver = Objects.requireNonNull(pathResolver, "Path resolver cannot be null");
    }

    public PreparedOperation prepare(String inputText, String outputDirectoryText, boolean encrypt, boolean separate,
                                     char[] password, char[] confirmation, Predicate<Path> confirmReplacement)
            throws java.io.IOException {
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(confirmation, "Confirmation cannot be null");
        Objects.requireNonNull(confirmReplacement, "Confirmation callback cannot be null");

        if (password.length == 0) {
            throw OperationException.validation(OperationErrorCode.PASSWORD_REQUIRED);
        }
        if (encrypt && !Arrays.equals(password, confirmation)) {
            throw OperationException.validation(OperationErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        Path input = pathResolver.resolveInput(inputText, encrypt);
        Path output = pathResolver.resolveOutput(input, outputDirectoryText, encrypt, separate);

        if (encrypt && !separate && !confirmReplacement.test(input)) {
            return null;
        }

        SaveMode saveMode = separate ? SaveMode.SEPARATE_FILE : SaveMode.REPLACE_ORIGINAL;
        return new PreparedOperation(input, output, encrypt, saveMode);
    }

    public void start(PreparedOperation operation, char[] password, OperationCallbacks callbacks) {
        Objects.requireNonNull(operation, "Operation cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(callbacks, "Callbacks cannot be null");

        if (operation.encrypt()) {
            handler.encrypt(operation.input(), operation.output(), password, operation.saveMode(), callbacks);
        } else {
            handler.decrypt(operation.input(), operation.output(), password, operation.saveMode(), callbacks);
        }
    }

    public record PreparedOperation(Path input, Path output, boolean encrypt, SaveMode saveMode) {
    }

    public boolean isDirectory(String inputText) {
        try {
            return java.nio.file.Files.isDirectory(Path.of(inputText));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
