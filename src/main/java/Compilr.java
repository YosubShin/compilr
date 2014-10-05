import lombok.Data;

import java.io.*;

/**
 * Created by Daniel on 10/4/14.
 */
@Data
public class Compilr {
    private static final String COMPILER_CMD = "g++";

    private String userName;
    private String projectName;
    private String sourceFileName;
    private String inputImageName;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage : java -jar compilr.jar <user_name> <project_name> <source_file_name> <input_image_name> [--path=<workspace_path>]");
            System.exit(-1);
        }
        String userName = args[0];
        String projectName = args[1];
        String sourceFileName = args[2];
        String inputImageName = args[3];

        if (args.length == 5) {
            String workspacePath = args[4].substring("--path=".length());
            Util.basePath = workspacePath;
        }

//        String userName = "user1";
//        String projectName = "project1";
//        String sourceFileName = "hackmit-test";
//        String inputImageName = "image.jpg";

        Compilr compilr = new Compilr();
        compilr.setUserName(userName);
        compilr.setProjectName(projectName);
        compilr.setSourceFileName(sourceFileName);
        compilr.setInputImageName(inputImageName);

        compilr.run();
    }

    public void run() {
        CompilrRequest request = new CompilrRequest(userName, projectName, sourceFileName, inputImageName);

        Result ocrResult = runTesseractOCR(request);
        Result ocrPostProcessResult = postProcessRawSourceFile(request, ocrResult.getOutputFile());
        Result compileResult = compileSourceCode(request, ocrPostProcessResult.getOutputFile());
        if (compileResult.getReturnCode() != 0) {
            // TODO Handle compile error and output error file
        }
        Result executeResult = executeSourceCode(request, compileResult.getOutputFile());
        if (compileResult.getReturnCode() != 0) {
            // TODO Handle runtime error and output stdout / error file
        }
    }

    private static Result runTesseractOCR(CompilrRequest request) {
        Result result = new Result();
        Process ocrProcess = null;
        try {
            File inputImageFile = new File(Util.getBaseDirectory(request), request.getInputImageName());
            File rawSourceCodeFile = new File(Util.getBaseDirectory(request), "raw_" + request.getSourceFileName() + ".txt");
            result.setOutputFile(rawSourceCodeFile);
            String command = String.format("tesseract %s %s source-code",
                    inputImageFile.getAbsolutePath(),
                    rawSourceCodeFile.getAbsolutePath().substring(0, rawSourceCodeFile.getAbsolutePath().indexOf(".txt")));
            ocrProcess = Runtime.getRuntime().exec(command);
            result.setReturnCode(ocrProcess.waitFor());
            BufferedReader stdoutReader =
                    new BufferedReader(new InputStreamReader(ocrProcess.getInputStream()));
            BufferedReader stderrReader =
                    new BufferedReader(new InputStreamReader(ocrProcess.getErrorStream()));

            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = stdoutReader.readLine())!= null) {
                sb.append(line).append("\n");
            }
            result.setStdout(sb.toString());

            sb = new StringBuilder();
            while ((line = stderrReader.readLine())!= null) {
                sb.append(line).append("\n");
            }
            result.setStderr(sb.toString());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Unable to run OCR", e);
        }
        return result;
    }

    private static Result postProcessRawSourceFile(CompilrRequest request, File fileToProcess) {
        Result result = new Result();
        String sourceCode = Util.readFromFile(fileToProcess);
        // Process Source code
        sourceCode = sourceCode.replaceAll("”", "\"");

        // Save source code
        File processedFile = new File(Util.getBaseDirectory(request), request.getSourceFileName() + ".cpp");
        Util.writeToFile(processedFile, sourceCode);

        result.setOutputFile(processedFile);
        result.setReturnCode(0);

        return result;
    }

    private static Result compileSourceCode(CompilrRequest request, File sourceFile) {
        Result result = new Result();

        try {
            File executableFile = new File(Util.getBaseDirectory(request), request.getSourceFileName());
            String command = String.format("%s %s -o %s",
                    COMPILER_CMD,
                    sourceFile.getAbsolutePath(),
                    executableFile);
            Process compileProcess = Runtime.getRuntime().exec(command);
            result.setReturnCode(compileProcess.waitFor());
            result.setOutputFile(executableFile);

            BufferedReader stdoutReader =
                    new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
            BufferedReader stderrReader =
                    new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));

            String line = "";
            StringBuilder sb = new StringBuilder();
            while ((line = stdoutReader.readLine())!= null) {
                sb.append(line + "\n");
            }
            result.setStdout(sb.toString());

            sb = new StringBuilder();
            while ((line = stderrReader.readLine()) != null) {
                sb.append(line + "\n");
            }
            result.setStderr(sb.toString());

            System.out.println("Compiler stdout :");
            System.out.println(result.getStdout());
            System.out.println("Compiler stderr :");
            System.out.println(result.getStderr());

        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("Failed to compile", e);
        }

        return result;
    }

    private static Result executeSourceCode(CompilrRequest request, File executableFile) {
        Result result = new Result();

        try {
            Process executeProcess = Runtime.getRuntime().exec(executableFile.getAbsolutePath());
            Thread.sleep(100);

            int retryCount = 10;
            int returnValue = -1;
            while (retryCount > 0) {
                try {
                    returnValue = executeProcess.exitValue();
                    // If reach here, process ended
                    break;
                } catch(IllegalThreadStateException e) {
                    retryCount--;
                    Thread.sleep(500);
                }
            }
            result.setReturnCode(returnValue);

            BufferedReader stdoutReader =
                    new BufferedReader(new InputStreamReader(executeProcess.getInputStream()));
            BufferedReader stderrReader =
                    new BufferedReader(new InputStreamReader(executeProcess.getErrorStream()));

            String line = "";
            StringBuilder sb = new StringBuilder();
            while ((line = stdoutReader.readLine())!= null) {
                sb.append(line + "\n");
            }
            result.setStdout(sb.toString());

            sb = new StringBuilder();
            while ((line = stderrReader.readLine()) != null) {
                sb.append(line + "\n");
            }
            if (returnValue == -1) {
                executeProcess.destroy();
                sb.append("Process is terminated due to timeout.");
            }

            result.setStderr(sb.toString());

            if (result.getStdout() != null) {
                File outputFile = new File(Util.getBaseDirectory(request), request.getSourceFileName() + ".out");
                Util.writeToFile(outputFile, result.getStdout());
            }

            System.out.println("Executable stdout :");
            System.out.println(result.getStdout());
            System.out.println("Executable stderr :");
            System.out.println(result.getStderr());

        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute", e);
        }
        return result;
    }

    private static void updateProgress(CompilrRequest request, String progress) {
        File progressFile = new File(Util.getBaseDirectory(request), "progress.txt");
        Util.writeToFile(progressFile, progress);
    }

}
