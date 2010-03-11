package de.lmu.ifi.dbs.elki.utilities;

import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * Class to provide a description wrapper for CLI information concerning an
 * algorithm.
 * 
 * @author Arthur Zimek
 * 
 * @deprecated use Annotations instead: \@Title, \@Description, \@Reference! 
 */
@Deprecated
public class OldDescription {
  /**
   * Newline String according to the system.
   */
  public final static String NEWLINE = System.getProperty("line.separator");

  /**
   * Short title of an algorithm.
   */
  private String shortTitle;

  /**
   * Long title of an algorithm.
   */
  private String longTitle;

  /**
   * A description of the purpose of an algorithm.
   */
  private String purpose;

  /**
   * A reference how to cite an algorithm.
   */
  private String reference;

  /**
   * Default indent
   */
  private final static String indent = "    ";

  /**
   * Wrapper to provide a description wrapper for CLI information concerning an
   * algorithm.
   * <p/>
   * The parameters are to describe desired informations.
   * 
   * @param shortTitle a short title for the algorithm
   * @param longTitle a long title for the algorithm
   * @param purposeAndDescription a description of purpose and functionality of
   *        the algorithm
   * @param reference a reference to literature to be cited when using this
   *        algorithm
   * @deprecated Use \@Reference annotation instead!
   */
  @Deprecated
  public OldDescription(String shortTitle, String longTitle, String purposeAndDescription, String reference) {
    this.shortTitle = shortTitle;
    this.longTitle = longTitle;
    this.purpose = purposeAndDescription;
    this.reference = reference;
  }

  /**
   * Wrapper to provide a description wrapper for CLI information concerning an
   * algorithm.
   * <p/>
   * The parameters are to describe desired informations.
   * 
   * @param shortTitle a short title for the algorithm
   * @param longTitle a long title for the algorithm
   * @param purposeAndDescription a description of purpose and functionality of
   *        the algorithm
   */
  public OldDescription(String shortTitle, String longTitle, String purposeAndDescription) {
    this.shortTitle = shortTitle;
    this.longTitle = longTitle;
    this.purpose = purposeAndDescription;
    this.reference = null;
  }

  /**
   * Wrapper to provide a description wrapper for CLI information concerning an
   * algorithm.
   * <p/>
   * The parameters are to describe desired informations.
   * 
   * @param cls a short title for the algorithm
   * @param longTitle a long title for the algorithm
   * @param purposeAndDescription a description of purpose and functionality of
   *        the algorithm
   * @param reference a reference to literature to be cited when using this
   *        algorithm
   */
  public OldDescription(Class<?> cls, String longTitle, String purposeAndDescription, String reference) {
    this.shortTitle = cls.getSimpleName();
    this.longTitle = longTitle;
    this.purpose = purposeAndDescription;
    this.reference = reference;
  }

  /**
   * Wrapper to provide a description wrapper for CLI information concerning an
   * algorithm.
   * <p/>
   * The parameters are to describe desired informations.
   * 
   * @param cls a short title for the algorithm
   * @param longTitle a long title for the algorithm
   * @param purposeAndDescription a description of purpose and functionality of
   *        the algorithm
   */
  public OldDescription(Class<?> cls, String longTitle, String purposeAndDescription) {
    this(cls, longTitle, purposeAndDescription, null);
  }

  /**
   * Returns a String for printing the description.
   * 
   * @see Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append(shortTitle);
    result.append(": ");
    result.append(longTitle);
    result.append(NEWLINE);

    final int width = FormatUtil.getConsoleWidth();

    if(purpose != null) {
      for(String line : FormatUtil.splitAtLastBlank(purpose, width - indent.length())) {
        result.append(indent);
        result.append(line);
        result.append(NEWLINE);
      }
    }
    if(reference != null) {
      result.append("Reference: " + NEWLINE);
      for(String line : FormatUtil.splitAtLastBlank(reference, width - indent.length())) {
        result.append(indent);
        result.append(line);
        result.append(NEWLINE);
      }
    }

    return result.toString();
  }

  /**
   * Returns the short title of this description.
   * 
   * @return the short title of this description
   */
  public String getShortTitle() {
    return shortTitle;
  }
  
  /**
   * Returns the long title of this description.
   * 
   * @return the long title of this description
   */
  public String getLongTitle() {
    return longTitle;
  }
}