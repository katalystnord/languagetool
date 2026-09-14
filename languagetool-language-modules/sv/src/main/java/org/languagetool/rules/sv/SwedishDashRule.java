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

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import org.languagetool.rules.AbstractDashRule;
import org.languagetool.rules.Example;

import java.util.ResourceBundle;

/**
 * Checks for Swedish compounds written with an en/em dash instead of a hyphen
 * (e.g. "e–post" instead of "e-post"), reusing the same {@code compounds.txt}
 * data already built for {@link CompoundRule}. Same mechanism as
 * {@code EnglishDashRule}/{@code GermanDashRule}.
 */
public class SwedishDashRule extends AbstractDashRule {

  private static volatile AhoCorasickDoubleArrayTrie<String> trie;

  public SwedishDashRule(ResourceBundle messages) {
    super(messages);
    addExamplePair(Example.wrong("Skicka ett <marker>e–post</marker> till mig."),
                   Example.fixed("Skicka ett <marker>e-post</marker> till mig."));
  }

  @Override
  public String getId() {
    return "SWEDISH_DASH_RULE";
  }

  @Override
  public String getDescription() {
    return "Kontrollerar om sammansatta ord felaktigt stavats med tankstreck i stället för bindestreck (t.ex. \"e – post\" i stället för \"e-post\").";
  }

  @Override
  public String getMessage() {
    return "Ett tankstreck användes i stället för ett bindestreck.";
  }

  @Override
  protected AhoCorasickDoubleArrayTrie<String> getCompoundsData() {
    AhoCorasickDoubleArrayTrie<String> data = trie;
    if (data == null) {
      synchronized (SwedishDashRule.class) {
        data = trie;
        if (data == null) {
          trie = data = loadCompoundFile("/sv/compounds.txt");
        }
      }
    }
    return data;
  }
}
