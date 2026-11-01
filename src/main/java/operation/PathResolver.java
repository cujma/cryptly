package operation;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PathResolver {

    public Path resolveInput(String inputText, boolean encrypt) {
        if (inputText.isBlank()) {
            throw new IllegalArgumentException("Please select an input file.");
        }

        final Path input;
        try {
            input = Path.of(inputText).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("The input file path is invalid.", exception);
        }

        if ((!Files.isRegularFile(input) && !Files.isDirectory(input)) || !Files.isReadable(input)) {
            throw new IllegalArgumentException(
                    "The input file or directory does not exist or cannot be read."
            );
        }
        if (!encrypt && Files.isDirectory(input)) {
            throw new IllegalArgumentException(
                    "To decrypt a directory, select its encrypted .enc file."
            );
        }
        return input;
    }

    public Path resolveOutput(Path input, String outputDirectoryText, boolean encrypt, boolean separate) {
        if (!separate) {
            if (Files.isDirectory(input)) {
                Path output = input.resolveSibling(input.getFileName() + ".enc");
                if (Files.exists(output)) {
                    throw new IllegalArgumentException("The encrypted output already exists.");
                }
                return output;
            }
            return input;
        }

        if (outputDirectoryText.isBlank()) {
            throw new IllegalArgumentException("Please select an output directory.");
        }

        final Path directory;
        try {
            directory = Path.of(outputDirectoryText).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("The output directory path is invalid.", exception);
        }

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("The output directory does not exist.");
        }

        String outputName = encrypt ? OutputNameResolver.forEncryption(input.getFileName().toString()) : OutputNameResolver.forDecryption(input.getFileName().toString());
        Path output = directory.resolve(outputName).normalize();

        if (output.equals(input)) {
            throw new IllegalArgumentException("Input and output files must be different.");
        }
        if (!encrypt && Files.isDirectory(output)) {
            output = OutputNameResolver.nextRestoredDirectory(output);
        }
        if (Files.exists(output)) {
            throw new IllegalArgumentException(
                    "The output file already exists. Choose another directory."
            );
        }
        return output;
    }
}
