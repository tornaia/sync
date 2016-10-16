package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.shared.api.matchers.AbstractSyncMatcher;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static org.hamcrest.Matchers.is;

public class FileMatcher extends AbstractSyncMatcher<File> {

    private Path rootDirectory;

    private Matcher<String> relativePath = new IsAnything<>();

    private Matcher<Long> length = new IsAnything<>();

    private Matcher<Long> creationTime = new IsAnything<>();

    private Matcher<Long> lastModifiedTime = new IsAnything<>();

    private Matcher<String> content = new IsAnything<>();

    public FileMatcher(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public FileMatcher relativePath(String relativePath) {
        this.relativePath = is(relativePath);
        return this;
    }

    public FileMatcher length(long length) {
        this.length = is(length);
        return this;
    }

    public FileMatcher creationTime(long creationTime) {
        this.creationTime = is(creationTime);
        return this;
    }

    public FileMatcher lastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = is(lastModifiedTime);
        return this;
    }

    public FileMatcher content(String content) {
        this.content = is(content);
        return this;
    }

    @Override
    protected boolean matchesSafely(File item, Description mismatchDescription) {
        try {
            return matches(relativePath, rootDirectory.relativize(item.toPath()).toString(), "relativePath: ", mismatchDescription) &&
                    matches(length, Files.readAttributes(item.toPath(), BasicFileAttributes.class).size(), "length: ", mismatchDescription) &&
                    matches(creationTime, Files.readAttributes(item.toPath(), BasicFileAttributes.class).creationTime().toMillis(), "creationTime: ", mismatchDescription) &&
                    matches(lastModifiedTime, Files.readAttributes(item.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis(), "lastModifiedTime: ", mismatchDescription) &&
                    matches(content, FileUtils.readFileToString(item), "content: ", mismatchDescription);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(File.class.getSimpleName())
                .appendText(", relativePath: ").appendDescriptionOf(relativePath)
                .appendText(", length: ").appendDescriptionOf(length)
                .appendText(", creationTime: ").appendDescriptionOf(creationTime)
                .appendText(", lastModifiedTime: ").appendDescriptionOf(lastModifiedTime)
                .appendText(", content: ").appendDescriptionOf(content);
    }
}