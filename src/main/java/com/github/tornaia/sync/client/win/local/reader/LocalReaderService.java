package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.remote.RemoteKnownState;
import com.github.tornaia.sync.client.win.util.FileUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
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
    private String syncDirectoryPath;

    @Value("${frosch-sync.userid}")
    private String userid;

    @Autowired
    private RemoteKnownState remoteKnownState;

    @Autowired
    private FileUtils fileUtils;

    private WatchService watchService;

    private Path syncDirectory;

    private Set<LocalFileEvent> createdEvents = new LinkedHashSet<>();
    private Set<LocalFileEvent> modifiedEvents = new LinkedHashSet<>();
    private Set<LocalFileEvent> deletedEvents = new LinkedHashSet<>();

    private volatile boolean contextIsRunning;

    @EventListener({ContextRefreshedEvent.class})
    public void ContextRefreshedEvent() throws IOException {
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

    public void startDiskWatch() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.syncDirectory = FileSystems.getDefault().getPath(syncDirectoryPath);
        Files.createDirectories(syncDirectory);

        Thread directoryEventsReaderThread = new Thread(() -> consumeEventsFromWatchService());
        directoryEventsReaderThread.setDaemon(true);
        directoryEventsReaderThread.setName(userid + "-" + syncDirectoryPath.substring(syncDirectoryPath.length() - 1) + "-LocalR");
        directoryEventsReaderThread.start();

        registerWatcherAndAddAllFiles();
    }

    private void registerWatcherAndAddAllFiles() {
        LOG.debug("Rescan directory tree");
        register(syncDirectory);
        Set<LocalFileEvent> newOrModifiedChangeList = getNewOrModifiedChangeList(syncDirectory);
        addNewEvents(newOrModifiedChangeList);
    }

    public Optional<LocalFileEvent> getNextCreated() {
        return getNext(createdEvents);
    }

    public Optional<LocalFileEvent> getNextModified() {
        return getNext(modifiedEvents);
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
        return absolutePath.toFile().exists();
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
        Set<LocalFileEvent> newCreatedEvents = localFileEvents.stream().filter(lfe -> Objects.equals(LocalEventType.CREATED, lfe.eventType)).collect(Collectors.toSet());
        Set<LocalFileEvent> newModifiedEvents = localFileEvents.stream().filter(lfe -> Objects.equals(LocalEventType.MODIFIED, lfe.eventType)).collect(Collectors.toSet());
        Set<LocalFileEvent> newDeletedEvents = localFileEvents.stream().filter(lfe -> Objects.equals(LocalEventType.DELETED, lfe.eventType)).collect(Collectors.toSet());
        LOG.info("Size of possibly new local events to process: c/m/d " + newCreatedEvents.size() + ", " + newModifiedEvents.size() + ", " + newDeletedEvents.size());
        synchronized (this) {
            LOG.info("Size of pending events before adding possibly new local events to process: c/m/d " + createdEvents.size() + ", " + modifiedEvents.size() + ", " + deletedEvents.size());
            createdEvents.addAll(newCreatedEvents);
            modifiedEvents.addAll(newModifiedEvents);
            deletedEvents.addAll(newDeletedEvents);
            LOG.info("Size of pending events after adding possibly new local events to process: c/m/d " + createdEvents.size() + ", " + modifiedEvents.size() + ", " + deletedEvents.size());
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
        synchronized (this) {
            createdEvents.add(fileCreatedEvent);
        }
    }

    private void addNewModifiedEvent(FileModifiedEvent fileModifiedEvent) {
        synchronized (this) {
            modifiedEvents.add(fileModifiedEvent);
        }
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
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = getRelativePath(file.toFile());
                    LOG.trace("Visit file: " + relativePath);
                    Optional<FileMetaInfo> optionalKnownFileMetaInfo = remoteKnownState.get(getRelativePath(file.toFile()));
                    if (!optionalKnownFileMetaInfo.isPresent()) {
                        LOG.trace("New file found: " + relativePath);
                        newFileEvents.add(new FileCreatedEvent(relativePath));
                        return FileVisitResult.CONTINUE;
                    }

                    FileMetaInfo knownFileMetaInfo = optionalKnownFileMetaInfo.get();
                    FileMetaInfo localFileMetaInfo = new FileMetaInfo(null, userid, relativePath, attrs.size(), attrs.creationTime().toMillis(), attrs.lastModifiedTime().toMillis());
                    if (!Objects.equals(knownFileMetaInfo, localFileMetaInfo)) {
                        LOG.trace("Modified file found: " + knownFileMetaInfo + " -> " + localFileMetaInfo);
                        newFileEvents.add(new FileModifiedEvent(relativePath));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
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
                    LOG.error("Overflow is an unhandled event type: " + event);
                    continue;
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
            addNewEvent(new FileCreatedEvent(relativePath));
        } else if (file.isDirectory()) {
            LOG.debug("Directory created event: " + fileUtils.getDescriptionForFile(file));
        } else {
            LOG.debug("Created event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileModified(Path filePath) {
        File file = filePath.toFile();
        if (file.isDirectory()) {
            LOG.trace("Directory modified event: " + fileUtils.getDescriptionForFile(file));
            return;
        } else if (file.isFile()) {
            String relativePath = getRelativePath(file);
            LOG.debug("File modified event: " + fileUtils.getDescriptionForFile(file));
            addNewEvent(new FileModifiedEvent(relativePath));
        } else {
            LOG.debug("Modified event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileDeleted(Path filePath) {
        File file = filePath.toFile();
        if (file.exists()) {
            LOG.debug("Deleted event of a non-existing file: " + file);
            return;
        }

        String relativePath = getRelativePath(file);
        LOG.debug("File deleted event: " + relativePath);
        addNewEvent(new FileDeleteEvent(relativePath));
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
