package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.openapi.util.TextRange;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.Inspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

interface SpellcheckResultProvider {

    default SpellcheckResult getMockSpellcheckResult(String text, String... offenderWords) {
        List<SpellcheckAlert> alerts = new ArrayList<>();
        for (String offenderWord : offenderWords) {
            int start = text.indexOf(offenderWord);
            int end = start + offenderWord.length();
            com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.Alert alert = new com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.Alert();
            alert.setGroup("Tactical");
            alert.setTitle(offenderWord);
            alert.setContent(text);
            alert.setStart(start);
            alert.setEnd(end);
            alert.setReplacements(new String[] {"replacement"});
            alerts.add(alert);
        }
        return new SpellcheckResult(text, alerts, StringUtils.EMPTY);
    }

    default SpellcheckResult doBasicCheck(Inspectable inspectable, String[] offenderWords) {
        SpellcheckResult spellcheckResult = getMockSpellcheckResult(inspectable.getText(), offenderWords);
        for (SpellcheckAlert alert : spellcheckResult.getAlerts()) {
            TextRange amendedRange = inspectable.toRangeInElement(alert.getRange());
            Assert.assertEquals(alert.getTitle(), amendedRange.substring(inspectable.getElement().getText()));
        }
        return spellcheckResult;
    }
}
