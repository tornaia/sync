package com.github.tornaia.sync.shared.api.matchers;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

import static org.hamcrest.CoreMatchers.is;

public class FileMetaInfoMatcher extends AbstractSyncMatcher<FileMetaInfo> {

  private Matcher<String> id = new IsAnything<>();

  private Matcher<String> relativePath = new IsAnything<>();

  private Matcher<Long> length = new IsAnything<>();

  private Matcher<Long> creationDateTime = new IsAnything<>();

  private Matcher<Long> modificationDateTime = new IsAnything<>();

  public FileMetaInfoMatcher id(String id) {
    this.id = is(id);
    return this;
  }

  public FileMetaInfoMatcher relativePath(String relativePath) {
    this.relativePath = is(relativePath);
    return this;
  }

  public FileMetaInfoMatcher length(Long length) {
    this.length = is(length);
    return this;
  }

  public FileMetaInfoMatcher creationDateTime(Long creationDateTime) {
    this.creationDateTime = is(creationDateTime);
    return this;
  }

  public FileMetaInfoMatcher modificationDateTime(Long modificationDateTime) {
    this.modificationDateTime = is(modificationDateTime);
    return this;
  }

  @Override
  protected boolean matchesSafely(FileMetaInfo item, Description mismatchDescription) {
    return matches(id, item.id, "id value: ", mismatchDescription) &&
      matches(relativePath, item.relativePath, "relativePath value: ", mismatchDescription) &&
      matches(length, item.length, "length value: ", mismatchDescription) &&
      matches(creationDateTime, item.creationDateTime, "creationDateTime value: ", mismatchDescription) &&
      matches(modificationDateTime, item.modificationDateTime, "modificationDateTime value: ", mismatchDescription);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(FileMetaInfo.class.getSimpleName())
      .appendText(", id: ").appendDescriptionOf(id)
      .appendText(", relativePath: ").appendDescriptionOf(relativePath)
      .appendText(", length: ").appendDescriptionOf(length)
      .appendText(", creationDateTime: ").appendDescriptionOf(creationDateTime)
      .appendText(", modificationDateTime: ").appendDescriptionOf(modificationDateTime);
  }
}
