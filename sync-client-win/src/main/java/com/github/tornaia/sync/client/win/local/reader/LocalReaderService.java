package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileCreatedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileDeletedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileModifiedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileCreatedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileModifiedEvent;
import com.github.tornaia.sync.client.win.remote.RemoteKnownState;
import com.github.tornaia.sync.client.win.util.FileUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DIRECTORY_POSTFIX;

@Component
public class LocalReaderService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalReaderService.class);

    @Value("${client.sync.directory.path}")
    private String directoryPath;

    @Value("${client.sync.userid}")
    private String userid;

    @Autowired
    private RemoteKnownState remoteKnownState;

    @Autowired
    private FileUtils fileUtils;

    private DirectoryWatchService directoryWatchService;

    private Path syncDirectory;

    private LinkedHashSet<LocalFileCreatedEvent> createdEvents = new LinkedHashSet<>();
    private LinkedHashSet<LocalFileModifiedEvent> modifiedEvents = new LinkedHashSet<>();
    private LinkedHashSet<LocalFileDeletedEvent> deletedEvents = new LinkedHashSet<>();

    @EventListener({ContextRefreshedEvent.class})
    public void onContextRefreshedEvent() throws IOException {
        LOG.info("Context refreshed event happened");
        directoryWatchService.restart();
    }

    @EventListener({ContextClosedEvent.class})
    public void onContextClosedEvent() throws IOException {
        LOG.info("Context closed event happened");
        directoryWatchService.stop();
    }

    @PostConstruct
    public void startDiskWatch() throws IOException {
        this.directoryWatchService = new DirectoryWatchService(directoryPath);
        this.syncDirectory = FileSystems.getDefault().getPath(directoryPath);

        directoryWatchService.addCreatedListener(this::addCreatedEvent);
        directoryWatchService.addModifiedListener(this::addModifiedEvent);
        directoryWatchService.addDeletedListener(this::addDeletedEvent);

        directoryWatchService.start();
    }

    public boolean hasNext() {
        synchronized (this) {
            return !createdEvents.isEmpty() || !modifiedEvents.isEmpty() || !deletedEvents.isEmpty();
        }
    }

    public LocalFileCreatedBulkEvents getNextCreated() {
        LocalFileCreatedBulkEvents localFileCreatedBulkEvents = new LocalFileCreatedBulkEvents(syncDirectory);
        synchronized (this) {
            while (!createdEvents.isEmpty()) {
                LocalFileCreatedEvent localFileEvent = createdEvents.iterator().next();
                boolean added = localFileCreatedBulkEvents.add(localFileEvent);
                if (added) {
                    createdEvents.remove(localFileEvent);
                } else {
                    break;
                }
            }
        }
        return localFileCreatedBulkEvents;
    }

    public LocalFileModifiedBulkEvents getNextModified() {
        LocalFileModifiedBulkEvents localFileModifiedBulkEvents = new LocalFileModifiedBulkEvents(syncDirectory);
        synchronized (this) {
            while (!modifiedEvents.isEmpty()) {
                LocalFileModifiedEvent localFileEvent = modifiedEvents.iterator().next();
                boolean added = localFileModifiedBulkEvents.add(localFileEvent);
                if (added) {
                    modifiedEvents.remove(localFileEvent);
                } else {
                    break;
                }
            }
        }
        return localFileModifiedBulkEvents;
    }

    public LocalFileDeletedBulkEvents getNextDeleted() {
        LocalFileDeletedBulkEvents localFileDeletedBulkEvents = new LocalFileDeletedBulkEvents(syncDirectory);
        synchronized (this) {
            while (!deletedEvents.isEmpty()) {
                LocalFileDeletedEvent localFileEvent = deletedEvents.iterator().next();
                boolean added = localFileDeletedBulkEvents.add(localFileEvent);
                if (added) {
                    deletedEvents.remove(localFileEvent);
                } else {
                    break;
                }
            }
        }
        return localFileDeletedBulkEvents;
    }

    public Optional<FileMetaInfo> getFileMetaInfo(String relativePath) {
        Path absolutePath = getAbsolutePath(relativePath);
        try {
            if (!exists(relativePath)) {
                return Optional.empty();
            }
            File file = absolutePath.toFile();
            FileMetaInfo fileMetaInfo = new FileMetaInfo(null, userid, relativePath, file);
            return Optional.of(fileMetaInfo);
        } catch (IOException e) {
            LOG.warn("Cannot read file from disk! Does not exist or locked?", e);
            return Optional.empty();
        }
    }

    public boolean exists(String relativePath) {
        Path absolutePath = getAbsolutePath(relativePath);
        File file = absolutePath.toFile();
        if (!file.exists()) {
            return false;
        }

        boolean lookingForFile = !relativePath.endsWith(DIRECTORY_POSTFIX);
        return lookingForFile ? file.isFile() : file.isDirectory();
    }

    public void reAddEvent(LocalFileCreatedEvent localFileCreatedEvent) {
        boolean isFileExist = syncDirectory.resolve(localFileCreatedEvent.relativePath).toFile().exists();
        if (isFileExist) {
            LOG.debug("External hint to re-add event: " + localFileCreatedEvent);
            addCreatedEvent(localFileCreatedEvent);
        } else {
            LOG.warn("Cannot re-add event since file does not exist: " + localFileCreatedEvent);
        }
    }

    public void reAddEvent(LocalFileModifiedEvent localFileModifiedEvent) {
        boolean isFileExist = syncDirectory.resolve(localFileModifiedEvent.relativePath).toFile().exists();
        if (isFileExist) {
            LOG.debug("External hint to re-add event: " + localFileModifiedEvent);
            addModifiedEvent(localFileModifiedEvent);
        } else {
            LOG.warn("Cannot re-add event since file does not exist: " + localFileModifiedEvent);
        }
    }

    public void reAddEvent(LocalFileDeletedEvent localFileDeletedEvent) {
        LOG.debug("Wont re-add event: " + localFileDeletedEvent);
    }

    public void addCreatedEvent(LocalFileCreatedEvent localFileCreatedEvent) {
        String relativePath = localFileCreatedEvent.relativePath;
        Path absolutePath = getAbsolutePath(relativePath);
        File file = absolutePath.toFile();

        if (file.isFile()) {
            LOG.debug("File created event: " + fileUtils.getDescriptionForFile(file));
            addNewCreatedEvent(new LocalFileCreatedEvent(relativePath));
        } else if (file.isDirectory()) {
            addNewCreatedEvent(new LocalFileCreatedEvent(relativePath + DIRECTORY_POSTFIX));
            LOG.debug("Directory created event: " + fileUtils.getDescriptionForFile(file));
        } else {
            LOG.debug("Created event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    public void addModifiedEvent(LocalFileModifiedEvent fileModifiedEvent) {
        String relativePath = fileModifiedEvent.relativePath;
        Path absolutePath = getAbsolutePath(relativePath);
        File file = absolutePath.toFile();

        if (file.isFile()) {
            LOG.debug("File modified event: " + fileUtils.getDescriptionForFile(file));
            addNewModifiedEvent(new LocalFileModifiedEvent(relativePath));
        } else if (file.isDirectory()) {
            addNewModifiedEvent(new LocalFileModifiedEvent(relativePath + DIRECTORY_POSTFIX));
            LOG.debug("Directory modified event: " + fileUtils.getDescriptionForFile(file));
            return;
        } else {
            LOG.debug("Modified event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    public void addDeletedEvent(LocalFileDeletedEvent fileDeleteEvent) {
        synchronized (this) {
            String relativePath = fileDeleteEvent.relativePath;
            Path absolutePath = getAbsolutePath(relativePath);
            File file = absolutePath.toFile();

            if (file.exists()) {
                LOG.debug("Deleted event of an existing file is skipped: " + relativePath);
                return;
            }

            Optional<FileMetaInfo> fileMetaInfoAsFile = remoteKnownState.get(relativePath);

            if (!fileMetaInfoAsFile.isPresent()) {
                LOG.debug("Deleted event of a file that is unknown to the server is skipped: " + relativePath);
                return;
            }

            FileMetaInfo fileMetaInfo = fileMetaInfoAsFile.get();
            if (fileMetaInfo.isFile()) {
                LOG.debug("File deleted event: " + fileUtils.getDescriptionForFile(file));
                addNewDeletedEvent(new LocalFileDeletedEvent(relativePath));
            } else if (fileMetaInfo.isDirectory()) {
                LOG.trace("Directory deleted event: " + fileUtils.getDescriptionForFile(file));
                addNewDeletedEvent(new LocalFileDeletedEvent(relativePath + DIRECTORY_POSTFIX));
            }
        }
    }

    private void addNewCreatedEvent(LocalFileCreatedEvent localFileCreatedEvent) {
        synchronized (this) {
            createdEvents.add(localFileCreatedEvent);
        }
    }

    private void addNewModifiedEvent(LocalFileModifiedEvent localFileModifiedEvent) {
        synchronized (this) {
            modifiedEvents.add(localFileModifiedEvent);
        }
    }

    private void addNewDeletedEvent(LocalFileDeletedEvent localFileDeletedEvent) {
        synchronized (this) {
            deletedEvents.add(localFileDeletedEvent);
        }
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
