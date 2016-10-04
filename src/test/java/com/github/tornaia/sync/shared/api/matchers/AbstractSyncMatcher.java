package com.github.tornaia.sync.shared.api.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public abstract class AbstractSyncMatcher<T> extends TypeSafeDiagnosingMatcher<T> {

  protected <X> boolean matches(Matcher<? extends X> matcher, X value, String attribute, Description mismatchDescription) {
    if (!matcher.matches(value)) {
      mismatchDescription.appendText(" " + attribute + " ");
      matcher.describeMismatch(value, mismatchDescription);
      return false;
    } else {
      return true;
    }
  }

}
