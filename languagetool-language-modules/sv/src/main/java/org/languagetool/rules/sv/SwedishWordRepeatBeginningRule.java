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

import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.rules.Example;
import org.languagetool.rules.WordRepeatBeginningRule;

/**
 * Extends {@link WordRepeatBeginningRule} with a list of Swedish "konjunktionaladverbial"
 * (connective adverbs), same mechanism as {@code GermanWordRepeatBeginningRule}: two
 * successive sentences starting with the same connective adverb get a more specific
 * message than the generic three-in-a-row check.
 */
public class SwedishWordRepeatBeginningRule extends WordRepeatBeginningRule {

  private static final Set<String> ADVERBS = new HashSet<>(Arrays.asList(
          "Dessutom", "Dessvärre", "Därefter", "Därför", "Därmed", "Emellertid",
          "Följaktligen", "Likaledes", "Slutligen", "Vidare", "Ytterligare"
  ));

  public SwedishWordRepeatBeginningRule(ResourceBundle messages, Language language) {
    super(messages, language);
    addExamplePair(Example.wrong("Han var trött. Han hade sovit dåligt. <marker>Han</marker> bestämde sig för att gå och lägga sig tidigt."),
                   Example.fixed("Han var trött. Han hade sovit dåligt. <marker>Därför</marker> bestämde han sig för att gå och lägga sig tidigt."));
  }

  @Override
  public String getId() {
    return "SWEDISH_WORD_REPEAT_BEGINNING_RULE";
  }

  @Override
  protected boolean isAdverb(AnalyzedTokenReadings token) {
    return ADVERBS.contains(token.getToken());
  }

}
