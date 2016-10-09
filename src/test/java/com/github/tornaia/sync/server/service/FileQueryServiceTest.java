package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileQueryServiceTest {

    @Rule
    public ExpectedException expectedException = none();

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileQueryService fileQueryService;

    @Before
    public void setUp() {
        List<File> storedFiles = Arrays.asList(new File("userid", "path", "data".getBytes(), 3L, 3L));
        when(fileRepository.findByUseridAndLastModifiedDateAfter("userid", 2L)).thenReturn(storedFiles);
    }

    @Test
    public void getModifiedFilesIfNoCollectionInDB() {
        when(fileRepository.findByUseridAndLastModifiedDateAfter("userid", 1L)).thenReturn(null);

        List<FileMetaInfo> result = fileQueryService.getModifiedFiles("userid", 1L);

        assertThat(result.isEmpty(), is(true));

        verify(fileRepository).findByUseridAndLastModifiedDateAfter("userid", 1L);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    public void getModifiedFiles() {
        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .userid("userid")
                .relativePath("path")
                .modificationDateTime(3L)
                .creationDateTime(3L);

        List<FileMetaInfo> result = fileQueryService.getModifiedFiles("userid", 2L);

        assertThat(result, contains(expected));

        verify(fileRepository).findByUseridAndLastModifiedDateAfter("userid", 2L);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    public void getFileByIdIfFileNotFound() {
        when(fileRepository.findOne("")).thenReturn(null);

        expectedException.expect(FileNotFoundException.class);
        fileQueryService.getFileById("");
    }

    @Test
    public void getMetaInfoByIdIfFileNotFound() {
        when(fileRepository.findOne("")).thenReturn(null);

        expectedException.expect(FileNotFoundException.class);
        fileQueryService.getFileMetaInfoById("");
    }
}
