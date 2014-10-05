import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Daniel on 10/4/14.
 */
@Data
@AllArgsConstructor
public class CompilrRequest {
    private String userName;
    private String projectName;
    private String sourceFileName;
    private String inputImageName;
}
