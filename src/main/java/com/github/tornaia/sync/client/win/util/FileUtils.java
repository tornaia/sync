package com.github.tornaia.sync.client.win.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

@Component
public class FileUtils {

    public String getDescriptionForFile(File file) {
        try {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return file.getAbsolutePath() + ", creationTime: " + attr.creationTime().toMillis() + ", lastModifiedTime: " + attr.lastModifiedTime().toMillis();
        } catch (IOException e) {
            return "Not available: " + e.getMessage();
        }
    }

    public void setCreationTime(Path path, long creationDateTime) throws IOException {
        Files.setAttribute(path, "basic:creationTime", FileTime.fromMillis(creationDateTime));
    }

    public void setLastModifiedTime(Path path, long modificationDateTime) throws IOException {
        Files.setAttribute(path, "basic:lastModifiedTime", FileTime.fromMillis(modificationDateTime));
    }
}
