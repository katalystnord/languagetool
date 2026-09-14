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

import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Ad-hoc, not-run-by-mvn-test document-level check for SWEDISH_WORD_REPEAT_BEGINNING_RULE:
 * groups consecutive treebank sentences (real document order, since .conllu files list
 * sentences in source order) into pseudo-documents and checks how often the rule fires,
 * since TreebankRuleRegressionCheck checks one isolated sentence at a time and can never
 * exercise a cross-sentence TextLevelRule at all.
 */
public final class WordRepeatBeginningDocumentCheck {
  private WordRepeatBeginningDocumentCheck() {
  }

  public static void main(String[] args) throws IOException {
    Path treebankDir = Paths.get(System.getProperty("treebank.dir", "../swedish-pos-dict/data/treebanks"));
    List<String> sentences = extractSentences(treebankDir);
    System.out.println("Loaded " + sentences.size() + " sentences");

    Language swedish = Languages.getLanguageForShortCode("sv");
    JLanguageTool lt = new JLanguageTool(swedish);

    int groupSize = 20;
    int totalHits = 0;
    int groupsChecked = 0;
    List<String> exampleHits = new ArrayList<>();
    for (int i = 0; i < sentences.size(); i += groupSize) {
      StringBuilder doc = new StringBuilder();
      for (int j = i; j < Math.min(i + groupSize, sentences.size()); j++) {
        doc.append(sentences.get(j)).append(" ");
      }
      List<RuleMatch> matches;
      try {
        matches = lt.check(doc.toString());
      } catch (Exception e) {
        continue;
      }
      groupsChecked++;
      for (RuleMatch m : matches) {
        if (m.getRule().getId().equals("SWEDISH_WORD_REPEAT_BEGINNING_RULE")) {
          totalHits++;
          if (exampleHits.size() < 25) {
            int start = Math.max(0, m.getFromPos() - 80);
            int end = Math.min(doc.length(), m.getToPos() + 20);
            exampleHits.add(doc.substring(start, end).replace("\n", " "));
          }
        }
      }
    }
    System.out.println("Checked " + groupsChecked + " pseudo-documents of " + groupSize + " sentences each");
    System.out.println("SWEDISH_WORD_REPEAT_BEGINNING_RULE: " + totalHits + " hits");
    System.out.println();
    for (String ex : exampleHits) {
      System.out.println("  ..." + ex + "...");
      System.out.println();
    }
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
