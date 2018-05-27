import Shell.Constants;
import Shell.FileType;
import Shell.GDirectory;
import Shell.GFile;

import javax.naming.CompositeName;
import java.nio.file.InvalidPathException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class Driver {
    private static final Map<String, String> manPages = new HashMap<>();
    private static GDirectory root;
    private static GDirectory currDir;
    private static boolean quit = false;

    public static void main(String[] args) {
        setUpManual();
        Scanner sc = new Scanner(System.in);    // Intellij does auto imports
        currDir = root = new GDirectory("/", null);

        System.out.println(Constants.welcomeText);
        // Process incoming user commands
        while (!quit) {
            System.out.print("\n>");
            // reads input line, stores the split array on space in line
            try {
                String[] line = sc.nextLine().split(" ");
                String ouput = runCommand(line);
                System.out.println(ouput);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String runCommand(String... line) throws Exception { //what does ... mean? String[] Is there a difference? yes
        // can pass in an array as runCommand("1", "2", "3") oh ok got it; also forces it to be the last param in a mthod
        String output = "";

        // considers the first word -> lower case for switch
        switch (line[0].toLowerCase()) {
            case Constants.MAN:     // line = ["manPages", "<content>"]      process it
                assertEQ(2, line.length, "Expected: man <command>");
                output = manPages.getOrDefault(line[1], "Invalid Command");
                break;
            case Constants.TOUCH:       // line = ["touch", "<fileName>"]      process it
                assertEQ(2, line.length, "Expected: touch <fileName>");
                if (currDir.contains(line[1]))
                    throw new Exception("Naming Conflict");

                // TODO: support file extensions
                GFile gFile = new GFile(line[1], FileType.TXT);
                currDir.add(gFile);
                break;
            case Constants.RM:      // line = ["rm", "<fileName>"]      process it
                assertEQ(2, line.length, "Expected: rm <fileName>");
                if (!currDir.containsFile(line[1]))
                    throw new Exception("File Not Found");

                currDir.remove(line[1]);
                break;
            case Constants.MKDIR:
                assertEQ(2, line.length, "Expected: mkdir <directoryName>");
                if (currDir.contains(line[1]))
                    throw new Exception(Constants.missingFileDir);

                currDir.add(new GDirectory(line[1], currDir));
                break;
            case Constants.RMDIR:
                assertEQ(2, line.length, "Expected: rmdir <directoryName>");
                if (!currDir.containsDirectory(line[1]) || !((GDirectory) currDir.get(line[1])).isEmpty())
                    throw new Exception("Directory not found");

                currDir.remove(line[1]);
                break;
            case Constants.LS: case Constants.LL:   // skip ll for now
                output = currDir.toString();
                break;
            case Constants.ECHO:
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < line.length; i++)
                    sb.append(line[i]).append(" ");

                sb.setLength(sb.length() -1);
                output = sb.toString();
                break;
            case Constants.CD:
                assertEQ(2, line.length, "Expected: cd <directoryName>");
                processCD(line[1]);
                break;
            case Constants.PWD:
                GDirectory temp = currDir;
                StringBuilder presentWorkingDirectory = new StringBuilder();
                do {
                    presentWorkingDirectory.insert(0, temp.getName());
                    if (currDir != root)
                        presentWorkingDirectory.insert(0, "/");
                } while ((temp = temp.getParent()) != root);

                output = presentWorkingDirectory.toString();
                break;
            case Constants.WRITE:
                if (currDir.containsDirectory(line[1]))
                    throw new Exception(line[1] + " is actually a directory");
                if (!currDir.containsFile(line[1]))
                    throw new Exception("File doesn't exist");

                StringBuilder content = new StringBuilder();
                for (int wordIndex = 2; wordIndex < line.length; wordIndex++)
                    content.append(line[wordIndex]).append(" ");

                ((GFile) currDir.get(line[1])).append(content.toString());
                break;
            case Constants.CAT:
                assertEQ(2, line.length, "Expected: cat <fileName");
                if (currDir.containsDirectory(line[1]))
                    throw new Exception(line[1] + " is actually a directory");
                if (!currDir.containsFile(line[1]))
                    throw new Exception("File doesn't exist");

                output = ((GFile) currDir.get(line[1])).getContent();
                break;
            case "quit":
                quit = true;
                break;
            default:
                output = "Unrecognized Command";
        }
        return output;
    }

    // all these strings need to move over to Constants
    private static void setUpManual() {
        manPages.put(Constants.MAN, Constants.MAN_DESC);
        manPages.put(Constants.TOUCH, Constants.TOUCH_DESC);
        manPages.put(Constants.RM, Constants.RM_DESC);
        manPages.put(Constants.MKDIR, Constants.MKDIR_DESC);
        manPages.put(Constants.RMDIR, Constants.RMDIR_DESC);
        manPages.put(Constants.LS, Constants.LS_DESC);
        manPages.put(Constants.LL, Constants.LL_DESC);
        manPages.put(Constants.ECHO, Constants.ECHO_DESC);
        manPages.put(Constants.CD, Constants.CD_DESC);
        manPages.put(Constants.PWD, Constants.PWD_DESC);
        manPages.put(Constants.WRITE, Constants.WRITE_DESC);
        manPages.put(Constants.CAT, Constants.CAT_DESC);
        manPages.put(Constants.QUIT, Constants.QUIT_DESC);
    }

    private static void assertEQ(int expected, int actual, String errorMsg) throws Exception{
        if (expected != actual)
            throw new Exception(errorMsg);
    }

    private static void assertBase(boolean exists, String path, boolean isFile, String errorMsg) throws Exception {
        if (exists != (isFile ? currDir.containsFile(path) : currDir.containsDirectory(path))) //I lost you here
            throw new Exception(errorMsg);
    }

    private static void processCD(String path) throws InvalidPathException {
        switch (path) {
            case ("/"):
                currDir = root;
                break;
            case ".":   // empty
                break;
            case (".."):
                currDir = currDir.getParent();
                break;
            default:
                throw new InvalidPathException(path, "Directory Not Found");
        }
    }
}
