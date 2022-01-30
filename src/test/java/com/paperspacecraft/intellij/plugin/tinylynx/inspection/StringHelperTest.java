package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.openapi.util.TextRange;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringHelperTest extends TestCase {

    public void testGetFarthestSpaceToTheLeft() {
        Assert.assertEquals(0, StringHelper.getFarthestSpaceToTheLeft("Hello", 0));
        Assert.assertEquals(0, StringHelper.getFarthestSpaceToTheLeft("  Hello", 1));
        Assert.assertEquals(5, StringHelper.getFarthestSpaceToTheLeft("Hello  world", 5));
        Assert.assertEquals(5, StringHelper.getFarthestSpaceToTheLeft("Hello  world", 10));
    }

    public void testGetAfterFarthestSpaceToTheRight() {
        Assert.assertEquals(5, StringHelper.getAfterFarthestSpaceToTheRight("Hello"));
        Assert.assertEquals(7, StringHelper.getAfterFarthestSpaceToTheRight("Hello  world"));
        Assert.assertEquals(12, StringHelper.getAfterFarthestSpaceToTheRight("Hello  world", 2));
        Assert.assertEquals(13, StringHelper.getAfterFarthestSpaceToTheRight("Hello world  hello", 2));
        Assert.assertEquals(18, StringHelper.getAfterFarthestSpaceToTheRight("Hello world *hello", 3));
    }

    public void testIsWithinJavadocTag() {
        final String text = "Some text having {@code javadoc tag} and {@link another tag}";
        Assert.assertTrue(StringHelper.isWithinJavadocTag(text, getTextRange(text, "tag")));
        Assert.assertTrue(StringHelper.isWithinJavadocTag(text, getTextRange(text, "javadoc tag")));
        Assert.assertTrue(StringHelper.isWithinJavadocTag(text, getTextRange(text, "another")));
        Assert.assertFalse(StringHelper.isWithinJavadocTag(text, getTextRange(text, "javadoc tag}")));
        Assert.assertFalse(StringHelper.isWithinJavadocTag(text, getTextRange(text, "having")));
    }

    public void testIsWithinCodeSnippet() {
        final String text = "Some text having ```code sni`p`pet``` and `another snippet`";
        Assert.assertTrue(StringHelper.isWithinCodeSnippet(text, getTextRange(text, "code")));
        Assert.assertTrue(StringHelper.isWithinCodeSnippet(text, getTextRange(text, "sni`p`pet")));
        Assert.assertTrue(StringHelper.isWithinCodeSnippet(text, getTextRange(text, "another")));
        Assert.assertFalse(StringHelper.isWithinCodeSnippet(text, getTextRange(text, "and")));
        Assert.assertFalse(StringHelper.isWithinCodeSnippet(text, getTextRange(text, "having")));
    }

    public void testIsWithinDelimiters() {
        final String text = "Some text with *emphasis_1* and _emphasis*2_";
        Assert.assertTrue(StringHelper.isWithinDelimiters(text, getTextRange(text, "emphasis_1"), "*"));
        Assert.assertTrue(StringHelper.isWithinDelimiters(text, getTextRange(text, "emphasis*2"), "_"));
        Assert.assertFalse(StringHelper.isWithinDelimiters(text, getTextRange(text, "emphasis_1"), "_"));
        Assert.assertFalse(StringHelper.isWithinDelimiters(text, getTextRange(text, "some"), "_"));
    }

    public void testExtractingBrackets() {
        final String text = "Some [text](having (nes()ted) brackets[) ";
        List<TextRange> squareBrackets = StringHelper.getDelimitedRanges(text, "[", "]");
        Assert.assertEquals(1, squareBrackets.size());
        Assert.assertEquals("[text]", squareBrackets.get(0).substring(text));

        List<TextRange> brackets = StringHelper.getDelimitedRanges(text, "(", ")");
        Assert.assertEquals(3, brackets.size());
        List<String> bracketsTexts = brackets.stream().map(b -> b.substring(text)).collect(Collectors.toList());
        Assert.assertTrue(Stream.of("()", "(nes()ted)", "(having (nes()ted) brackets[)").allMatch(bracketsTexts::contains));
    }

    public void testWhitespaceMatcher() {
        final String text = " // Some \n text *///";
        StringHelper.WhitespaceMatcher matcher = StringHelper.whitespaceMatcher(text);
        LinkedList<TextRange> ranges = new LinkedList<>();
        while (matcher.find()) {
            ranges.add(new TextRange(matcher.getStart(), matcher.getEnd()));
        }
        Assert.assertEquals(3, ranges.size());
        StringBuilder rest = new StringBuilder(text);
        ranges.descendingIterator().forEachRemaining(r -> rest.replace(r.getStartOffset(), r.getEndOffset(), ""));
        Assert.assertEquals("Sometext", rest.toString());
    }


    private static TextRange getTextRange(String haystack, String needle) {
        int position = haystack.indexOf(needle);
        if (position < 0) {
            return TextRange.EMPTY_RANGE;
        }
        return new TextRange(position, position + needle.length());
    }
}
