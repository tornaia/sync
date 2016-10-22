package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.shared.api.matchers.AbstractSyncMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static org.hamcrest.Matchers.is;

public class DirectoryMatcher extends AbstractSyncMatcher<File> {

    private Path rootDirectory;

    private Matcher<String> relativePath = new IsAnything<>();

    private Matcher<Long> creationTime = new IsAnything<>();

    private Matcher<Long> lastModifiedTime = new IsAnything<>();

    public DirectoryMatcher(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public DirectoryMatcher relativePath(String relativePath) {
        this.relativePath = is(relativePath);
        return this;
    }

    public DirectoryMatcher creationTime(long creationTime) {
        this.creationTime = is(creationTime);
        return this;
    }

    public DirectoryMatcher lastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = is(lastModifiedTime);
        return this;
    }

    @Override
    protected boolean matchesSafely(File item, Description mismatchDescription) {
        try {
            return matches(relativePath, rootDirectory.relativize(item.toPath()).toString(), "relativePath: ", mismatchDescription) &&
                    matches(creationTime, Files.readAttributes(item.toPath(), BasicFileAttributes.class).creationTime().toMillis(), "creationTime: ", mismatchDescription) &&
                    matches(lastModifiedTime, Files.readAttributes(item.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis(), "lastModifiedTime: ", mismatchDescription);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(File.class.getSimpleName())
                .appendText(", relativePath: ").appendDescriptionOf(relativePath)
                .appendText(", creationTime: ").appendDescriptionOf(creationTime)
                .appendText(", lastModifiedTime: ").appendDescriptionOf(lastModifiedTime);
    }
}