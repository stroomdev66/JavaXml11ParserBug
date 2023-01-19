import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    public static Path resolveDir(final String projectDir) {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        Path dir = root.resolve(projectDir);
        if (!Files.isDirectory(dir)) {
            dir = root.getParent().resolve(projectDir);
            if (!Files.isDirectory(dir)) {
                throw new RuntimeException("Path not found: " + dir);
            }
        }

        return dir;
    }
}
