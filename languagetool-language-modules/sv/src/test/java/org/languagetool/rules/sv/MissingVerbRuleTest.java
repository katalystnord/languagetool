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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.TestTools;

public class MissingVerbRuleTest {

  private final MissingVerbRule rule = new MissingVerbRule(TestTools.getEnglishMessages(), Languages.getLanguageForShortCode("sv"));

  @Test
  public void test() throws IOException {
    JLanguageTool lt = new JLanguageTool(Languages.getLanguageForShortCode("sv"));

    assertGood("Här är ett verb, bara för att testa.", lt);
    assertGood("Rubrik utan verb men ändå inte för kort", lt);
    assertGood("Talar du kanske turkiska?", lt);
    assertGood("Lägg kassaskåpet i resväskan i bagageutrymmet.", lt);
    assertGood("Ta med dina barn.", lt);
    assertGood("Bra jobbat.", lt);  // no verb, but very short
    assertGood("Ja!", lt);  // no verb, but very short

    assertBad("Denna mening inget verb alls.", lt);
    assertBad("Jag ett nytt cykel igår.", lt);
  }

  private void assertGood(String text, JLanguageTool lt) throws IOException {
    assertEquals("Found unexpected error in: '" + text + "'", 0, rule.match(lt.getAnalyzedSentence(text)).length);
  }

  private void assertBad(String text, JLanguageTool lt) throws IOException {
    assertEquals("Did not find expected error in: '" + text + "'", 1, rule.match(lt.getAnalyzedSentence(text)).length);
  }

}
