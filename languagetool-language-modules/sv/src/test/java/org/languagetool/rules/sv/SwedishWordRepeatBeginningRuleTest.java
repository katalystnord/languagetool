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
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SwedishWordRepeatBeginningRuleTest {

  private final SwedishWordRepeatBeginningRule rule =
          new SwedishWordRepeatBeginningRule(TestTools.getEnglishMessages(), Languages.getLanguageForShortCode("sv"));
  private final JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("sv"));

  @Test
  public void testRule() throws IOException {
    // three sentences in a row starting with the same word: match on the third
    assertMatchCount(1, "Han gick till affären.", "Han köpte mjölk.", "Han gick hem igen.");
    // two sentences starting with the same connective adverb: match
    assertMatchCount(1, "Han var trött.", "Dessutom var det sent.", "Dessutom hade han inte ätit.");
    // different opening words: no match
    assertMatchCount(0, "Han gick till affären.", "Hon köpte mjölk.", "De gick hem igen.");
    // only two plain (non-adverb) sentences repeating: no match, needs three
    assertMatchCount(0, "Han gick till affären.", "Han köpte mjölk.");
  }

  private void assertMatchCount(int expected, String... sentences) throws IOException {
    List<AnalyzedSentence> analyzed = new ArrayList<>();
    for (String s : sentences) {
      analyzed.add(lt.getAnalyzedSentence(s));
    }
    RuleMatch[] matches = rule.match(analyzed);
    assertThat("Got " + Arrays.toString(matches), matches.length, is(expected));
  }

}
