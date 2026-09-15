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

public class SwedishReadabilityRuleTest {

  private final SwedishReadabilityRule tooEasyRule =
          new SwedishReadabilityRule(TestTools.getEnglishMessages(), Languages.getLanguageForShortCode("sv"), null, true);
  private final SwedishReadabilityRule tooDifficultRule =
          new SwedishReadabilityRule(TestTools.getEnglishMessages(), Languages.getLanguageForShortCode("sv"), null, false);
  private final JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("sv"));

  @Test
  public void testTooEasy() throws IOException {
    // simple, short-word, short-sentence text (needs 2+ paragraphs to trigger, same as the German original)
    String easy = "Katten satt på mattan. Hunden sprang i parken. Solen sken varmt. Barnen lekte glatt.\n\n"
            + "De åt glass i solen. Mamma log mot dem. Det var en fin dag. Alla var glada nu.";
    assertThat(matchCount(tooEasyRule, easy) > 0, is(true));
    assertThat(matchCount(tooDifficultRule, easy), is(0));
  }

  @Test
  public void testTooDifficult() throws IOException {
    String hard = "Ansökningsförfarandet för tillståndsgivning avseende verksamhetsutövning inom det aktuella "
            + "regelverksområdet förutsätter dokumenterad kompetensutveckling och kvalitetssäkringsprocesser "
            + "i enlighet med gällande författningsbestämmelser och myndighetsföreskrifter.\n\n"
            + "Implementeringen av kvalitetssäkringssystemet kräver omfattande dokumentationsunderlag samt "
            + "kontinuerlig uppföljning av verksamhetsresultat i förhållande till fastställda målsättningar "
            + "och prestationsindikatorer inom organisationsstrukturen.";
    assertThat(matchCount(tooDifficultRule, hard) > 0, is(true));
    assertThat(matchCount(tooEasyRule, hard), is(0));
  }

  private int matchCount(SwedishReadabilityRule rule, String text) throws IOException {
    List<AnalyzedSentence> analyzed = lt.analyzeText(text);
    return rule.match(analyzed).length;
  }

}
