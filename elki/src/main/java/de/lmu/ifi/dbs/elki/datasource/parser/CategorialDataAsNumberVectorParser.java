package de.lmu.ifi.dbs.elki.datasource.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * A very simple parser for categorial data, which will then be encoded as
 * numbers. This is closely modeled after the number vector parser.
 *
 * TODO: specify handling for numerical values.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.landmark
 * @apiviz.has NumberVector
 *
 * @param <V> the type of NumberVector used
 */
@Description("This parser expects data in roughly the same format as the NumberVectorLabelParser,\n"//
+ "except that it will enumerate all unique strings to always produce numerical values.\n"//
+ "This way, it can for example handle files that contain lines like 'y,n,y,y,n,y,n'.")
public class CategorialDataAsNumberVectorParser<V extends NumberVector> extends NumberVectorLabelParser<V> {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(CategorialDataAsNumberVectorParser.class);

  /**
   * For String unification.
   */
  TObjectIntHashMap<String> unique = new TObjectIntHashMap<>();

  /**
   * Base for enumerating unique values.
   */
  int ustart = Math.max(unique.getNoEntryValue() + 1, 1);

  /**
   * Pattern for NaN values.
   */
  Matcher nanpattern = Pattern.compile("\\?").matcher("Dummy text");

  /**
   * Constructor with defaults.
   *
   * @param factory Vector factory
   */
  public CategorialDataAsNumberVectorParser(NumberVector.Factory<V> factory) {
    this(CSVReaderFormat.DEFAULT_FORMAT, null, factory);
  }

  /**
   * Constructor.
   *
   * @param format Input format
   * @param labelIndices Column indexes that are numeric.
   * @param factory Vector factory
   */
  public CategorialDataAsNumberVectorParser(CSVReaderFormat format, long[] labelIndices, NumberVector.Factory<V> factory) {
    super(format, labelIndices, factory);
  }

  @Override
  public Event nextEvent() {
    Event e = super.nextEvent();
    if(e == Event.END_OF_STREAM) {
      unique.clear();
    }
    return e;
  }

  @Override
  protected boolean parseLineInternal() {
    int i = 0;
    for(/* Initialized by nextLineExceptComments */; tokenizer.valid(); tokenizer.advance(), i++) {
      if(!isLabelColumn(i)) {
        try {
          attributes.add(tokenizer.getDouble());
          continue;
        }
        catch(NumberFormatException e) {
          String s = tokenizer.getSubstring();
          if(nanpattern.reset(s).matches()) {
            attributes.add(Double.NaN);
            continue;
          }
          int id = unique.get(s);
          if(id == unique.getNoEntryValue()) {
            id = ustart + unique.size();
            unique.put(s, id);
          }
          attributes.add(id);
          continue;
        }
      }
      // Else: labels.
      haslabels = true;
      labels.add(tokenizer.getSubstring());
    }
    // Pass outside via class variables
    curvec = createVector();
    curlbl = LabelList.make(labels);
    attributes.clear();
    labels.clear();
    return true;
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
  public static class Parameterizer<V extends NumberVector> extends NumberVectorLabelParser.Parameterizer<V> {
    @Override
    protected CategorialDataAsNumberVectorParser<V> makeInstance() {
      return new CategorialDataAsNumberVectorParser<>(format, labelIndices, factory);
    }
  }
}
