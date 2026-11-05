package operation;

import cipher.FileCipher;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PathResolver {

    public Path resolveInput(String inputText, boolean encrypt) throws java.io.IOException {
        if (inputText.isBlank()) {
            throw OperationException.validation(OperationErrorCode.INPUT_REQUIRED);
        }

        final Path input;
        try {
            input = Path.of(inputText).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw OperationException.validation(OperationErrorCode.INVALID_INPUT_PATH);
        }

        if ((!Files.isRegularFile(input) && !Files.isDirectory(input)) || !Files.isReadable(input)) {
            throw OperationException.validation(OperationErrorCode.INPUT_NOT_ACCESSIBLE);
        }
        if (encrypt && Files.isRegularFile(input)) {
            try {
                if (FileCipher.hasEncryptedMagic(input)) {
                    throw OperationException.validation(OperationErrorCode.ALREADY_ENCRYPTED);
                }
            } catch (java.io.IOException exception) {
                throw OperationException.validation(OperationErrorCode.INPUT_NOT_ACCESSIBLE);
            }
        }
        if (!encrypt && Files.isDirectory(input)) {
            throw OperationException.validation(OperationErrorCode.DIRECTORY_CANNOT_BE_DECRYPTED);
        }
        return input;
    }

    public Path resolveOutput(Path input, String outputDirectoryText, boolean encrypt, boolean separate) throws java.io.IOException {
        if (!separate) {
            if (Files.isDirectory(input)) {
                Path output = input.resolveSibling(input.getFileName() + ".enc");
                if (Files.exists(output)) {
                    throw OperationException.validation(OperationErrorCode.ENCRYPTED_OUTPUT_EXISTS);
                }
                return output;
            }
            if (!encrypt) {
                Path output = input.resolveSibling(
                        OutputNameResolver.forDecryption(input.getFileName().toString())
                );
                if (Files.exists(output)) {
                    throw OperationException.validation(OperationErrorCode.DECRYPTED_OUTPUT_EXISTS);
                }
                return output;
            }
            return input;
        }

        if (outputDirectoryText.isBlank()) {
            throw OperationException.validation(OperationErrorCode.OUTPUT_DIRECTORY_REQUIRED);
        }

        final Path directory;
        try {
            directory = Path.of(outputDirectoryText).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw OperationException.validation(OperationErrorCode.INVALID_OUTPUT_DIRECTORY);
        }

        if (!Files.isDirectory(directory)) {
            throw OperationException.validation(OperationErrorCode.OUTPUT_DIRECTORY_NOT_FOUND);
        }

        String outputName = encrypt ? OutputNameResolver.forEncryption(input.getFileName().toString()) : OutputNameResolver.forDecryption(input.getFileName().toString());
        Path output = directory.resolve(outputName).normalize();

        if (output.equals(input)) {
            throw OperationException.validation(OperationErrorCode.INPUT_OUTPUT_MUST_DIFFER);
        }
        if (!encrypt && Files.isDirectory(output)) {
            output = OutputNameResolver.nextRestoredDirectory(output);
        }
        if (Files.exists(output)) {
            throw OperationException.validation(OperationErrorCode.OUTPUT_ALREADY_EXISTS);
        }
        return output;
    }
}
