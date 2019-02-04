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
package de.lmu.ifi.dbs.elki.data;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;

/**
 * A HierarchicalClassLabel is a ClassLabel to reflect a hierarchical structure
 * of classes.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @composed - - - Comparable
 */
public class HierarchicalClassLabel extends ClassLabel {
  /**
   * The default separator pattern, a point ('.').
   */
  public static final Pattern DEFAULT_SEPARATOR = Pattern.compile("\\.");

  /**
   * The default separator, a point ('.').
   */
  public static final String DEFAULT_SEPARATOR_STRING = ".";

  /**
   * Type information.
   */
  public static final SimpleTypeInformation<HierarchicalClassLabel> TYPE = new SimpleTypeInformation<>(HierarchicalClassLabel.class);

  /**
   * Holds the Pattern to separate different levels parsing input.
   */
  private Pattern separatorPattern;

  /**
   * A String to separate different levels in a String representation of this
   * HierarchicalClassLabel.
   */
  private String separatorString;

  /**
   * Holds the names on the different levels.
   */
  private Comparable<?>[] levelwiseNames;

  /**
   * Constructs a hierarchical class label from the given name, using the given
   * Pattern to match separators of different levels in the given name, and
   * setting the given separator-String to separate different levels in String
   * representations of this HierarchicalClassLabel.
   * 
   * @param name a String describing a hierarchical class label
   * @param regex a Pattern to match separators of different levels in the given
   *        name
   * @param separator a separator String to separate different levels in the
   *        String-representation of this HierarchicalClassLabel
   */
  public HierarchicalClassLabel(String name, Pattern regex, String separator) {
    super();
    this.separatorPattern = regex;
    this.separatorString = separator;
    String[] levelwiseStrings = separatorPattern.split(name);
    this.levelwiseNames = new Comparable<?>[levelwiseStrings.length];
    for (int i = 0; i < levelwiseStrings.length; i++) {
      try {
        levelwiseNames[i] = Integer.valueOf(levelwiseStrings[i]);
      } catch (NumberFormatException e) {
        levelwiseNames[i] = levelwiseStrings[i];
      }
    }
  }

  /**
   * Constructs a hierarchical class label from the given name. Different levels
   * are supposed to be separated by points ('.'), as defined by
   * {@link #DEFAULT_SEPARATOR DEFAULT_SEPARATOR}. Also, in a
   * String-representation of this HierarchicalClassLabel, different levels get
   * separated by '.'.
   * 
   * @param label a String describing a hierarchical class label
   */
  public HierarchicalClassLabel(String label) {
    this(label, DEFAULT_SEPARATOR, DEFAULT_SEPARATOR_STRING);
  }

  /**
   * Compares two HierarchicalClassLabels. Names at higher levels are compared
   * first. Names at a lower level are compared only if their parent-names are
   * equal. Names at a level are tried to be compared as integer values. If this
   * does not succeed, both names are compared as Strings.
   * 
   * {@inheritDoc}
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public int compareTo(ClassLabel o) {
    HierarchicalClassLabel h = (HierarchicalClassLabel) o;
    for (int i = 0; i < this.levelwiseNames.length && i < h.levelwiseNames.length; i++) {
      int comp = 0;
      try {
        Comparable first = this.levelwiseNames[i];
        Comparable second = h.levelwiseNames[i];
        comp = first.compareTo(second);
      } catch (RuntimeException e) {
        String h1 = (String) (this.levelwiseNames[i] instanceof Integer ? this.levelwiseNames[i].toString() : this.levelwiseNames[i]);
        String h2 = (String) (h.levelwiseNames[i] instanceof Integer ? h.levelwiseNames[i].toString() : h.levelwiseNames[i]);
        comp = h1.compareTo(h2);
      }
      if (comp != 0) {
        return comp;
      }
    }
    return (this.levelwiseNames.length < h.levelwiseNames.length) ? -1 : ((this.levelwiseNames.length == h.levelwiseNames.length) ? 0 : 1);
  }

  /**
   * The length of the hierarchy of names.
   * 
   * @return length of the hierarchy of names
   */
  public int depth() {
    return levelwiseNames.length - 1;
  }

  /**
   * Returns the name at the given level as a String.
   * 
   * @param level the level to return the name at
   * @return the name at the given level as a String
   */
  public String getNameAt(int level) {
    return this.levelwiseNames[level] instanceof Integer ? this.levelwiseNames[level].toString() : (String) this.levelwiseNames[level];
  }

  /**
   * Returns a String representation of this HierarchicalClassLabel using
   * {@link #separatorString separatorString} to separate levels.
   * 
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return toString(levelwiseNames.length);
  }

  /**
   * Create a String representation of this ClassLabel comprising only the
   * first <code>level</code> levels.
   * 
   * @param level the lowest level to include in the String representation.
   * @return a String representation of this ClassLabel comprising only the
   *         first <code>level</code> levels
   */
  public String toString(int level) {
    if (level > levelwiseNames.length) {
      throw new IllegalArgumentException("Specified level exceeds depth of hierarchy.");
    }

    StringBuilder name = new StringBuilder();
    for (int i = 0; i < level; i++) {
      name.append(this.getNameAt(i));
      if (i < level - 1) {
        name.append(this.separatorString);
      }
    }
    return name.toString();
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @has - creates - HierarchicalClassLabel
   * @stereotype factory
   */
  public static class Factory extends ClassLabel.Factory<HierarchicalClassLabel> {
    @Override
    public HierarchicalClassLabel makeFromString(String lbl) {
      lbl = lbl.intern();
      HierarchicalClassLabel l = existing.get(lbl);
      if (l == null) {
        l = new HierarchicalClassLabel(lbl);
        existing.put(lbl, l);
      }
      return l;
    }

    @Override
    public SimpleTypeInformation<? super HierarchicalClassLabel> getTypeInformation() {
      return TYPE;
    }
  }
}
