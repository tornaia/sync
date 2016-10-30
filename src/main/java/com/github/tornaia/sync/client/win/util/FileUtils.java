package com.github.tornaia.sync.client.win.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;

@Component
public class FileUtils {

    @Autowired
    private RandomUtils randomUtils;

    public String getDescriptionForFile(File file) {
        try {
            Path pathAsFile = file.toPath();
            boolean pathAsFileExist = pathAsFile.toFile().exists();
            if (pathAsFileExist) {
                return getDescription(pathAsFile);
            }

            Path pathAsDirectory = file.toPath().resolve(DOT_FILENAME);
            boolean pathAsDirectoryExist = pathAsDirectory.toFile().exists();
            if (pathAsDirectoryExist) {
                return getDescription(pathAsDirectory);
            }
            return "File does not exist: " + file.getAbsolutePath();
        } catch (IOException e) {
            return "Exception throw during file read: " + e.getMessage();
        }
    }

    private String getDescription(Path path) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return path.toFile().getAbsolutePath() + ", creationTime: " + attr.creationTime().toMillis() + ", lastModifiedTime: " + attr.lastModifiedTime().toMillis();
    }

    public void setCreationTime(Path path, long creationDateTime) throws IOException {
        Files.setAttribute(path, "basic:creationTime", FileTime.fromMillis(creationDateTime));
    }

    public void setLastModifiedTime(Path path, long modificationDateTime) throws IOException {
        if (path.toFile().isDirectory()) {
            throw new IllegalArgumentException("Never set lastModifiedTime of a directory: " + path);
        }
        Files.setAttribute(path, "basic:lastModifiedTime", FileTime.fromMillis(modificationDateTime));
    }

    public Path createWorkFile() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return Files.createFile(new File(tmpDir).toPath().resolve(randomUtils.getRandomString()));
    }

    public Path createWorkDirectory() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return Files.createDirectory(new File(tmpDir).toPath().resolve(randomUtils.getRandomString()));
    }
}
