package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.ParagraphInspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckResult;
import org.junit.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MarkdownProblemRegisteringTest extends MarkdownCodeInsightBase implements SpellcheckResultProvider {

    public void testExclusionByEmphasis() throws IOException {
        ParagraphInspectable paragraphInspectable = new ParagraphInspectable(getParagraph(1));
        String[] offenderWords = new String[] {"mistaken", "denouncing", "circumstances", "resultant", "laborious", "Nor"};
        String[] skippedOffenderWords = new String[] {"circumstances", "resultant", "laborious", "Nor"};

        SpellcheckResult spellcheckResult = doBasicCheck(paragraphInspectable, offenderWords);
        Assert.assertEquals(offenderWords.length, spellcheckResult.getAlerts().size());

        List<SpellcheckAlert> relevantAlerts = spellcheckResult
                .getAlerts()
                .stream()
                .filter(paragraphInspectable::isAlertRelevant)
                .collect(Collectors.toList());
        Assert.assertTrue(Arrays.stream(skippedOffenderWords).noneMatch(word -> relevantAlerts.stream().anyMatch(alert -> alert.getTitle().equals(word))));
    }

    public void testExclusionByHyperlink() throws IOException {
        ParagraphInspectable paragraphInspectable = new ParagraphInspectable(getParagraph(3));
        String[] offenderWords = new String[] {"righteous", "indignation", "denounce", "beguiled"};
        String[] skippedOffenderWords = new String[] {"righteous", "indignation"};

        SpellcheckResult spellcheckResult = doBasicCheck(paragraphInspectable, offenderWords);
        Assert.assertEquals(offenderWords.length, spellcheckResult.getAlerts().size());

        List<SpellcheckAlert> relevantAlerts = spellcheckResult
                .getAlerts()
                .stream()
                .filter(paragraphInspectable::isAlertRelevant)
                .collect(Collectors.toList());
        Assert.assertTrue(Arrays.stream(skippedOffenderWords).noneMatch(word -> relevantAlerts.stream().anyMatch(alert -> alert.getTitle().equals(word))));
    }
}
