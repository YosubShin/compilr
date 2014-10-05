import java.io.*;
import java.nio.file.Path;

/**
 * Created by Daniel on 10/4/14.
 */
public class Util {

    public static String basePath = "./";

    public static File getBaseDirectory(CompilrRequest request) {
        File basePathFile = new File(basePath);
        return new File(basePathFile, request.getProjectName());
    }

    public static void writeToFile(File file, String content) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to file");
        }
    }

    public static String readFromFile(File file) {
        FileReader reader = null;
        BufferedReader br = null;
        try {
            String line = "";
            reader = new FileReader(file);
            br = new BufferedReader(reader);

            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();

            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                br.close();
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
