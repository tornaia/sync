package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.config.SpringS3Config;
import com.github.tornaia.sync.server.data.converter.FileToFileMetaInfoConverter;
import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.websocket.SyncWebSocketHandler;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import com.github.tornaia.sync.shared.api.matchers.RemoteFileEventMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;

import static com.github.tornaia.sync.shared.api.RemoteEventType.CREATED;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
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

    @Mock
    private FileToFileMetaInfoConverter fileToFileMetaInfoConverter;

    @Mock
    private SpringS3Config springS3Config;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private FileCommandService commandService;

    @Test
    public void createIfFileExists() throws Exception {
        when(fileRepository.findByUseridAndPath("userid", "path")).thenReturn(new File());

        expectedException.expect(FileAlreadyExistsException.class);
        commandService.createFile("clientid", "userid", 1L, 2L, 666L, "path", new ByteArrayInputStream("Test".getBytes()));
    }

    @Test
    public void createFile() throws Exception {
        when(fileRepository.insert(any(File.class))).thenReturn(new File("userid", "path", 2L, 3L, 666L));
        when(fileToFileMetaInfoConverter.convert(any(File.class))).thenReturn(new FileMetaInfo(null, "userid", "path", 12, 2L, 3L));

        commandService.createFile("clientid", "userid", 2L, 3L, 666L, "path", new ByteArrayInputStream("test_content".getBytes()));

        verify(syncWebSocketHandler).notifyClientsExceptForSource(argThat(is("clientid")), argThat(new RemoteFileEventMatcher()
                .eventType(CREATED)
                .fileMetaInfo(new FileMetaInfoMatcher()
                        .userid("userid")
                        .relativePath("path")
                        .size(12L)
                        .creationDateTime(2L)
                        .modificationDateTime(3L))));
        verifyNoMoreInteractions(syncWebSocketHandler);
    }
}
