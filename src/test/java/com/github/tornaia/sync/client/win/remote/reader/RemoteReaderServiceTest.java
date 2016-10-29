package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteEventType;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

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

    @Test
    public void relativePathLengthComparatorCompareMethodImplementedCorrectly() {
        for (int i = 0; i < 1000; i++) {
            FileMetaInfo fileMetaInfo = new FileMetaInfo("id", "userid", "relativePath" + i, 1L, 500L, 600L);
            remoteReaderService.reAddEvent(new RemoteFileEvent(RemoteEventType.DELETED, fileMetaInfo));
        }

        for (int i = 0; i < 1000; i++) {
            Optional<RemoteFileEvent> nextDeleted = remoteReaderService.getNextDeleted();
            assertTrue("i: " + i, nextDeleted.isPresent());
        }

        Optional<RemoteFileEvent> nextDeleted = remoteReaderService.getNextDeleted();
        assertFalse(nextDeleted.isPresent());
    }
}
