package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.remote.RemoteKnownState;
import com.github.tornaia.sync.client.win.util.FileUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.constant.FileSystemConstants;
import com.sun.nio.file.ExtendedWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

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

    private WatchService watchService;

    private Path syncDirectory;

    private LinkedHashSet<LocalFileEvent> createdEvents = new LinkedHashSet<>();
    private LinkedHashSet<LocalFileEvent> modifiedEvents = new LinkedHashSet<>();
    private LinkedHashSet<LocalFileEvent> deletedEvents = new LinkedHashSet<>();

    private volatile boolean contextIsRunning;

    @EventListener({ContextRefreshedEvent.class})
    public void onContextRefreshedEvent() throws IOException {
        LOG.info("Context refreshed event happened");
        contextIsRunning = true;
        startDiskWatch();
    }

    @EventListener({ContextClosedEvent.class})
    public void onContextClosedEvent() throws IOException {
        LOG.info("Context closed event happened");
        contextIsRunning = false;
        watchService.close();
    }

    private void startDiskWatch() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.syncDirectory = FileSystems.getDefault().getPath(directoryPath);
        Files.createDirectories(syncDirectory);

        Thread directoryEventsReaderThread = new Thread(() -> consumeEventsFromWatchService());
        directoryEventsReaderThread.setDaemon(true);
        directoryEventsReaderThread.setName(userid + "-" + directoryPath.substring(directoryPath.length() - 1) + "-LocalR");
        directoryEventsReaderThread.start();

        registerWatcherAndAddAllFiles();
    }

    private void registerWatcherAndAddAllFiles() {
        LOG.info("Register watcherService and scan syncDirectory");
        register(syncDirectory);
        Set<LocalFileEvent> newOrModifiedChangeList = getNewOrModifiedChangeList(syncDirectory);
        addNewEvents(newOrModifiedChangeList);
    }

    public boolean hasNext() {
        synchronized (this) {
            return !createdEvents.isEmpty() || !modifiedEvents.isEmpty() || !deletedEvents.isEmpty();
        }
    }

    public BulkLocalFileCreatedEvent getNextCreated() {
        BulkLocalFileCreatedEvent bulkLocalFileCreatedEvent = new BulkLocalFileCreatedEvent(syncDirectory);
        synchronized (this) {
            while (!createdEvents.isEmpty()) {
                FileCreatedEvent localFileEvent = (FileCreatedEvent) createdEvents.iterator().next();
                boolean added = bulkLocalFileCreatedEvent.add(localFileEvent);
                if (added) {
                    createdEvents.remove(localFileEvent);
                } else {
                    break;
                }
            }
        }
        return bulkLocalFileCreatedEvent;
    }

    public BulkLocalFileModifiedEvent getNextModified() {
        BulkLocalFileModifiedEvent bulkLocalFileModifiedEvent = new BulkLocalFileModifiedEvent(syncDirectory);
        synchronized (this) {
            while (!modifiedEvents.isEmpty()) {
                FileModifiedEvent localFileEvent = (FileModifiedEvent) modifiedEvents.iterator().next();
                boolean added = bulkLocalFileModifiedEvent.add(localFileEvent);
                if (added) {
                    modifiedEvents.remove(localFileEvent);
                } else {
                    break;
                }
            }
        }
        return bulkLocalFileModifiedEvent;
    }

    public Optional<LocalFileEvent> getNextDeleted() {
        return getNext(deletedEvents);
    }

    private Optional<LocalFileEvent> getNext(Set<LocalFileEvent> events) {
        synchronized (this) {
            if (events.isEmpty()) {
                return Optional.empty();
            }
            LocalFileEvent next = events.iterator().next();
            events.remove(next);
            return Optional.of(next);
        }
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

        boolean lookingForFile = !relativePath.endsWith(FileSystemConstants.DIRECTORY_POSTFIX);
        return lookingForFile ? file.isFile() : file.isDirectory();
    }

    public void reAddEvent(LocalFileEvent localFileEvent) {
        boolean isFileExist = syncDirectory.resolve(localFileEvent.relativePath).toFile().exists();
        boolean isDelete = Objects.equals(localFileEvent.eventType, LocalEventType.DELETED);
        if (isDelete && isFileExist) {
            LOG.warn("Does not make sense to re-add DELETED local event to LocalReaderService when the file is on the disk: " + localFileEvent.relativePath);
            return;
        }

        if (isFileExist) {
            LOG.debug("External hint to re-add event: " + localFileEvent);
            addNewEvent(localFileEvent);
        } else {
            LOG.warn("Cannot re-add event since file does not exist: " + localFileEvent);
        }
    }

    private void addNewEvents(Set<LocalFileEvent> localFileEvents) {
        LinkedHashSet<LocalFileEvent> newCreatedEvents = localFileEvents.stream().filter(lfe -> Objects.equals(LocalEventType.CREATED, lfe.eventType)).collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<LocalFileEvent> newModifiedEvents = localFileEvents.stream().filter(lfe -> Objects.equals(LocalEventType.MODIFIED, lfe.eventType)).collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<LocalFileEvent> newDeletedEvents = localFileEvents.stream().filter(lfe -> Objects.equals(LocalEventType.DELETED, lfe.eventType)).collect(Collectors.toCollection(LinkedHashSet::new));
        LOG.debug("Size of possibly new local events to process: c/m/d " + newCreatedEvents.size() + "/" + newModifiedEvents.size() + "/" + newDeletedEvents.size());
        synchronized (this) {
            LOG.debug("Size of pending events before adding possibly new local events to process: c/m/d " + createdEvents.size() + "/" + modifiedEvents.size() + "/" + deletedEvents.size());
            addNewCreatedEventsToTheBeginningOfTheCreatedEventsLinkedHashSet(newCreatedEvents);
            addNewModifiedEventsToTheBeginningOfTheModifiedEventsLinkedHashSet(newModifiedEvents);
            addNewDeletedEventsToTheEndOfTheDeletedEventsLinkedHashSet(newDeletedEvents);
            LOG.debug("Size of pending events after adding possibly new local events to process: c/m/d " + createdEvents.size() + "/" + modifiedEvents.size() + "/" + deletedEvents.size());
        }
    }

    private void addNewCreatedEventsToTheBeginningOfTheCreatedEventsLinkedHashSet(LinkedHashSet<LocalFileEvent> newCreatedEvents) {
        synchronized (this) {
            newCreatedEvents.addAll(createdEvents);
            createdEvents = newCreatedEvents;
        }
    }

    private void addNewModifiedEventsToTheBeginningOfTheModifiedEventsLinkedHashSet(LinkedHashSet<LocalFileEvent> newModifiedEvents) {
        synchronized (this) {
            newModifiedEvents.addAll(modifiedEvents);
            modifiedEvents = newModifiedEvents;
        }
    }

    private void addNewDeletedEventsToTheEndOfTheDeletedEventsLinkedHashSet(LinkedHashSet<LocalFileEvent> newDeletedEvents) {
        synchronized (this) {
            deletedEvents.addAll(newDeletedEvents);
        }
    }

    // TODO ugly and poor design
    private void addNewEvent(LocalFileEvent localFileEvent) {
        // TODO later here we can combine events to optimize things like:
        // create-delete (same path) -> nothing
        // create-delete-create (same path) -> last create only
        switch (localFileEvent.eventType) {
            case CREATED:
                addNewCreatedEvent((FileCreatedEvent) localFileEvent);
                break;
            case MODIFIED:
                addNewModifiedEvent((FileModifiedEvent) localFileEvent);
                break;
            case DELETED:
                addNewDeletedEvent((FileDeleteEvent) localFileEvent);
                break;
            default:
                throw new IllegalStateException("Unknown localFileEvent: " + localFileEvent);
        }
    }

    private void addNewCreatedEvent(FileCreatedEvent fileCreatedEvent) {
        LinkedHashSet<LocalFileEvent> set = new LinkedHashSet<>();
        set.add(fileCreatedEvent);
        addNewCreatedEventsToTheBeginningOfTheCreatedEventsLinkedHashSet(set);
    }

    private void addNewModifiedEvent(FileModifiedEvent fileModifiedEvent) {
        LinkedHashSet<LocalFileEvent> set = new LinkedHashSet<>();
        set.add(fileModifiedEvent);
        addNewModifiedEventsToTheBeginningOfTheModifiedEventsLinkedHashSet(set);
    }

    private void addNewDeletedEvent(FileDeleteEvent fileDeleteEvent) {
        synchronized (this) {
            deletedEvents.add(fileDeleteEvent);
        }
    }

    private Set<LocalFileEvent> getNewOrModifiedChangeList(Path root) {
        Set<LocalFileEvent> newFileEvents = new HashSet<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(syncDirectory)) {
                        LOG.trace("Skipping syncDirectory: " + dir);
                        return FileVisitResult.CONTINUE;
                    }
                    String relativePath = getRelativePath(dir.toFile());
                    LOG.trace("Visit directory: " + relativePath);
                    Optional<FileMetaInfo> optionalKnownFileMetaInfo = remoteKnownState.get(relativePath);
                    if (!optionalKnownFileMetaInfo.isPresent()) {
                        LOG.trace("New directory found: " + relativePath);
                        newFileEvents.add(FileCreatedEvent.ofDirectory(relativePath));
                        return FileVisitResult.CONTINUE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = getRelativePath(file.toFile());
                    LOG.trace("Visit file: " + relativePath);
                    Optional<FileMetaInfo> optionalKnownFileMetaInfo = remoteKnownState.get(getRelativePath(file.toFile()));
                    if (!optionalKnownFileMetaInfo.isPresent()) {
                        LOG.trace("New file found: " + relativePath);
                        newFileEvents.add(FileCreatedEvent.ofFile(relativePath));
                        return FileVisitResult.CONTINUE;
                    }

                    FileMetaInfo knownFileMetaInfo = optionalKnownFileMetaInfo.get();
                    FileMetaInfo localFileMetaInfo = new FileMetaInfo(null, userid, relativePath, attrs.size(), attrs.creationTime().toMillis(), attrs.lastModifiedTime().toMillis());
                    if (!Objects.equals(knownFileMetaInfo, localFileMetaInfo)) {
                        LOG.trace("Modified file found: " + knownFileMetaInfo + " -> " + localFileMetaInfo);
                        newFileEvents.add(FileModifiedEvent.ofFile(relativePath));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (FileSystemException e) {
            LOG.warn("Cannot scan filesystem: " + e.getMessage());
            // TODO recursive so stack overflow might occur if the fileSystem is blocked for long
            registerWatcherAndAddAllFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newFileEvents;
    }

    private void consumeEventsFromWatchService() {
        while (contextIsRunning) {
            WatchKey key;
            try {
                key = watchService.poll(25, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ClosedWatchServiceException e) {
                LOG.warn("Run terminated: " + e.getMessage());
                return;
            }
            if (key == null) {
                Thread.yield();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();

                if (kind == OVERFLOW) {
                    LOG.warn("Overflow! Restarting LocalReaderService");
                    try {
                        onContextClosedEvent();
                        onContextRefreshedEvent();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                Path ownerPath = (Path) key.watchable();
                Path filePath = ownerPath.resolve(filename);
                if (kind == ENTRY_CREATE) {
                    onFileCreated(filePath);
                } else if (kind == ENTRY_MODIFY) {
                    onFileModified(filePath);
                } else if (kind == ENTRY_DELETE) {
                    onFileDeleted(filePath);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

            Thread.yield();
        }
    }

    private void onFileCreated(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);

        if (file.isFile()) {
            LOG.debug("File created event: " + fileUtils.getDescriptionForFile(file));
            addNewEvent(FileCreatedEvent.ofFile(relativePath));
        } else if (file.isDirectory()) {
            addNewEvent(FileCreatedEvent.ofDirectory(relativePath));
            LOG.debug("Directory created event: " + fileUtils.getDescriptionForFile(file));
            Set<LocalFileEvent> newOrModifiedChangeList = getNewOrModifiedChangeList(filePath);
            addNewEvents(newOrModifiedChangeList);
        } else {
            LOG.debug("Created event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileModified(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);
        if (file.isFile()) {
            LOG.debug("File modified event: " + fileUtils.getDescriptionForFile(file));
            addNewEvent(FileModifiedEvent.ofFile(relativePath));
        } else if (file.isDirectory()) {
            addNewEvent(FileModifiedEvent.ofDirectory(relativePath));
            LOG.debug("Directory modified event: " + fileUtils.getDescriptionForFile(file));
            return;
        } else {
            LOG.debug("Modified event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileDeleted(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);

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
            addNewEvent(FileDeleteEvent.ofFile(relativePath));
        } else if (fileMetaInfo.isDirectory()) {
            LOG.trace("Directory deleted event: " + fileUtils.getDescriptionForFile(file));
            addNewEvent(FileDeleteEvent.ofDirectory(relativePath));
        }
    }

    private void register(Path syncDirectory) {
        try {
            syncDirectory.register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW}, ExtendedWatchEventModifier.FILE_TREE);
        } catch (IOException e) {
            LOG.warn("Cannot register watcher: " + e);
        }
    }

    private String getRelativePath(File file) {
        return syncDirectory.relativize(file.toPath()).toString();
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
