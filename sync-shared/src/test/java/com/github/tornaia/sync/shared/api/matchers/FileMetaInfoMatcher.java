package com.github.tornaia.sync.shared.api.matchers;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

import static org.hamcrest.CoreMatchers.is;

public class FileMetaInfoMatcher extends AbstractTypeSafeDiagnosingMatcher<FileMetaInfo> {

    private Matcher<String> id = new IsAnything<>();

    private Matcher<String> userid = new IsAnything<>();

    private Matcher<String> relativePath = new IsAnything<>();

    private Matcher<Long> size = new IsAnything<>();

    private Matcher<Long> creationDateTime = new IsAnything<>();

    private Matcher<Long> modificationDateTime = new IsAnything<>();

    public FileMetaInfoMatcher id(String id) {
        this.id = is(id);
        return this;
    }

    public FileMetaInfoMatcher userid(String userid) {
        this.userid = is(userid);
        return this;
    }

    public FileMetaInfoMatcher relativePath(String relativePath) {
        this.relativePath = is(relativePath);
        return this;
    }

    public FileMetaInfoMatcher size(long size) {
        this.size = is(size);
        return this;
    }

    public FileMetaInfoMatcher creationDateTime(long creationDateTime) {
        this.creationDateTime = is(creationDateTime);
        return this;
    }

    public FileMetaInfoMatcher modificationDateTime(long modificationDateTime) {
        this.modificationDateTime = is(modificationDateTime);
        return this;
    }

    @Override
    protected boolean matchesSafely(FileMetaInfo item, Description mismatchDescription) {
        return matches(id, item.id, "id value: ", mismatchDescription) &&
                matches(userid, item.userid, "userid value: ", mismatchDescription) &&
                matches(relativePath, item.relativePath, "relativePath value: ", mismatchDescription) &&
                matches(size, item.size, "size value: ", mismatchDescription) &&
                matches(creationDateTime, item.creationDateTime, "creationDateTime value: ", mismatchDescription) &&
                matches(modificationDateTime, item.modificationDateTime, "modificationDateTime value: ", mismatchDescription);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(FileMetaInfo.class.getSimpleName())
                .appendText(", id: ").appendDescriptionOf(id)
                .appendText(", userid: ").appendDescriptionOf(userid)
                .appendText(", relativePath: ").appendDescriptionOf(relativePath)
                .appendText(", size: ").appendDescriptionOf(size)
                .appendText(", creationDateTime: ").appendDescriptionOf(creationDateTime)
                .appendText(", modificationDateTime: ").appendDescriptionOf(modificationDateTime);
    }
}
