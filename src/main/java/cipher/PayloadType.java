package cipher;

import java.io.IOException;

public enum PayloadType {
    FILE((byte) 1),
    DIRECTORY((byte) 2);

    private final byte code;

    PayloadType(byte code) {
        this.code = code;
    }

    static PayloadType fromCode(byte code) throws IOException {
        for (PayloadType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IOException("Unsupported encrypted payload type.");
    }

    byte code() {
        return code;
    }
}
