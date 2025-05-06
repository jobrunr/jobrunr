package org.jobrunr.tests.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PageAssertions;
import com.microsoft.playwright.impl.LocatorAssertionsImpl;
import com.microsoft.playwright.impl.PageAssertionsImpl;
import org.assertj.core.api.Assertions;

public class PlaywrightAssertions extends Assertions {

    /**
     * Creates a {@code LocatorAssertions} object for the given {@code Locator}.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * PlaywrightAssertions.assertThat(locator).isVisible();
     * }</pre>
     *
     * @param locator {@code Locator} object to use for assertions.
     * @since v1.18
     */
    public static LocatorAssertions assertThat(Locator locator) {
        return new LocatorAssertionsImpl(locator);
    }

    /**
     * Creates a {@code PageAssertions} object for the given {@code Page}.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * PlaywrightAssertions.assertThat(page).hasTitle("News");
     * }</pre>
     *
     * @param page {@code Page} object to use for assertions.
     * @since v1.18
     */
    public static PageAssertions assertThat(Page page) {
        return new PageAssertionsImpl(page);
    }

}
