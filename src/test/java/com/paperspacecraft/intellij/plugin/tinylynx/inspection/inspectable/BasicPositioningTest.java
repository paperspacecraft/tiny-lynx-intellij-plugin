package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

public class BasicPositioningTest extends TestCase {

    private static final String SINGLELINE_COMMENT = "        // Comment string !  \n";
    private static final String SINGLELINE_COMMENT_SANITIZED = "Comment string !";

    private static final String COMPLEX_SINGLELINE_COMMENT =
            "        // Comment has started in one line.  \n" +
            "        // Then it transfers to another.";
    private static final String COMPLEX_SINGLELINE_COMMENT_SANITIZED = "Comment has started in one line. Then it transfers to another.";

    private static final String MULTILINE_COMMENT =
            "        /*\n" +
            "            Multiline comment\n" +
            "          \n " +
            "            transferred to the second line,  \n" +
            "            and yet one more\n" +
            "         */\r\n";
    private static final String MULTILINE_COMMENT_SANITIZED = "Multiline comment transferred to the second line, and yet one more";

    public void testSingleLineCommentParsing() {
        CommentInspectable commentInspectable = new CommentInspectable(SINGLELINE_COMMENT);
        Assert.assertEquals(SINGLELINE_COMMENT_SANITIZED, commentInspectable.getText());
        doWordByWordCheck(SINGLELINE_COMMENT, SINGLELINE_COMMENT_SANITIZED, commentInspectable);
    }

    public void testComplexSingleLineCommentParsing() {
        CommentInspectable commentInspectable = new CommentInspectable(COMPLEX_SINGLELINE_COMMENT);
        Assert.assertEquals(COMPLEX_SINGLELINE_COMMENT_SANITIZED, commentInspectable.getText());
        doWordByWordCheck(COMPLEX_SINGLELINE_COMMENT, COMPLEX_SINGLELINE_COMMENT_SANITIZED, commentInspectable);
    }

    public void testMultiLineCommentParsing() {
        CommentInspectable commentInspectable = new CommentInspectable( MULTILINE_COMMENT);
        Assert.assertEquals(MULTILINE_COMMENT_SANITIZED, commentInspectable.getText());
        doWordByWordCheck(MULTILINE_COMMENT, MULTILINE_COMMENT_SANITIZED, commentInspectable);
    }

    private static void doWordByWordCheck(String source, String sanitized, CommentInspectable commentInspectable) {
        String[] words = sanitized.split(StringUtils.SPACE);
        for (String word : words) {
            int sanitizedPosition = sanitized.indexOf(word);
            int absolutePosition = commentInspectable.toPositionInElement(sanitizedPosition);
            Assert.assertEquals(word, source.substring(absolutePosition, absolutePosition + word.length()));
        }
    }
}
