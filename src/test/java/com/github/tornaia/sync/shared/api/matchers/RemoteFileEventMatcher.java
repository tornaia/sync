package com.github.tornaia.sync.shared.api.matchers;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteEventType;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

import static org.hamcrest.CoreMatchers.is;

public class RemoteFileEventMatcher extends AbstractSyncMatcher<RemoteFileEvent> {

    private Matcher<RemoteEventType> eventType = new IsAnything<>();

    private Matcher<FileMetaInfo> fileMetaInfo = new IsAnything<>();

    public RemoteFileEventMatcher eventType(RemoteEventType eventType) {
        this.eventType = is(eventType);
        return this;
    }

    public RemoteFileEventMatcher fileMetaInfo(Matcher<FileMetaInfo> fileMetaInfo) {
        this.fileMetaInfo = fileMetaInfo;
        return this;
    }

    @Override
    protected boolean matchesSafely(RemoteFileEvent item, Description mismatchDescription) {
        return matches(eventType, item.eventType, "eventType value: ", mismatchDescription) &&
                matches(fileMetaInfo, item.fileMetaInfo, "fileMetaInfo value: ", mismatchDescription);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(RemoteFileEvent.class.getSimpleName())
                .appendText(", eventType: ").appendDescriptionOf(eventType)
                .appendText(", fileMetaInfo: ").appendDescriptionOf(fileMetaInfo);
    }

}