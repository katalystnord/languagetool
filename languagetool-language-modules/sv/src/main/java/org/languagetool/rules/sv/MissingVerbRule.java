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

import org.apache.commons.lang3.StringUtils;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.rules.*;
import org.languagetool.tools.StringTools;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Checks that a sentence contains at least one verb. Ignores very short
 * sentences. Port of the German {@code MissingVerbRule}: same mechanism
 * (a whole-sentence verb-presence scan, not a chunker), adapted to this
 * tagger's tagset ("VB" verb prefix, "." punctuation tag rather than
 * German's "VER"/"PKT").
 *
 * Off by default, same as the German original, and for the same reason:
 * real Swedish text (like real German text) is full of grammatically
 * legitimate verbless sentences (interjections, exclamations, headline-
 * style fragments, answers to implicit questions), and this rule can't
 * tell those apart from a genuine missing-verb error without a real
 * discourse/context model. Verified against the full 17,760-sentence
 * Talbanken+LinES corpus via a dedicated check (see
 * MissingVerbRuleCorpusCheck.java): even after tuning the minimum
 * sentence length, the false-positive rate stays too high to enable by
 * default; see that class's own comment and implementation-plan.md for
 * the numbers and the specific sentence shapes involved.
 */
public class MissingVerbRule extends Rule {

  private static final int MIN_TOKENS_FOR_ERROR = 5;

  private final Language language;

  public MissingVerbRule(ResourceBundle messages, Language language) {
    this.language = language;
    super.setCategory(Categories.GRAMMAR.getCategory(messages));
    setDefaultOff();
    addExamplePair(Example.wrong("<marker>I denna mening inget ord.</marker>"),
                   Example.fixed("I denna mening <marker>saknas</marker> inget ord."));
  }

  @Override
  public String getId() {
    return "SV_MISSING_VERB";
  }

  @Override
  public String getDescription() {
    return "Mening utan verb";
  }

  @Override
  public RuleMatch[] match(AnalyzedSentence sentence) throws IOException {
    if (!isRealSentence(sentence)) {
      return RuleMatch.EMPTY_ARRAY;
    }
    boolean verbFound = false;
    AnalyzedTokenReadings lastToken = null;
    int i = 0;
    for (AnalyzedTokenReadings readings : sentence.getTokensWithoutWhitespace()) {
      if (readings.hasPosTagStartingWith("VB")
          || (!readings.isTagged() && !StringTools.isCapitalizedWord(readings.getToken()))
          || (i == 1 && verbAtSentenceStart(readings))) {
        verbFound = true;
        break;
      }
      lastToken = readings;
      i++;
    }
    if (!verbFound && lastToken != null && sentence.getTokensWithoutWhitespace().length >= MIN_TOKENS_FOR_ERROR) {
      RuleMatch match = new RuleMatch(this, sentence, 0, lastToken.getStartPos() + lastToken.getToken().length(), "Denna mening verkar sakna ett verb.");
      return new RuleMatch[]{ match };
    }
    return RuleMatch.EMPTY_ARRAY;
  }

  // we want to ignore headlines, and these usually don't end with [.?!]
  private boolean isRealSentence(AnalyzedSentence sentence) {
    AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
    if (tokens.length > 0) {
      AnalyzedTokenReadings lastToken = tokens[tokens.length - 1];
      return lastToken.hasPosTag(".") && StringUtils.equalsAny(lastToken.getToken(), ".", "?", "!");
    }
    return false;
  }

  private boolean verbAtSentenceStart(AnalyzedTokenReadings readings) throws IOException {
    // start of sentence is mis-tagged because of the uppercase first character, work around that:
    String lowercased = StringTools.lowercaseFirstChar(readings.getToken());
    List<AnalyzedTokenReadings> lcReadings = language.getTagger().tag(Collections.singletonList(lowercased));
    return lcReadings.size() > 0 && lcReadings.get(0).hasPosTagStartingWith("VB");
  }

}
