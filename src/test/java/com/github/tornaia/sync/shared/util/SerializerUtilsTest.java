package com.github.tornaia.sync.shared.util;

import com.github.tornaia.sync.shared.api.CreateFileRequest;
import com.github.tornaia.sync.shared.api.CreateFileRequestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class SerializerUtilsTest {

    @InjectMocks
    private SerializerUtils serializerUtils;

    @Test
    public void nonAsciCharsAreEscaped() {
        String uglyFilename = "" + (char) 56036;

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("100001")
                .relativePath(uglyFilename)
                .creationDateTime(1000L)
                .modificationDateTime(2000L)
                .size(10L)
                .create();

        String actual = serializerUtils.toJSON(createFileRequest);
        String expected = "{\"userid\":\"100001\",\"relativePath\":\"\\uDAE4\",\"size\":10,\"creationDateTime\":1000,\"modificationDateTime\":2000}";
        assertEquals(expected, actual);
    }
}
