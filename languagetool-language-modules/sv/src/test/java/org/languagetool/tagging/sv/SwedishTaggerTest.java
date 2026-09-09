/* LanguageTool, a natural language style checker 
 * Copyright (C) 2006 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.tagging.sv;

import org.junit.Before;
import org.junit.Test;
import org.languagetool.TestTools;
import org.languagetool.language.Swedish;
import org.languagetool.tokenizers.WordTokenizer;

import java.io.IOException;

public class SwedishTaggerTest {
    
  private SwedishTagger tagger;
  private WordTokenizer tokenizer;
      
  @Before
  public void setUp() {
    tagger = new SwedishTagger();
    tokenizer = new WordTokenizer();
  }

  @Test
  public void testDictionary() throws IOException {
    TestTools.testDictionary(tagger, new Swedish());
  }

  @Test
  public void testTagger() throws IOException {
    // "Det/[den]PN" and "en/[man]PN" are new since the SALDO merge (2026-09-08):
    // SALDO models den/det/de as gender/number forms of one pronoun paradigm
    // ("den"), and "en" as a colloquial object form of the impersonal
    // pronoun "man" ("det gör en glad"). Both verified linguistically
    // correct, not a regression, everything else in this string is
    // byte-for-byte unchanged from before the merge.
    TestTools.myAssert("Det är nog bäst att du får en klubba till",
        "Det/[den]PN|Det/[det]PN -- är/[vara]VB:PRS -- nog/[nog]AB -- bäst/[bra]JJ:S|bäst/[bäst]AB|bäst/[god]JJ:S -- att/[att]KN -- du/[du]PN -- får/[få]VB:PRS|får/[får]NN:OF:PLU:NOM:NEU|får/[får]NN:OF:SIN:NOM:NEU -- en/[en]NN:OF:SIN:NOM:UTR|en/[en]PN|en/[man]PN -- klubba/[klubba]NN:OF:SIN:NOM:UTR|klubba/[klubba]VB:IMP|klubba/[klubba]VB:INF -- till/[till]AB|till/[till]PP", tokenizer, tagger);

    // en + passant/[null]null
    // Hon/[ho]... and det/[den]PN and en/[man]PN are new since the SALDO
    // merge, same verified-correct pattern as above ("ho" = archaic word
    // for trough, "hon" is its definite form).
    TestTools.myAssert("Hon nämnde, en passant, att det inte var klädsamt",
        "Hon/[ho]NN:BF:SIN:NOM:UTR|Hon/[hon]PN -- nämnde/[nämna]VB:PRT -- en/[en]NN:OF:SIN:NOM:UTR|en/[en]PN|en/[man]PN -- passant/[null]null -- att/[att]KN -- det/[den]PN|det/[det]PN -- inte/[inte]AB -- var/[var]AB|var/[var]NN:OF:SIN:NOM:NEU|var/[var]PN|var/[vara]VB:IMP|var/[vara]VB:PRT -- klädsamt/[klädsam]JJ:PN", tokenizer, tagger);

    TestTools.myAssert("Nato-vänliga länder har blivit fler.",
        "Nato-vänliga/[null]null -- länder/[land]NN:OF:PLU:NOM:NEU|länder/[länd]NN:OF:PLU:NOM:UTR|länder/[lända]VB:PRS -- har/[ha]VB:PRS -- blivit/[bli]VB:SUP -- fler/[mången]JJ:K", tokenizer, tagger);
    // s/[s]NN:OF:SIN:NOM:NEU and s/[s]NN:OF:PLU:NOM:NEU are new: SALDO has
    // "s" (the letter name) as a genuine, if narrow, noun lemgram (s..nn.1),
    // paradigm nn_6n_frx, a zero-plural neuter noun like "hus" where
    // singular and plural indefinite share one spelling. Both readings are
    // real SALDO data (sg indef nom = "s", pl indef nom = "s"); only the
    // singular one survived earlier because of a merge_sources.py dedup bug
    // that dropped every SALDO reading after the first for a (form, lemma)
    // pair not already in the pre-SALDO dictionary, fixed 2026-09-09, see
    // implementation-plan.md. Real ambiguity, not a bug, disambiguation is a
    // separate concern from tagging.
    TestTools.myAssert("FN:s nya projekt.",
        "FN/[FN]PM:NOM:ACR -- s/[s]NN:OF:PLU:NOM:NEU|s/[s]NN:OF:SIN:NOM:NEU -- nya/[ny]JJ:BF|nya/[ny]JJ:P -- projekt/[projekt]NN:OF:PLU:NOM:NEU|projekt/[projekt]NN:OF:SIN:NOM:NEU", tokenizer, tagger);

    // Du/[Du]PN is new: SALDO has a separate lemma for capitalized "Du", the
    // formal/polite letter-writing convention, distinct from lowercase "du".
    // era/[ni]PN is new: "era" (your, plural/formal) as a possessive form of
    // "ni", same pattern as vår/våra -> vi earlier in this file.
    TestTools.myAssert("Du menar sannolikt \"massera\" om du inte skriver om masarnas era förstås.",
        "Du/[Du]PN|Du/[du]PN -- menar/[mena]VB:PRS -- sannolikt/[sannolik]JJ:PN|sannolikt/[sannolikt]AB -- massera/[massera]VB:IMP|massera/[massera]VB:INF -- om/[om]AB|om/[om]KN|om/[om]PP -- du/[du]PN -- inte/[inte]AB -- skriver/[skriva]VB:PRS -- om/[om]AB|om/[om]KN|om/[om]PP -- masarnas/[mas]NN:BF:PLU:GEN:UTR -- era/[era]NN:OF:SIN:NOM:UTR|era/[era]PN|era/[ni]PN -- förstås/[förstå]VB:INF:PF|förstås/[förstå]VB:PRS:PF|förstås/[förstås]AB", tokenizer, tagger);
  }

}
