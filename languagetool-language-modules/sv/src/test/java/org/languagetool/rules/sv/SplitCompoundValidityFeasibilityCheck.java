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

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.spelling.hunspell.HunspellRule;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Not a JUnit test: a standalone, throwaway feasibility check (same rationale as the
 * document-level split-compound feasibility script mentioned in implementation-plan.md)
 * for whether adding a Hunspell compound-validity filter (does the concatenation of two
 * adjacent bare-indefinite-singular nouns spellcheck as a real word?) rescues the "bare
 * indefinite noun + indefinite noun" split-compound pattern (roadmap.md KTH taxonomy
 * sub-pattern (e), e.g. "fotogen kök" -> "fotogenkök") that was rejected outright
 * (4705 false positives out of 17,760 sentences) as a pure POS-pattern rule with no such
 * filter.
 */
public final class SplitCompoundValidityFeasibilityCheck {
  private SplitCompoundValidityFeasibilityCheck() {
  }

  public static void main(String[] args) throws IOException {
    Path treebankDir = Paths.get(System.getProperty("treebank.dir", "../swedish-pos-dict/data/treebanks"));
    List<String> sentences = extractSentences(treebankDir);
    System.out.println("Loaded " + sentences.size() + " sentences");

    Language swedish = Languages.getLanguageForShortCode("sv");
    JLanguageTool lt = new JLanguageTool(swedish);
    HunspellRule hunspell = new HunspellRule(JLanguageTool.getMessageBundle(swedish), swedish, null, null);

    int rawHits = 0;
    int validCompoundHits = 0;
    List<String> rawExamples = new ArrayList<>();
    List<String> validExamples = new ArrayList<>();

    for (String sentence : sentences) {
      AnalyzedSentence analyzed;
      try {
        analyzed = lt.getAnalyzedSentence(sentence);
      } catch (Exception e) {
        continue;
      }
      AnalyzedTokenReadings[] tokens = analyzed.getTokensWithoutWhitespace();
      for (int i = 0; i + 1 < tokens.length; i++) {
        if (isBareIndefiniteSingularNoun(tokens[i]) && isBareIndefiniteSingularNoun(tokens[i + 1])) {
          rawHits++;
          String w1 = tokens[i].getToken();
          String w2 = tokens[i + 1].getToken();
          String concat = w1 + w2.toLowerCase();
          if (rawExamples.size() < 15) {
            rawExamples.add(w1 + " " + w2 + "  (in: " + sentence + ")");
          }
          if (!hunspell.isMisspelled(concat)) {
            validCompoundHits++;
            if (validExamples.size() < 40) {
              validExamples.add(w1 + " " + w2 + " -> " + concat + "  (in: " + sentence + ")");
            }
          }
        }
      }
    }

    System.out.println("Raw pattern hits (NN:OF:SIN + NN:OF:SIN, no filter): " + rawHits);
    System.out.println("Hits surviving the Hunspell compound-validity filter: " + validCompoundHits);
    System.out.println();
    System.out.println("=== Sample of RAW hits (before filter) ===");
    for (String s : rawExamples) {
      System.out.println("  " + s);
    }
    System.out.println();
    System.out.println("=== Sample of hits SURVIVING the validity filter ===");
    for (String s : validExamples) {
      System.out.println("  " + s);
    }
  }

  private static boolean isBareIndefiniteSingularNoun(AnalyzedTokenReadings token) {
    if (token.getReadings().isEmpty()) {
      return false;
    }
    return token.getReadings().stream().anyMatch(r ->
            r.getPOSTag() != null && r.getPOSTag().matches("NN:OF:SIN:.*"));
  }

  private static List<String> extractSentences(Path treebankDir) throws IOException {
    List<String> sentences = new ArrayList<>();
    try (var files = Files.walk(treebankDir)) {
      List<Path> conlluFiles = files.filter(p -> p.toString().endsWith(".conllu")).sorted().toList();
      for (Path file : conlluFiles) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          String line;
          while ((line = reader.readLine()) != null) {
            if (line.startsWith("# text =")) {
              sentences.add(line.substring("# text =".length()).trim());
            }
          }
        }
      }
    }
    return sentences;
  }
}
