package com.github.tornaia.sync.client.win.local.writer;

import com.github.tornaia.sync.client.win.util.FileUtils;
import com.github.tornaia.sync.client.win.util.RandomUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Component
public class DiskWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(DiskWriterService.class);

    @Autowired
    private RandomUtils randomUtils;

    @Autowired
    private FileUtils fileUtils;

    public Optional<Path> createTempFile(byte[] fileContent, long creationDateTime, long modificationDateTime) {
        Path tempFile;
        try {
            tempFile = fileUtils.createWorkFile();
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
            fileUtils.setCreationTime(tempFile, creationDateTime);
            fileUtils.setLastModifiedTime(tempFile, modificationDateTime);
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
            if (!absolutePath.toFile().getParentFile().isDirectory()) {
                LOG.error("Cannot create directories to target");
                return false;
            }
            LOG.trace("Directory to target already exist");
        }

        try {
            Files.move(tempFile.get(), absolutePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("Cannot move temporary file to target", e);
            return false;
        }

        try {
            fileUtils.setCreationTime(absolutePath, creationDateTime);
            fileUtils.setLastModifiedTime(absolutePath, modificationDateTime);
        } catch (IOException e) {
            LOG.error("Cannot set file's attributes", e);
            return false;
        }

        LOG.trace("File was written to disk: " + absolutePath.toFile().getAbsolutePath() + ", creationDateTime: " + creationDateTime + ", modificationDateTime: " + modificationDateTime + ", bytes: " + fileContent.length);
        return true;
    }

    public boolean replaceFileAtomically(Path source, Path target) {
        LOG.trace("Replace " + target.toFile().getAbsolutePath() + " with " + source.toFile().getAbsolutePath());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error("Cannot write target file", e);
            return false;
        }
        return true;
    }

    public boolean delete(Path fileAbsolutePath) {
        LOG.trace("Delete " + fileAbsolutePath.toFile().getAbsolutePath());
        try {
            Files.delete(fileAbsolutePath);
        } catch (IOException e) {
            LOG.error("Cannot write target file", e);
            return false;
        }
        return true;
    }
}
