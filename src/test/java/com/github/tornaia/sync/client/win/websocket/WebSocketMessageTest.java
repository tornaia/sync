package com.github.tornaia.sync.client.win.websocket;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class WebSocketMessageTest {

    @Test
    public void canParse() {
        String message = "{\"id\":\"57f536d845669f4324af0e1c\",\"userid\":\"42342\",\"relativePath\":\"1475688144638\",\"size\":\"7777\",\"creationDateTime\":\"1475688152764\",\"modificationDateTime\":\"1475688152765\"}";

        FileMetaInfo fileMetaInfo = new Gson().fromJson(message, FileMetaInfo.class);

        assertThat(fileMetaInfo, new FileMetaInfoMatcher()
                .id("57f536d845669f4324af0e1c")
                .userid("42342")
                .relativePath("1475688144638")
                .size(7777L)
                .creationDateTime(1475688152764L)
                .modificationDateTime(1475688152765L));
    }
}
