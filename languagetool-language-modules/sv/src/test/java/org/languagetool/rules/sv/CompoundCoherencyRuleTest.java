/* LanguageTool, a natural language style checker
 * Copyright (C) 2026 Katalyst Nord AB
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.sv;

import org.junit.Test;
import org.languagetool.AnalyzedSentence;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.TestTools;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CompoundCoherencyRuleTest {

  private final CompoundCoherencyRule rule = new CompoundCoherencyRule(TestTools.getEnglishMessages());
  private final JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("sv"));

  @Test
  public void testRule() throws IOException {
    // consistent spelling, same form used both times: no match
    assertOkay("Jag skickade ett e-mejl.", "Han fick också ett e-mejl.");
    assertOkay("Jag skickade ett epost.", "Han fick också ett epost.");
    // different words entirely: no match
    assertOkay("Jag har en dator.", "Han har en telefon.");
    // inflected forms of a consistently-spelled compound: no match
    assertOkay("Det är ett sms-lån.", "Han tog ett annat sms-lån.");
    assertOkay("Det är ett sms-lån.", "Kostnaden för sms-lånet är hög.");

    // inconsistent hyphenation of the same compound: match
    assertError("Jag skickade ett e-post.", "Han fick också ett epost.", null);
    assertError("Det är ett sms-lån.", "Han tog ett annat smslån.", null);
  }

  private void assertOkay(String s1, String s2) throws IOException {
    RuleMatch[] matches = getMatches(s1, s2);
    assertThat("Got " + Arrays.toString(matches), matches.length, is(0));
  }

  private void assertError(String s1, String s2, String suggestion) throws IOException {
    RuleMatch[] matches = getMatches(s1, s2);
    assertThat("Got " + Arrays.toString(matches), matches.length, is(1));
  }

  private RuleMatch[] getMatches(String s1, String s2) throws IOException {
    AnalyzedSentence sent1 = lt.getAnalyzedSentence(s1);
    AnalyzedSentence sent2 = lt.getAnalyzedSentence(s2);
    return rule.match(Arrays.asList(sent1, sent2));
  }

}
