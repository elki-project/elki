package de.lmu.ifi.dbs.elki.data;

import java.util.regex.Pattern;

/**
 * A HierarchicalClassLabel is a ClassLabel to reflect a hierarchical structure
 * of classes.
 * 
 * @author Arthur Zimek
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
  // TODO: fix generics warnings.
  private Comparable<?>[] levelwiseNames;

  /**
   * @see ClassLabel#ClassLabel()
   */
  public HierarchicalClassLabel() {
    super();
  }

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
  public void init(String name, Pattern regex, String separator) {
    this.separatorPattern = regex;
    this.separatorString = separator;
    String[] levelwiseStrings = separatorPattern.split(name);
    this.levelwiseNames = new Comparable[levelwiseStrings.length];
    for(int i = 0; i < levelwiseStrings.length; i++) {
      try {
        levelwiseNames[i] = new Integer(levelwiseStrings[i]);
      }
      catch(NumberFormatException e) {
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
   * @see #init(String, java.util.regex.Pattern, String)
   */
  @Override
  public void init(String label) {
    init(label, DEFAULT_SEPARATOR, DEFAULT_SEPARATOR_STRING);
  }

  /**
   * Compares two HierarchicalClassLabels. Names at higher levels are compared
   * first. Names at a lower level are compared only if their parent-names are
   * equal. Names at a level are tried to be compared as integer values. If this
   * does not succeed, both names are compared as Strings.
   * 
   */
  @SuppressWarnings("unchecked")
  public int compareTo(ClassLabel o) {
    HierarchicalClassLabel h = (HierarchicalClassLabel) o;
    for(int i = 0; i < this.levelwiseNames.length && i < h.levelwiseNames.length; i++) {
      int comp = 0;
      try {
        Comparable first = this.levelwiseNames[i];
        Comparable second = h.levelwiseNames[i];
        comp = first.compareTo(second);
      }
      catch(RuntimeException e) {
        String h1 = (String) (this.levelwiseNames[i] instanceof Integer ? this.levelwiseNames[i].toString() : this.levelwiseNames[i]);
        String h2 = (String) (h.levelwiseNames[i] instanceof Integer ? h.levelwiseNames[i].toString() : h.levelwiseNames[i]);
        comp = h1.compareTo(h2);
      }
      if(comp != 0) {
        return comp;
      }
    }
    return new Integer(this.levelwiseNames.length).compareTo(new Integer(h.levelwiseNames.length));
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
   * @see #toString(int)
   */
  @Override
  public String toString() {
    return toString(levelwiseNames.length);
  }

  /**
   * Provides a String representation of this ClassLabel comprising only the
   * first <code>level</code> levels.
   * 
   * @param level the lowest level to include in the String representation.
   * @return a String representation of this ClassLabel comprising only the
   *         first <code>level</code> levels
   */
  public String toString(int level) {
    if(level > levelwiseNames.length) {
      throw new IllegalArgumentException("Specified level exceeds depth of hierarchy.");
    }

    StringBuffer name = new StringBuffer();
    for(int i = 0; i < level; i++) {
      name.append(this.getNameAt(i));
      if(i < level - 1) {
        name.append(this.separatorString);
      }
    }
    return name.toString();
  }
}
