package com.bulletjournal.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * Tests {@link ContentDiffToolTest}
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ContentDiffTool.class)
@ActiveProfiles("test")
public class ContentDiffToolTest {

    @Autowired
    private ContentDiffTool contentDiffTool;

    private void assertDiffApplied(String originalText, String modifiedText) {
        String diff = contentDiffTool.computeDiff(originalText, modifiedText);
        String result = contentDiffTool.applyDiff(originalText, diff);
        Assert.assertEquals(modifiedText, result);
    }

    @Test
    public void testComputeDiffAndApply() {
        String text1 = "An array of differences is computed which describe " +
                "the transformation of text1 into text2. Each difference is " +
                "an array (JavaScript, Lua) or tuple (Python) or Diff object" +
                " (C++, C#, Objective C, Java). The first element specifies" +
                " if it is an insertion (1), a deletion (-1) or an equality" +
                " (0). The second element specifies the affected text.";
        String text2 = "An array of differences is computed describe " +
                "the transformation of text1 into text2. Each difference is " +
                " (C++, C#, Objective C, Java). The first element specifies" +
                "an array (JavaScript, Lua) or tuple (Python) or Diff object" +
                " if it is an insertion (1), a deletion (-1) or an equality" +
                " (0). The second element specifies the affected text.";
        String text3 = "An array of differences is computed which describe " +
                "the transformation of text1 into text2. Each difference is " +
                "an array (JavaScript, Lua) or tuple (Python) or Diff object" +
                " (C++, C#, Objective C, Java). The first element specifies" +
                " if it is an insertion (1), a deletion (-1) or an equality" +
                " (0). The second element specifies the affected text." +
                "an array (JavaScript, Lua) or tuple (Python) or Diff object";
        String text4 = "";

        assertDiffApplied(text1, text2);
        assertDiffApplied(text2, text3);
        assertDiffApplied(text1, text3);
        assertDiffApplied(text3, text4);
    }
}
