package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;
import com.github.tornaia.sync.shared.api.matchers.AbstractTypeSafeDiagnosingMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

import static org.hamcrest.CoreMatchers.is;

public class LocalFileEventMatcher extends AbstractTypeSafeDiagnosingMatcher<AbstractLocalFileEvent> {

    private Matcher<LocalFileEventType> eventType = new IsAnything<>();

    private Matcher<String> relativePath = new IsAnything<>();

    public LocalFileEventMatcher eventType(LocalFileEventType eventType) {
        this.eventType = is(eventType);
        return this;
    }

    public LocalFileEventMatcher relativePath(String relativePath) {
        this.relativePath = is(relativePath);
        return this;
    }

    @Override
    protected boolean matchesSafely(AbstractLocalFileEvent item, Description mismatchDescription) {
        return matches(eventType, item.eventType, "eventType: ", mismatchDescription) &&
                matches(relativePath, item.relativePath, "relativePath: ", mismatchDescription);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(AbstractLocalFileEvent.class.getSimpleName())
                .appendText(", eventType: ").appendDescriptionOf(eventType)
                .appendText(", relativePath: ").appendDescriptionOf(relativePath);
    }
}
