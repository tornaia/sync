package com.github.tornaia.sync.client.win.local.writer;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

@Component
public class ConflictFileNameGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ConflictFileNameGenerator.class);

    public Path resolve(Path absolutePath, FileMetaInfo localFileMetaInfo) {
        String originalFileName = absolutePath.toFile().getAbsolutePath();
        boolean hasExtension = originalFileName.indexOf('.') != -1;
        String postFix = "_conflict_" + localFileMetaInfo.length + "_" + localFileMetaInfo.creationDateTime + "_" + localFileMetaInfo.modificationDateTime;
        String conflictFileName = hasExtension ? originalFileName.split("\\.", 2)[0] + postFix + "." + originalFileName.split("\\.", 2)[1] : originalFileName + postFix;

        // TODO what should happen when this renamed/conflictFileName file exists? or if the generated file name/path is too log?
        Path renamed = new File(absolutePath.toFile().getParentFile().getAbsolutePath()).toPath().resolve(conflictFileName);
        LOG.warn("File already exists on server. Renaming " + absolutePath + " -> " + renamed);
        return renamed;
    }
}
