package operation;

import java.util.Objects;

public class OperationException extends java.io.IOException {
    private final OperationErrorCode code;

    public OperationException(OperationErrorCode code) {
        this(code, null);
    }

    public OperationException(OperationErrorCode code, Throwable cause) {
        super(cause);
        this.code = Objects.requireNonNull(code, "Error code cannot be null");
    }

    public OperationErrorCode code() {
        return code;
    }

    public static IllegalArgumentException validation(OperationErrorCode code) {
        return new IllegalArgumentException(new OperationException(code));
    }
}
