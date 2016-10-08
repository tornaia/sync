package com.github.tornaia.sync.client.win.local.writer;

import com.github.tornaia.sync.client.win.util.RandomUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

@Component
public class DiskWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(DiskWriterService.class);

    @Autowired
    private RandomUtils randomUtils;

    public Optional<Path> createTempFile(byte[] fileContent, long creationDateTime, long modificationDateTime) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile(randomUtils.getRandomString(), "suffix");
        } catch (IOException e) {
            LOG.error("Cannot create temporary file", e);
            return Optional.empty();
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            IOUtils.write(fileContent, fos);
        } catch (IOException e) {
            LOG.error("Cannot write temporary file", e);
            return Optional.empty();
        }

        try {
            java.nio.file.Files.setAttribute(tempFile, "basic:creationTime", FileTime.fromMillis(creationDateTime));
            java.nio.file.Files.setAttribute(tempFile, "basic:lastModifiedTime", FileTime.fromMillis(modificationDateTime));
        } catch (IOException e) {
            LOG.error("Cannot set temporary file's attributes", e);
            return Optional.empty();
        }

        return Optional.of(tempFile);
    }

    public boolean writeFileAtomically(Path absolutePath, byte[] fileContent, long creationDateTime, long modificationDateTime) {
        Optional<Path> tempFile = createTempFile(fileContent, creationDateTime, modificationDateTime);

        if (!tempFile.isPresent()) {
            LOG.error("Cannot create temporary file");
            return false;
        }

        if (!absolutePath.toFile().getParentFile().mkdirs()) {
            LOG.error("Cannot create directories to target");
            return false;
        }

        try {
            Files.move(tempFile.get(), absolutePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("Cannot move temporary file to target", e);
            return false;
        }

        return true;
    }

    public boolean replaceFile(Path what, Path with) {
        Path oldContentNewFilePath = new File(what.toFile().getAbsolutePath() + "_conflict_" + System.currentTimeMillis()).toPath();
        try {
            Files.move(what, oldContentNewFilePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("Cannot create backup", e);
            return false;
        }

        try {
            Files.move(with, what, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("Cannot write new file", e);
            return false;
        }
        return true;
    }
}
