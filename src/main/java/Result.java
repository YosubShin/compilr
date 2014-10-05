import lombok.Data;

import java.io.File;

/**
 * Created by Daniel on 10/4/14.
 */
@Data
public class Result {
    private int returnCode;
    private String stdout;
    private String stderr;
    private File outputFile;
}
