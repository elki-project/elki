package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Provides a parser for parsing one BitVector per line, bits separated by
 * whitespace.
 * <p/>
 * Several labels may be given per BitVector. A label must not be parseable as
 * Bit. Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has BitVector
 */
@Title("Bit Vector Label Parser")
@Description("Parses the following format of lines:\n" + "A single line provides a single BitVector. Bits are separated by whitespace. Any substring not containing whitespace is tried to be read as Bit. If this fails, it will be appended to a label. (Thus, any label must not be parseable as Bit.) Empty lines and lines beginning with \"#\" will be ignored. If any BitVector differs in its dimensionality from other BitVectors, the parse method will fail with an Exception.")
public class BitVectorLabelParser extends AbstractParser implements Parser {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(BitVectorLabelParser.class);

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChars Quotation character
   * @param comment Comment pattern
   */
  public BitVectorLabelParser(Pattern colSep, String quoteChars, Pattern comment) {
    super(colSep, quoteChars, comment);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<BitVector> vectors = new ArrayList<>();
    List<LabelList> labels = new ArrayList<>();
    ArrayList<String> ll = new ArrayList<>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        // Skip empty lines and comments
        if(line.length() <= 0 || (comment != null && comment.matcher(line).matches())) {
          continue;
        }
        BitSet bitSet = new BitSet();
        ll.clear();
        int i = 0;
        for(tokenizer.initialize(line, 0, lengthWithoutLinefeed(line)); tokenizer.valid(); tokenizer.advance()) {
          try {
            if(tokenizer.getLongBase10() > 0) {
              bitSet.set(i);
            }
            ++i;
          }
          catch(NumberFormatException e) {
            ll.add(tokenizer.getSubstring());
          }
        }

        if(dimensionality < 0) {
          dimensionality = i;
        }
        else if(dimensionality != i) {
          throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ".");
        }

        vectors.add(new BitVector(bitSet, dimensionality));
        labels.add(LabelList.make(ll));
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    return MultipleObjectsBundle.makeSimple(getTypeInformation(dimensionality), vectors, TypeUtil.LABELLIST, labels);
  }

  protected VectorFieldTypeInformation<BitVector> getTypeInformation(int dimensionality) {
    return new VectorFieldTypeInformation<>(BitVector.FACTORY, dimensionality);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParser.Parameterizer {
    @Override
    protected BitVectorLabelParser makeInstance() {
      return new BitVectorLabelParser(colSep, quoteChars, comment);
    }
  }
}
