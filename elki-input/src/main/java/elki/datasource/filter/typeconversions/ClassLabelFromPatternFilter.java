/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.datasource.filter.typeconversions;

import java.util.regex.Pattern;

import elki.data.LabelList;
import elki.data.SimpleClassLabel;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.datasource.bundle.BundleMeta;
import elki.datasource.filter.AbstractStreamFilter;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;
import elki.utilities.optionhandling.parameters.StringParameter;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Streaming filter to derive an outlier class label.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
public class ClassLabelFromPatternFilter extends AbstractStreamFilter {
  /**
   * Current meta data
   */
  BundleMeta meta = null;

  /**
   * Bitset of label columns
   */
  IntArrayList labelcols = new IntArrayList();

  /**
   * Label to return for positive matches.
   */
  SimpleClassLabel positive;

  /**
   * Label to return for negative matches.
   */
  SimpleClassLabel negative;

  /**
   * Matching pattern.
   */
  Pattern pattern;

  /**
   * Constructor.
   *
   * @param pattern Pattern for matching
   * @param positive Positive label
   * @param negative Negative label
   */
  public ClassLabelFromPatternFilter(Pattern pattern, String positive, String negative) {
    super();
    this.pattern = pattern;
    this.positive = new SimpleClassLabel(positive);
    this.negative = new SimpleClassLabel(negative);
  }

  /**
   * Constructor.
   *
   * @param pattern Pattern for matching
   * @param positive Positive label
   * @param negative Negative label
   */
  public ClassLabelFromPatternFilter(Pattern pattern, SimpleClassLabel positive, SimpleClassLabel negative) {
    super();
    this.pattern = pattern;
    this.positive = positive;
    this.negative = negative;
  }

  @Override
  public BundleMeta getMeta() {
    if(meta == null) {
      // Rebuild metadata.
      BundleMeta origmeta = source.getMeta();
      meta = new BundleMeta(origmeta.size() + 1);
      meta.add(TypeUtil.SIMPLE_CLASSLABEL);
      labelcols.clear();
      for(int i = 0; i < origmeta.size(); i++) {
        final SimpleTypeInformation<?> orig = origmeta.get(i);
        if(TypeUtil.GUESSED_LABEL.isAssignableFromType(orig)) {
          labelcols.add(i);
        }
        meta.add(orig);
      }
    }
    return meta;
  }

  @Override
  public Object data(int rnum) {
    if(rnum > 0) {
      return source.data(rnum - 1);
    }
    if(meta == null) {
      getMeta(); // Trigger build
    }
    for(int i = 0; i < labelcols.size(); i++) {
      Object o = source.data(labelcols.getInt(i));
      if(o == null) {
        continue;
      }
      if(o instanceof LabelList) {
        final LabelList ll = (LabelList) o;
        for(int j = 0; j < ll.size(); j++) {
          if(pattern.matcher(ll.get(j)).find()) {
            return positive;
          }
        }
        continue;
      }
      if(pattern.matcher(o.toString()).find()) {
        return positive;
      }
    }
    return negative;
  }

  @Override
  public Event nextEvent() {
    final Event ev = source.nextEvent();
    if(Event.META_CHANGED.equals(ev)) {
      meta = null;
    }
    return ev;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Pattern for recognizing positive objects.
     */
    public static final OptionID PATTERN_ID = new OptionID("class.pattern", "Regular expression to identify positive objects.");

    /**
     * Class label to assign to positive instances.
     */
    public static final OptionID POSITIVE_ID = new OptionID("class.positive", "Class label to use for positive instances.");

    /**
     * Class label to assign to negative instances.
     */
    public static final OptionID NEGATIVE_ID = new OptionID("class.negative", "Class label to use for negative instances.");

    /**
     * Matching pattern.
     */
    Pattern pattern;

    /**
     * Names for positive and negative classes.
     */
    String positive, negative;

    @Override
    public void configure(Parameterization config) {
      new PatternParameter(PATTERN_ID) //
          .grab(config, x -> pattern = x);
      new StringParameter(POSITIVE_ID, "positive") //
          .grab(config, x -> positive = x);
      new StringParameter(NEGATIVE_ID, "negative") //
          .grab(config, x -> negative = x);
    }

    @Override
    public ClassLabelFromPatternFilter make() {
      return new ClassLabelFromPatternFilter(pattern, positive, negative);
    }
  }
}
