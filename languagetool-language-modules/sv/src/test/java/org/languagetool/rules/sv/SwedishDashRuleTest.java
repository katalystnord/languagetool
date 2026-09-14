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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SwedishDashRuleTest {

  private final SwedishDashRule rule = new SwedishDashRule(TestTools.getEnglishMessages());
  private final JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("sv"));

  @Test
  public void testRule() throws IOException {
    assertMatchCount(1, "Skicka ett e–post till mig.");
    assertMatchCount(1, "Skicka ett e—post till mig.");
    assertMatchCount(1, "Skicka ett e – post till mig.");
    assertMatchCount(1, "Han köpte en Coca–Cola.");
    // correctly hyphenated: no match
    assertMatchCount(0, "Skicka ett e-post till mig.");
    assertMatchCount(0, "Han köpte en Coca-Cola.");
    // an unrelated dash elsewhere in the sentence: no match
    assertMatchCount(0, "Mötet – som var långt – tog slut sent.");
  }

  private void assertMatchCount(int expected, String sentence) throws IOException {
    AnalyzedSentence analyzed = lt.getAnalyzedSentence(sentence);
    RuleMatch[] matches = rule.match(analyzed);
    assertThat("Got " + matches.length + " for: " + sentence, matches.length, is(expected));
  }

}
