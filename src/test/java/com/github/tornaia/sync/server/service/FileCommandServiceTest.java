package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.websocket.SyncWebSocketHandler;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import com.github.tornaia.sync.shared.api.matchers.RemoteFileEventMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.tornaia.sync.shared.api.RemoteEventType.CREATED;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileCommandServiceTest {

    @Rule
    public ExpectedException expectedException = none();

    @Mock
    private FileRepository fileRepository;

    @Mock
    private SyncWebSocketHandler syncWebSocketHandler;

    @InjectMocks
    private FileCommandService commandService;

    @Before
    public void setUp() {
        when(fileRepository.findByPath(anyString())).thenReturn(null);
        when(fileRepository.insert(Mockito.any(File.class))).thenReturn(new File("userid", "path", "test_content".getBytes(), 2L, 2L));
    }

    @Test
    public void createIfFileExists() throws Exception {
        when(fileRepository.findByPath(anyString())).thenReturn(new File());

        expectedException.expect(FileAlreadyExistsException.class);
        commandService.createFile("userid", 1L, 2L, "path", "Test".getBytes());
    }

    @Test
    public void createFile() throws Exception {
        commandService.createFile("userid", 2L, 2L, "path", "test_content".getBytes());

        verify(syncWebSocketHandler).notifyClients(argThat(new RemoteFileEventMatcher()
                .eventType(CREATED)
                .fileMetaInfo(new FileMetaInfoMatcher()
                        .userid("userid")
                        .relativePath("path")
                        .length(12L)
                        .creationDateTime(2L)
                        .modificationDateTime(2L))));
        verifyNoMoreInteractions(syncWebSocketHandler);
    }
}
