package com.github.tornaia.sync.client.win.local.writer;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.constant.FileSystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DIRECTORY_POSTFIX;

@Component
public class ConflictFileNameGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ConflictFileNameGenerator.class);

    private static final char EXTENSION_SEPARATOR_CHAR = '.';
    private static final String EXTENSION_SEPARATOR_STRING = ".";

    public Path resolve(Path absolutePath, FileMetaInfo localFileMetaInfo) {
        String originalFileName = absolutePath.toFile().getAbsolutePath();
        boolean isDirectory = originalFileName.endsWith(DIRECTORY_POSTFIX);
        if (isDirectory) {
            originalFileName = originalFileName.substring(0, originalFileName.length() - DIRECTORY_POSTFIX.length());
        }
        boolean hasExtension = originalFileName.indexOf(EXTENSION_SEPARATOR_CHAR) != -1;
        String postFix = "_conflict_" + localFileMetaInfo.size + "_" + localFileMetaInfo.creationDateTime + "_" + localFileMetaInfo.modificationDateTime;
        String conflictFileName = hasExtension ? originalFileName.split("\\.", 2)[0] + postFix + EXTENSION_SEPARATOR_STRING + originalFileName.split("\\.", 2)[1] : originalFileName + postFix;

        // TODO what should happen when this renamed/conflictFileName file exists? or if the generated file name/path is too long?
        Path renamed = new File(absolutePath.toFile().getParentFile().getAbsolutePath()).toPath().resolve(conflictFileName);
        if (isDirectory) {
            renamed = renamed.resolve(FileSystemConstants.DOT_FILENAME);
        }
        LOG.warn("Instead of " + absolutePath + " use " + renamed);
        return renamed;
    }
}
