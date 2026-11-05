package gui;

import operation.OperationErrorCode;
import operation.OperationException;

public final class ErrorMessageProvider {
    public String messageFor(String errorCode) {
        try {
            return messageFor(OperationErrorCode.valueOf(errorCode));
        } catch (IllegalArgumentException exception) {
            return messageFor(OperationErrorCode.UNKNOWN_ERROR);
        }
    }

    public String messageFor(Throwable error) {
        if (error instanceof IllegalArgumentException
                && error.getCause() instanceof OperationException operationException) {
            return messageFor(operationException.code());
        }
        if (error instanceof javax.crypto.AEADBadTagException) {
            return messageFor(OperationErrorCode.AUTHENTICATION_FAILED);
        }
        if (error instanceof OperationException operationException) {
            return messageFor(operationException.code());
        }
        return "The operation could not be completed.";
    }

    public String messageFor(OperationErrorCode code) {
        return switch (code) {
            case INPUT_REQUIRED -> "Please select an input file.";
            case INVALID_INPUT_PATH -> "The input file path is invalid.";
            case INPUT_NOT_ACCESSIBLE -> "The input file or directory does not exist or cannot be read.";
            case ALREADY_ENCRYPTED -> "The selected file is already encrypted.";
            case DIRECTORY_CANNOT_BE_DECRYPTED -> "To decrypt a directory, select its encrypted .enc file.";
            case ENCRYPTED_OUTPUT_EXISTS -> "The encrypted output already exists.";
            case DECRYPTED_OUTPUT_EXISTS -> "The decrypted output already exists.";
            case OUTPUT_DIRECTORY_REQUIRED -> "Please select an output directory.";
            case INVALID_OUTPUT_DIRECTORY -> "The output directory path is invalid.";
            case OUTPUT_DIRECTORY_NOT_FOUND -> "The output directory does not exist.";
            case INPUT_OUTPUT_MUST_DIFFER -> "Input and output files must be different.";
            case OUTPUT_ALREADY_EXISTS -> "The output file already exists.";
            case PASSWORD_REQUIRED -> "Please enter a password.";
            case PASSWORDS_DO_NOT_MATCH -> "Passwords do not match.";
            case INVALID_ENCRYPTED_FILE -> "The encrypted file is invalid.";
            case INVALID_ARCHIVE -> "The encrypted archive is invalid.";
            case INPUT_FILE_MISSING -> "The input file does not exist.";
            case INPUT_NOT_REGULAR_FILE -> "The input path is not a regular file.";
            case OUTPUT_DIRECTORY_MISSING -> "The output directory does not exist.";
            case TEMPORARY_OUTPUT_MISSING -> "The temporary output file does not exist.";
            case INVALID_PASSWORD -> "The password is invalid.";
            case AUTHENTICATION_FAILED -> "Authentication failed. The password may be incorrect.";
            case INVALID_SALT -> "The encrypted file salt is invalid.";
            case INVALID_ENCRYPTED_BLOCK -> "The encrypted file contains an invalid block.";
            case ENCRYPTED_FILE_INCOMPLETE -> "The encrypted file is incomplete.";
            case ENCRYPTED_FILE_TRUNCATED -> "The encrypted file is truncated.";
            case ENCRYPTED_FILE_END_MARKER_INVALID -> "The encrypted file end marker is invalid.";
            case ENCRYPTED_FILE_HAS_TRAILING_DATA -> "The encrypted file contains unexpected trailing data.";
            case ENCRYPTED_FILE_BLOCK_INCOMPLETE -> "The encrypted file contains an incomplete block.";
            case TOO_MANY_BLOCKS -> "The encrypted file contains too many blocks.";
            case UNSUPPORTED_PAYLOAD -> "The encrypted payload type is not supported.";
            case DIRECTORY_NOT_ACCESSIBLE -> "The input directory is not accessible.";
            case UNSUPPORTED_DIRECTORY_CONTENT -> "Symbolic links and special files are not supported.";
            case ARCHIVE_DUPLICATE_ENTRY -> "The archive contains duplicate entries.";
            case ARCHIVE_CONFLICTING_ENTRY -> "The archive contains conflicting entries.";
            case ARCHIVE_INVALID_PATH -> "The archive contains an invalid path.";
            case ARCHIVE_UNSAFE_PATH -> "The archive contains an unsafe path.";
            case UNSUPPORTED_FORMAT -> "The encrypted file format is not supported.";
            case UNKNOWN_ERROR -> "The operation could not be completed.";
        };
    }
}
