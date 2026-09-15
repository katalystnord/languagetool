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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.rules.Category;
import org.languagetool.rules.CategoryId;
import org.languagetool.rules.Category.Location;
import org.languagetool.rules.ITSIssueType;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.RuleOption;
import org.languagetool.rules.TextLevelRule;
import org.languagetool.tools.Tools;

/**
 * Swedish readability check using LIX (läsbarhetsindex), not a port of the
 * core {@code ReadabilityRule}'s Flesch-Reading-Ease formula: that class's
 * level-categorization thresholds are hardcoded to Flesch's 0-100 scale in
 * a private method, so a different formula with a different scale (LIX
 * has no fixed upper bound and doesn't use syllable counts at all) can't
 * cleanly override just the formula and get correct levels out. Same
 * on/off "too easy"/"too difficult" configurable-level shape as the
 * German/English readability rules otherwise, registered as two instances
 * the same way.
 *
 * LIX = words/sentences + 100 * (words longer than 6 characters) / words.
 * Standard Swedish interpretation bands (Björnsson, 1968), levels 0-4:
 * &lt;30 mycket lättläst (children's books), 30-40 lättläst (fiction,
 * popular magazines), 40-50 medelsvår (normal newspaper text), 50-60
 * svår (normal for official texts), &gt;60 mycket svår (bureaucratic
 * Swedish).
 */
public class SwedishReadabilityRule extends TextLevelRule {

  private static final int MIN_WORDS = 10;
  private static final int MARK_WORDS = 3;

  private final Language lang;
  private final boolean tooEasyTest;
  private final int level;
  private int nAllSentences = 0;
  private int nAllWords = 0;
  private int nAllLongWords = 0;

  public SwedishReadabilityRule(ResourceBundle messages, Language lang, UserConfig userConfig, boolean tooEasyTest) {
    super(messages);
    super.setCategory(new Category(new CategoryId("TEXT_ANALYSIS"), "Textanalys", Location.INTERNAL, false));
    setLocQualityIssueType(ITSIssueType.Style);
    setDefaultOff();
    this.lang = lang;
    this.tooEasyTest = tooEasyTest;
    int tmpLevel = -1;
    if (userConfig != null) {
      Object[] cf = userConfig.getConfigValueByID(getId());
      if (cf != null && cf.length > 0) {
        tmpLevel = (int) cf[0];
      }
    }
    this.level = tmpLevel >= 0 ? tmpLevel : 2;
  }

  @Override
  public String getId() {
    return tooEasyTest ? "LIX_READABILITY_SV_SIMPLE" : "LIX_READABILITY_SV_DIFFICULT";
  }

  @Override
  public String getDescription() {
    return tooEasyTest ? "Läsbarhet (LIX): texten är för lättläst" : "Läsbarhet (LIX): texten är för svårläst";
  }

  public String getConfigureText() {
    return "Svårighetsgrad 0 (mycket lättläst) till 4 (mycket svårläst):";
  }

  @Override
  public RuleOption[] getRuleOptions() {
    return new RuleOption[]{ new RuleOption(2, getConfigureText(), 0, 4) };
  }

  private String levelName(int level) {
    switch (level) {
      case 0: return "mycket lättläst";
      case 1: return "lättläst";
      case 2: return "medelsvår";
      case 3: return "svår";
      default: return "mycket svår";
    }
  }

  private int getReadabilityLevel(double lix) {
    if (lix < 30) {
      return 0;
    } else if (lix < 40) {
      return 1;
    } else if (lix < 50) {
      return 2;
    } else if (lix < 60) {
      return 3;
    } else {
      return 4;
    }
  }

  private boolean isLongWord(String token) {
    return token.length() > 6;
  }

  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    nAllSentences = 0;
    nAllWords = 0;
    nAllLongWords = 0;
    int nSentences = 0;
    int nWords = 0;
    int nLongWords = 0;
    int pos = 0;
    int startPos = -1;
    int endPos = -1;
    int nParagraph = 0;
    for (int n = 0; n < sentences.size(); n++) {
      AnalyzedSentence sentence = sentences.get(n);
      AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
      if (startPos < 0 && tokens.length > 1) {
        startPos = pos + tokens[1].getStartPos();
      }
      if (endPos < 0 && tokens.length > MARK_WORDS) {
        endPos = pos + tokens[MARK_WORDS].getEndPos();
      }
      nSentences++;
      for (AnalyzedTokenReadings token : tokens) {
        if (!token.isWhitespace() && !token.isNonWord()) {
          nWords++;
          if (isLongWord(token.getToken())) {
            nLongWords++;
          }
        }
      }
      if (Tools.isParagraphEnd(sentences, n, lang) && nWords >= MIN_WORDS) {
        double lix = (double) nWords / (double) nSentences + 100.0 * nLongWords / nWords;
        int rLevel = getReadabilityLevel(lix);
        if ((tooEasyTest && rLevel < level) || (!tooEasyTest && rLevel > level)) {
          String simple = tooEasyTest ? "lättläst" : "svårläst";
          String msg = "Läsbarhet (LIX): Texten i detta stycke är för " + simple
                  + " {LIX " + (int) lix + ": " + levelName(rLevel) + "}.";
          RuleMatch ruleMatch = new RuleMatch(this, sentence, startPos, endPos, msg);
          ruleMatches.add(ruleMatch);
        }
        nAllSentences += nSentences;
        nAllWords += nWords;
        nAllLongWords += nLongWords;
        nSentences = 0;
        nWords = 0;
        nLongWords = 0;
        startPos = -1;
        endPos = -1;
        nParagraph++;
      }
      pos += sentence.getCorrectedTextLength();
    }
    if (nParagraph <= 1) {
      return toRuleMatchArray(new ArrayList<>());
    }
    return toRuleMatchArray(ruleMatches);
  }

  public int getAllSentences() {
    return nAllSentences;
  }

  public int getAllWords() {
    return nAllWords;
  }

  public int getAllLongWords() {
    return nAllLongWords;
  }

  @Override
  public int minToCheckParagraph() {
    return -1;
  }

}
