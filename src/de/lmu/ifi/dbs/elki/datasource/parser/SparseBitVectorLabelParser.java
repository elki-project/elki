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
 * Provides a parser for parsing one sparse BitVector per line, where the
 * indices of the one-bits are separated by whitespace. The first index starts
 * with zero.
 * <p/>
 * Several labels may be given per BitVector, a label must not be parseable as
 * an Integer. Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has BitVector
 */
@Title("Sparse Bit Vector Label Parser")
@Description("Parser for the lines of the following format:\n" + "A single line provides a single sparse BitVector. The indices of the one-bits are " + "separated by whitespace. The first index starts with zero. Any substring not containing whitespace is tried to be read as an Integer. " + "If this fails, it will be appended to a label. (Thus, any label must not be parseable as an Integer.) " + "Empty lines and lines beginning with \"#\" will be ignored.")
public class SparseBitVectorLabelParser extends AbstractParser implements Parser {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SparseBitVectorLabelParser.class);

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChars Quotation character
   * @param comment Comment pattern
   */
  public SparseBitVectorLabelParser(Pattern colSep, String quoteChars, Pattern comment) {
    super(colSep, quoteChars, comment);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<BitVector> vectors = new ArrayList<>();
    List<LabelList> lblc = new ArrayList<>();
    try {
      List<BitSet> bitSets = new ArrayList<>();
      List<LabelList> allLabels = new ArrayList<>();
      ArrayList<String> labels = new ArrayList<>();
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        // Skip empty lines and comments
        if(line.length() <= 0 || (comment != null && comment.matcher(line).matches())) {
          continue;
        }
        BitSet bitSet = new BitSet();
        labels.clear();

        for(tokenizer.initialize(line, 0, lengthWithoutLinefeed(line)); tokenizer.valid(); tokenizer.advance()) {
          try {
            int index = (int) tokenizer.getLongBase10();
            bitSet.set(index);
            dimensionality = Math.max(dimensionality, index);
          }
          catch(NumberFormatException e) {
            labels.add(tokenizer.getSubstring());
          }
        }

        bitSets.add(bitSet);
        allLabels.add(LabelList.make(labels));
      }

      ++dimensionality;
      for(int i = 0; i < bitSets.size(); i++) {
        vectors.add(new BitVector(bitSets.get(i), dimensionality));
        lblc.add(allLabels.get(i));
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    return MultipleObjectsBundle.makeSimple(getTypeInformation(dimensionality), vectors, TypeUtil.LABELLIST, lblc);
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
    protected SparseBitVectorLabelParser makeInstance() {
      return new SparseBitVectorLabelParser(colSep, quoteChars, comment);
    }
  }
}
