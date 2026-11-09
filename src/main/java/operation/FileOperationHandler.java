package operation;

import cipher.SaveMode;
import java.nio.file.Path;

public interface FileOperationHandler {

    OperationHandle encrypt(Path input, Path output, char[] password, SaveMode saveMode, OperationCallbacks callbacks);

    OperationHandle decrypt(Path input, Path output, char[] password, SaveMode saveMode, OperationCallbacks callbacks);
}
