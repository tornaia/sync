package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteEventType;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class RemoteReaderServiceTest {

    @InjectMocks
    private RemoteReaderService remoteReaderService;

    @Test
    public void reAddEvent() {
        FileMetaInfo fileMetaInfo = new FileMetaInfo("id", "userid", "relativePath", 1L, 500L, 600L);
        RemoteFileEvent remoteFileEvent = new RemoteFileEvent(RemoteEventType.MODIFIED, fileMetaInfo);

        remoteReaderService.reAddEvent(remoteFileEvent);

        assertEquals(remoteFileEvent, remoteReaderService.getNextModified().get());
    }

    @Test
    public void addingDeleteEventRemovesUnprocessedModifiedEvent() {
        FileMetaInfo fileMetaInfo = new FileMetaInfo("id", "userid", "relativePath", 1L, 500L, 600L);

        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.MODIFIED, fileMetaInfo));
        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.DELETED, fileMetaInfo));

        assertFalse(remoteReaderService.getNextModified().isPresent());
        assertTrue(remoteReaderService.getNextDeleted().isPresent());
    }

    @Test
    public void addingDeleteEventRemovesUnprocessedModifiedEvents() {
        FileMetaInfo fileMetaInfo = new FileMetaInfo("id", "userid", "relativePath", 1L, 500L, 600L);

        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.MODIFIED, fileMetaInfo));
        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.MODIFIED, fileMetaInfo));
        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.DELETED, fileMetaInfo));

        assertFalse(remoteReaderService.getNextModified().isPresent());
        assertTrue(remoteReaderService.getNextDeleted().isPresent());
    }

    @Test
    public void addingDeleteEventRemovesUnprocessedCreatedEvent() {
        FileMetaInfo fileMetaInfo = new FileMetaInfo("id", "userid", "relativePath", 1L, 500L, 600L);

        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.CREATED, fileMetaInfo));
        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.DELETED, fileMetaInfo));

        assertTrue(remoteReaderService.getNextCreated().createEvents.isEmpty());
        assertTrue(remoteReaderService.getNextDeleted().isPresent());
    }

    @Test
    public void addingDeleteEventRemovesUnprocessedCreatedEvents() {
        FileMetaInfo fileMetaInfo = new FileMetaInfo("id", "userid", "relativePath", 1L, 500L, 600L);

        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.CREATED, fileMetaInfo));
        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.CREATED, fileMetaInfo));
        remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.DELETED, fileMetaInfo));

        assertTrue(remoteReaderService.getNextCreated().createEvents.isEmpty());
        assertTrue(remoteReaderService.getNextDeleted().isPresent());
    }
}
