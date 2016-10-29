package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileCreatedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileDeletedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileModifiedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.single.AbstractLocalFileEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileCreatedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileModifiedEvent;
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

    private LinkedHashSet<AbstractLocalFileEvent> createdEvents = new LinkedHashSet<>();
    private LinkedHashSet<AbstractLocalFileEvent> modifiedEvents = new LinkedHashSet<>();
    private LinkedHashSet<AbstractLocalFileEvent> deletedEvents = new LinkedHashSet<>();

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
        Set<AbstractLocalFileEvent> newOrModifiedChangeList = getNewOrModifiedChangeList(syncDirectory);
        addNewEvents(newOrModifiedChangeList);
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
                LocalFileCreatedEvent localFileEvent = (LocalFileCreatedEvent) createdEvents.iterator().next();
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
                LocalFileModifiedEvent localFileEvent = (LocalFileModifiedEvent) modifiedEvents.iterator().next();
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
                LocalFileDeletedEvent localFileEvent = (LocalFileDeletedEvent) deletedEvents.iterator().next();
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

    private Optional<AbstractLocalFileEvent> getNext(Set<AbstractLocalFileEvent> events) {
        synchronized (this) {
            if (events.isEmpty()) {
                return Optional.empty();
            }
            AbstractLocalFileEvent next = events.iterator().next();
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

    public void reAddEvent(AbstractLocalFileEvent abstractLocalFileEvent) {
        boolean isFileExist = syncDirectory.resolve(abstractLocalFileEvent.relativePath).toFile().exists();
        boolean isDelete = Objects.equals(abstractLocalFileEvent.eventType, LocalFileEventType.DELETED);
        if (isDelete && isFileExist) {
            LOG.warn("Does not make sense to re-add DELETED local event to LocalReaderService when the file is on the disk: " + abstractLocalFileEvent.relativePath);
            return;
        }

        if (isFileExist) {
            LOG.debug("External hint to re-add event: " + abstractLocalFileEvent);
            addNewEvent(abstractLocalFileEvent);
        } else {
            LOG.warn("Cannot re-add event since file does not exist: " + abstractLocalFileEvent);
        }
    }

    private void addNewEvents(Set<AbstractLocalFileEvent> abstractLocalFileEvents) {
        LinkedHashSet<AbstractLocalFileEvent> newCreatedEvents = abstractLocalFileEvents.stream().filter(lfe -> Objects.equals(LocalFileEventType.CREATED, lfe.eventType)).collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<AbstractLocalFileEvent> newModifiedEvents = abstractLocalFileEvents.stream().filter(lfe -> Objects.equals(LocalFileEventType.MODIFIED, lfe.eventType)).collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<AbstractLocalFileEvent> newDeletedEvents = abstractLocalFileEvents.stream().filter(lfe -> Objects.equals(LocalFileEventType.DELETED, lfe.eventType)).collect(Collectors.toCollection(LinkedHashSet::new));
        LOG.debug("Size of possibly new local events to process: c/m/d " + newCreatedEvents.size() + "/" + newModifiedEvents.size() + "/" + newDeletedEvents.size());
        synchronized (this) {
            LOG.debug("Size of pending events before adding possibly new local events to process: c/m/d " + createdEvents.size() + "/" + modifiedEvents.size() + "/" + deletedEvents.size());
            addNewCreatedEventsToTheBeginningOfTheCreatedEventsLinkedHashSet(newCreatedEvents);
            addNewModifiedEventsToTheBeginningOfTheModifiedEventsLinkedHashSet(newModifiedEvents);
            addNewDeletedEventsToTheEndOfTheDeletedEventsLinkedHashSet(newDeletedEvents);
            LOG.debug("Size of pending events after adding possibly new local events to process: c/m/d " + createdEvents.size() + "/" + modifiedEvents.size() + "/" + deletedEvents.size());
        }
    }

    private void addNewCreatedEventsToTheBeginningOfTheCreatedEventsLinkedHashSet(LinkedHashSet<AbstractLocalFileEvent> newCreatedEvents) {
        synchronized (this) {
            newCreatedEvents.addAll(createdEvents);
            createdEvents = newCreatedEvents;
        }
    }

    private void addNewModifiedEventsToTheBeginningOfTheModifiedEventsLinkedHashSet(LinkedHashSet<AbstractLocalFileEvent> newModifiedEvents) {
        synchronized (this) {
            newModifiedEvents.addAll(modifiedEvents);
            modifiedEvents = newModifiedEvents;
        }
    }

    private void addNewDeletedEventsToTheEndOfTheDeletedEventsLinkedHashSet(LinkedHashSet<AbstractLocalFileEvent> newDeletedEvents) {
        synchronized (this) {
            deletedEvents.addAll(newDeletedEvents);
        }
    }

    // TODO ugly and poor design
    private void addNewEvent(AbstractLocalFileEvent abstractLocalFileEvent) {
        // TODO later here we can combine events to optimize things like:
        // create-delete (same path) -> nothing
        // create-delete-create (same path) -> last create only
        switch (abstractLocalFileEvent.eventType) {
            case CREATED:
                addNewCreatedEvent((LocalFileCreatedEvent) abstractLocalFileEvent);
                break;
            case MODIFIED:
                addNewModifiedEvent((LocalFileModifiedEvent) abstractLocalFileEvent);
                break;
            case DELETED:
                addNewDeletedEvent((LocalFileDeletedEvent) abstractLocalFileEvent);
                break;
            default:
                throw new IllegalStateException("Unknown abstractLocalFileEvent: " + abstractLocalFileEvent);
        }
    }

    private void addNewCreatedEvent(LocalFileCreatedEvent localFileCreatedEvent) {
        LinkedHashSet<AbstractLocalFileEvent> set = new LinkedHashSet<>();
        set.add(localFileCreatedEvent);
        addNewCreatedEventsToTheBeginningOfTheCreatedEventsLinkedHashSet(set);
    }

    private void addNewModifiedEvent(LocalFileModifiedEvent fileModifiedEvent) {
        LinkedHashSet<AbstractLocalFileEvent> set = new LinkedHashSet<>();
        set.add(fileModifiedEvent);
        addNewModifiedEventsToTheBeginningOfTheModifiedEventsLinkedHashSet(set);
    }

    private void addNewDeletedEvent(LocalFileDeletedEvent fileDeleteEvent) {
        synchronized (this) {
            deletedEvents.add(fileDeleteEvent);
        }
    }

    private Set<AbstractLocalFileEvent> getNewOrModifiedChangeList(Path root) {
        Set<AbstractLocalFileEvent> newFileEvents = new HashSet<>();
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
                        newFileEvents.add(LocalFileCreatedEvent.ofDirectory(relativePath));
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
                        newFileEvents.add(LocalFileCreatedEvent.ofFile(relativePath));
                        return FileVisitResult.CONTINUE;
                    }

                    FileMetaInfo knownFileMetaInfo = optionalKnownFileMetaInfo.get();
                    FileMetaInfo localFileMetaInfo = new FileMetaInfo(null, userid, relativePath, attrs.size(), attrs.creationTime().toMillis(), attrs.lastModifiedTime().toMillis());
                    if (!Objects.equals(knownFileMetaInfo, localFileMetaInfo)) {
                        LOG.trace("Modified file found: " + knownFileMetaInfo + " -> " + localFileMetaInfo);
                        newFileEvents.add(LocalFileModifiedEvent.ofFile(relativePath));
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
            addNewEvent(LocalFileCreatedEvent.ofFile(relativePath));
        } else if (file.isDirectory()) {
            addNewEvent(LocalFileCreatedEvent.ofDirectory(relativePath));
            LOG.debug("Directory created event: " + fileUtils.getDescriptionForFile(file));
            Set<AbstractLocalFileEvent> newOrModifiedChangeList = getNewOrModifiedChangeList(filePath);
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
            addNewEvent(LocalFileModifiedEvent.ofFile(relativePath));
        } else if (file.isDirectory()) {
            addNewEvent(LocalFileModifiedEvent.ofDirectory(relativePath));
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
            addNewEvent(LocalFileDeletedEvent.ofFile(relativePath));
        } else if (fileMetaInfo.isDirectory()) {
            LOG.trace("Directory deleted event: " + fileUtils.getDescriptionForFile(file));
            addNewEvent(LocalFileDeletedEvent.ofDirectory(relativePath));
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
