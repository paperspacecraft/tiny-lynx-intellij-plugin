package com.paperspacecraft.intellij.plugin.tinylynx;

import com.paperspacecraft.intellij.plugin.tinylynx.inspection.JavaProblemRegisteringTest;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.StringHelperTest;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.BasicPositioningTest;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.InspectableTest;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.DebouncerTest;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckServiceTest;
import junit.framework.Test;
import junit.framework.TestSuite;

@SuppressWarnings("squid:S2187")
public class AllTests extends TestSuite {

    public static Test suite() {
        final AllTests suite = new AllTests();
        suite.addTestSuite(StringHelperTest.class);
        suite.addTestSuite(BasicPositioningTest.class);
        suite.addTestSuite(InspectableTest.class);
        suite.addTestSuite(JavaProblemRegisteringTest.class);
        suite.addTestSuite(SpellcheckServiceTest.class);
        suite.addTestSuite(DebouncerTest.class);
        return suite;
    }
}
