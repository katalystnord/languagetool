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
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * Not a JUnit test: MissingVerbRule ships {@code setDefaultOff()} (same as
 * the German original), so it isn't exercised by {@link TreebankRuleRegressionCheck}'s
 * ordinary active-rules loop. This calls the rule's {@code match()} directly
 * against every sentence in the full Talbanken+LinES corpus to measure its
 * real false-positive rate, the same corpus and the same bar (as close to
 * 0 as possible) every other rule in this project is held to, adapted here
 * for a rule that isn't a grammar.xml pattern.
 *
 * Usage: same as TreebankRuleRegressionCheck, run this class's main() with
 * the compiled test classpath (add -Dtreebank.dir=... to override the
 * default location).
 */
public final class MissingVerbRuleCorpusCheck {

  private MissingVerbRuleCorpusCheck() {
  }

  public static void main(String[] args) throws IOException {
    Path treebankDir = Paths.get(System.getProperty("treebank.dir",
      "../swedish-pos-dict/data/treebanks"));
    if (!Files.isDirectory(treebankDir)) {
      throw new IllegalStateException("Treebank directory not found: " + treebankDir.toAbsolutePath());
    }

    List<String> sentences = extractSentences(treebankDir);
    System.out.println("Loaded " + sentences.size() + " sentences from " + treebankDir);

    Language swedish = Languages.getLanguageForShortCode("sv");
    JLanguageTool lt = new JLanguageTool(swedish);
    MissingVerbRule rule = new MissingVerbRule(ResourceBundle.getBundle("org.languagetool.MessagesBundle"), swedish);

    int checked = 0;
    List<String> hits = new ArrayList<>();
    for (String sentence : sentences) {
      AnalyzedSentence analyzed;
      try {
        analyzed = lt.getAnalyzedSentence(sentence);
      } catch (Exception e) {
        continue;
      }
      checked++;
      RuleMatch[] matches = rule.match(analyzed);
      if (matches.length > 0) {
        hits.add(sentence);
      }
    }

    System.out.println("Checked " + checked + " sentences.");
    System.out.println(rule.getId() + ": " + hits.size() + " false positive(s) out of " + checked
      + " (" + String.format("%.2f", 100.0 * hits.size() / checked) + "%)");
    for (String s : hits) {
      System.out.println("  - " + s);
    }
  }

  private static List<String> extractSentences(Path treebankDir) throws IOException {
    List<String> sentences = new ArrayList<>();
    try (Stream<Path> files = Files.walk(treebankDir)) {
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
