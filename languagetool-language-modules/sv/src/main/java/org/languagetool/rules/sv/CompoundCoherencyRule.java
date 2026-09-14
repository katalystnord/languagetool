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
import org.jetbrains.annotations.Nullable;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.rules.*;

import java.io.IOException;
import java.util.*;

/**
 * Check consistent hyphenation for Swedish compounds within one document.
 * It's a style issue if a compound is written once with and once without a
 * hyphen in the same text, e.g. "e-post" and "epost", or "sms-lån" and
 * "smslån". Port of the German {@code CompoundCoherencyRule}: same
 * mechanism (normalize each token's lemma by stripping hyphens, flag a
 * normalized form that was seen with a different surface spelling
 * elsewhere in the document), no language-specific logic needed since the
 * check is about hyphen placement, not grammar.
 *
 * Deliberately narrower than Swedish's much more common "särskrivning"
 * problem (a compound wrongly split across two separate, space-separated
 * tokens, e.g. "sjuk sköterska" for "sjuksköterska"): that needs comparing
 * a two-token sequence against a one-token compound elsewhere in the
 * document, a different comparison than this rule's single-token
 * hyphen-normalization, and was investigated and rejected as a
 * single-sentence POS-pattern rule (see implementation-plan.md and
 * roadmap.md's split-compound entries) precisely because it can't be
 * checked reliably within one sentence. A document-level, cross-reference
 * version of that check (this rule's mechanism, extended to also compare
 * multi-token sequences) is a plausible future extension, not attempted
 * here.
 *
 * Known limitation, not a false-positive risk: when a compound isn't in
 * the dictionary at all (common for productive technical/loan compounds
 * like "sms-lån"), its lemma is null and the code falls back to the raw
 * surface token for normalization, which still carries inflection. Two
 * occurrences in different grammatical forms of the same unknown
 * compound (e.g. "sms-lån" and "smslånet") then normalize to different
 * strings and aren't cross-referenced at all, a missed detection rather
 * than a wrong one. Same limitation the German original has; not
 * language-specific.
 *
 * Two real false-positive classes found running this against ~25,000
 * real sentences (Talbanken+LinES plus a Wikipedia/c4 sample), both
 * accepted as a known tradeoff of this mechanism rather than fixed
 * (matching the same style-category, same-tradeoff status the German
 * original already has upstream): (1) a hyphen used as phonetic/stress
 * notation rather than real orthography, e.g. a pronunciation gloss
 * like "(uttalas abbekå-s)" for "Abbekås", or "'anden' med stöt
 * 'and-en'" explicitly discussing pronunciation of "anden" - no POS or
 * lemma signal distinguishes this from genuine inconsistent spelling;
 * (2) common, reusable descriptive compounds like "svartvit"/"svart-vit"
 * (both individually standard spellings) can legitimately describe two
 * different, unrelated things in the same document, unlike a specific
 * named entity or technical term, where two occurrences are far more
 * likely to actually be the same thing spelled two ways.
 */
public class CompoundCoherencyRule extends TextLevelRule {

  public CompoundCoherencyRule(ResourceBundle messages) {
    super.setCategory(Categories.STYLE.getCategory(messages));
    addExamplePair(Example.wrong("Det finns bara en brevlåda för e-post. Kontrollera din <marker>epost</marker> ofta."),
                   Example.fixed("Det finns bara en brevlåda för e-post. Kontrollera din <marker>e-post</marker> ofta."));
  }

  @Override
  public String getId() {
    return "SV_COMPOUND_COHERENCY";
  }

  @Override
  public String getDescription() {
    return "Enhetlig stavning av sammansättningar (med eller utan bindestreck)";
  }

  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    Map<String,List<String>> normToTextOccurrences = new HashMap<>();
    int pos = 0;
    for (AnalyzedSentence sentence : sentences) {
      AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
      for (AnalyzedTokenReadings atr : tokens) {
        String lemmaOrNull = getLemma(atr);
        String token = atr.getToken();
        if (token.isEmpty()) {
          continue;
        }
        // The whole implementation could be simpler, but this way we also catch cases where
        // the word (and thus its lemma) isn't known.
        String lemma = lemmaOrNull != null ? lemmaOrNull : token;
        String normToken = lemma.replace("-", "").toLowerCase();
        if (StringUtils.isNumeric(normToken)) {
          // avoid messages about "2-3" and "23" both being used
          break;
        }
        List<String> textOcc = normToTextOccurrences.get(normToken);
        if (textOcc != null) {
          if (textOcc.stream().noneMatch(f -> f.equalsIgnoreCase(lemma))) {
            String other = textOcc.get(0);
            if (containsHyphenInside(other) || containsHyphenInside(token)) {
              String msg = "Inkonsekvent användning av bindestreck. Texten innehåller både '" + token + "' och '" + other + "'.";
              RuleMatch ruleMatch = new RuleMatch(this, sentence, pos + atr.getStartPos(), pos + atr.getEndPos(), msg);
              if (token.replace("-", "").equalsIgnoreCase(other.replace("-", ""))) {
                // might be different inflected forms, so only suggest if really just the hyphen is different:
                ruleMatch.setSuggestedReplacement(other);
              }
              ruleMatches.add(ruleMatch);
            }
          }
        } else {
          List<String> l = new ArrayList<>();
          l.add(lemma);
          normToTextOccurrences.putIfAbsent(normToken, l);
        }
      }
      pos += sentence.getCorrectedTextLength();
    }
    return toRuleMatchArray(ruleMatches);
  }

  private boolean containsHyphenInside(String token) {
    return token.contains("-") && !token.startsWith("-") && !token.endsWith("-");
  }

  @Nullable
  private String getLemma(AnalyzedTokenReadings atr) {
    String lemmaOrNull = atr.hasSameLemmas() && atr.getReadingsLength() > 0 ? atr.getReadings().get(0).getLemma() : null;
    if (lemmaOrNull != null) {
      // our analysis may get a lemma without the hyphen for a hyphenated token
      // (e.g. "Jugendfoto" for "Jugend-Fotos" in German); 'fix' that here
      String token = atr.getToken();
      if (!lemmaOrNull.contains("-") && token.contains("-")) {
        StringBuilder lemmaBuilder = new StringBuilder();
        for (int lemmaPos = 0, tokenPos = 0; lemmaPos < lemmaOrNull.length(); lemmaPos++, tokenPos++) {
          if (tokenPos >= token.length()) {
            break;
          }
          char lemmaChar = lemmaOrNull.charAt(lemmaPos);
          char tokenChar = token.charAt(tokenPos);
          if (lemmaChar == tokenChar) {
            lemmaBuilder.append(lemmaChar);
          } else if (token.charAt(tokenPos) == '-') {
            tokenPos++;  // skip hyphen
            lemmaBuilder.append('-');
            if (lemmaPos + 1 < token.length() && Character.isUpperCase(token.charAt(tokenPos))) {
              lemmaBuilder.append(Character.toUpperCase(lemmaChar));
            } else {
              lemmaBuilder.append(lemmaChar);
            }
          }
        }
        return lemmaBuilder.toString();
      }
      return lemmaOrNull;
    } else {
      return null;
    }
  }

  @Override
  public int minToCheckParagraph() {
    return -1;
  }

}
