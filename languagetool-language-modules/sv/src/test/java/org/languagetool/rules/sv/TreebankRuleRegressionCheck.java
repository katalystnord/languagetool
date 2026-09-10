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
import java.util.*;
import java.util.stream.Stream;

/**
 * Not a JUnit test (no {@code @Test}, deliberately not run by {@code mvn test}):
 * a manually-invoked regression tool for grammar.xml rules, run against the full
 * text of real, correct Swedish UD treebank corpora (Talbanken + LinES) rather
 * than the handful of examples any one rule ships with.
 *
 * A new pattern rule can pass its own {@code <example>} sentences (checked by
 * {@link SwedishPatternRuleTest}) while still firing on correct real-world text
 * the author never thought to try; that's exactly the false-positive class this
 * catches. Every prior rule of this kind (DEN.JJ.NEUTRAL_n-ord.NN,
 * DET.JJ.UTRUM_t-ord.NN, DENNA.NEUTRAL_n-ord.NN, DETTA.UTRUM_t-ord.NN) was
 * checked this way before being accepted; this class replaces what was
 * previously an ad-hoc, uncommitted script with something reusable.
 *
 * Usage: point {@code -Dtreebank.dir=...} at a directory containing one or more
 * subdirectories of {@code .conllu} files (defaults to
 * {@code ../swedish-pos-dict/data/treebanks} relative to this repo, matching
 * this project's convention of checking out languagetool, swedish-pos-dict,
 * and the notes repo as sibling directories), then run e.g.:
 *
 * <pre>
 * mvn -pl languagetool-language-modules/sv exec:java \
 *   -Dexec.mainClass=org.languagetool.rules.sv.TreebankRuleRegressionCheck \
 *   -Dexec.classpathScope=test
 * </pre>
 *
 * or simply compile the test sources and run this class's main() directly with
 * the resulting classpath. Prints, per rule ID of interest, every sentence (if
 * any) where that rule fired; a rule with zero hits here has passed the check.
 */
public final class TreebankRuleRegressionCheck {

  private TreebankRuleRegressionCheck() {
  }

  public static void main(String[] args) throws IOException {
    // Rule IDs this check cares about. Extend this list as new mined rules are
    // added; a rule not listed here still gets exercised (checkInternal() runs
    // every active rule), it just won't get its own reported hit count.
    Set<String> watchedRuleIds = new LinkedHashSet<>(Arrays.asList(
      "DEN.JJ.NEUTRAL_n-ord.NN",
      "DET.JJ.UTRUM_t-ord.NN",
      "DENNA.NEUTRAL_n-ord.NN",
      "DETTA.UTRUM_t-ord.NN"
    ));

    Path treebankDir = Paths.get(System.getProperty("treebank.dir",
      "../swedish-pos-dict/data/treebanks"));
    if (!Files.isDirectory(treebankDir)) {
      throw new IllegalStateException("Treebank directory not found: " + treebankDir.toAbsolutePath()
        + " (pass -Dtreebank.dir=/path/to/treebanks, containing subdirectories of .conllu files)");
    }

    List<String> sentences = extractSentences(treebankDir);
    System.out.println("Loaded " + sentences.size() + " sentences from " + treebankDir);

    Language swedish = Languages.getLanguageForShortCode("sv");
    JLanguageTool lt = new JLanguageTool(swedish);

    Map<String, List<String>> hitsByRule = new LinkedHashMap<>();
    for (String ruleId : watchedRuleIds) {
      hitsByRule.put(ruleId, new ArrayList<>());
    }

    int checked = 0;
    for (String sentence : sentences) {
      List<RuleMatch> matches;
      try {
        matches = lt.check(sentence);
      } catch (Exception e) {
        // A handful of treebank sentences are fragments or contain characters
        // the tokenizer/tagger chokes on; skip and keep going rather than
        // abort the whole run over one bad sentence.
        continue;
      }
      checked++;
      for (RuleMatch match : matches) {
        String id = match.getRule().getId();
        if (hitsByRule.containsKey(id)) {
          hitsByRule.get(id).add(sentence);
        }
      }
    }

    System.out.println("Checked " + checked + " sentences against all active Swedish rules.");
    System.out.println();
    boolean anyFailures = false;
    for (Map.Entry<String, List<String>> entry : hitsByRule.entrySet()) {
      List<String> hits = entry.getValue();
      if (hits.isEmpty()) {
        System.out.println(entry.getKey() + ": 0 false positives, PASS");
      } else {
        anyFailures = true;
        System.out.println(entry.getKey() + ": " + hits.size() + " false positive(s), FAIL");
        for (String s : hits) {
          System.out.println("  - " + s);
        }
      }
    }
    if (anyFailures) {
      System.exit(1);
    }
  }

  /**
   * Pulls sentence text straight from each .conllu file's "# text = " comment
   * lines. That's the treebank's own detokenized rendering, so it's used as-is
   * rather than reassembled from individual token/SpaceAfter columns.
   */
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
