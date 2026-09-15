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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SwedishPassiveSentenceRuleTest {

  private final SwedishPassiveSentenceRule rule =
          new SwedishPassiveSentenceRule(TestTools.getEnglishMessages(), Languages.getLanguageForShortCode("sv"), null);
  private final JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("sv"));

  @Test
  public void testRule() throws IOException {
    // 3 out of 4 sentences passive (75%), well above the 40% default: match expected
    assertMatchCount(true, "Boken skrevs av honom.", "Han blev sedd av alla.", "Bilen tvättades igår.", "Han sprang hem.");
    // no passive sentences: no match
    assertMatchCount(false, "Han skrev boken.", "Alla såg honom.", "Han sprang hem.");
    // deponent verbs (finns, trivs, hoppas) must not be counted as passive
    assertMatchCount(false, "Det finns en bok. Han trivs bra. Jag hoppas på det bästa. Han sprang hem.");
  }

  private void assertMatchCount(boolean expectMatch, String... sentences) throws IOException {
    List<AnalyzedSentence> analyzed = new ArrayList<>();
    for (String s : sentences) {
      analyzed.add(lt.getAnalyzedSentence(s));
    }
    RuleMatch[] matches = rule.match(analyzed);
    if (expectMatch) {
      assertThat("Expected a match, got none", matches.length > 0, is(true));
    } else {
      assertThat("Got unexpected match(es)", matches.length, is(0));
    }
  }

}
