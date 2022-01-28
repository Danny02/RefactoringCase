package dev.nullzwo.interview.refactoring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllTests {

    @Test
    void myFirstTest() {
        assertTrue(true, "always true");
        assertThat("this").isEqualTo("this");
    }
}
