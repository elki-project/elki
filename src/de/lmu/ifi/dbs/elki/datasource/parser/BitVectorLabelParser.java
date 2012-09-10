package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.Bit;
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
   * @param colSep
   * @param quoteChar
   */
  public BitVectorLabelParser(Pattern colSep, char quoteChar) {
    super(colSep, quoteChar);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<BitVector> vectors = new ArrayList<BitVector>();
    List<LabelList> labels = new ArrayList<LabelList>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          List<String> entries = tokenize(line);
          // FIXME: use more efficient storage right away?
          List<Bit> attributes = new ArrayList<Bit>();
          LabelList ll = null;
          for(String entry : entries) {
            try {
              Bit attribute = Bit.valueOf(entry);
              attributes.add(attribute);
            }
            catch(NumberFormatException e) {
              if(ll == null) {
                ll = new LabelList(1);
              }
              ll.add(entry);
            }
          }

          if(dimensionality < 0) {
            dimensionality = attributes.size();
          }
          else if(dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ".");
          }

          vectors.add(new BitVector(attributes.toArray(new Bit[attributes.size()])));
          labels.add(ll);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    return MultipleObjectsBundle.makeSimple(getTypeInformation(dimensionality), vectors, TypeUtil.LABELLIST, labels);
  }

  protected VectorFieldTypeInformation<BitVector> getTypeInformation(int dimensionality) {
    return new VectorFieldTypeInformation<BitVector>(BitVector.FACTORY, dimensionality);
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
      return new BitVectorLabelParser(colSep, quoteChar);
    }
  }
}