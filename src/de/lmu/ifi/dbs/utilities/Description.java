package de.lmu.ifi.dbs.utilities;

import java.util.Vector;

/**
 * Class to provide a description wrapper for CLI information concerning an algorithm.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class Description
{
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
     * Indent for printing.
     */
    private static String indent = "    ";
    
    /**
     * Width of columns.
     */
    private static int[] columns = {indent.length(),80 - indent.length()};
    
    /**
     * A PrettyPrinter for the defined columns.
     */
    private static PrettyPrinter prettyPrinter = new PrettyPrinter(columns," ");
    
    /**
     * Wrapper to provide a description wrapper for CLI information concerning an algorithm.
     * 
     * The parameters are to describe desired informations.
     * @param shortTitle a short title for the algorithm
     * @param longTitle a long title for the algorithm
     * @param purposeAndDescription a description of purpose and functionality of the algorithm
     * @param reference a reference to literature to be cited when using this algorithm
     */
    public Description(String shortTitle, String longTitle, String purposeAndDescription, String reference)
    {
        this.shortTitle = shortTitle;
        this.longTitle = longTitle;
        this.purpose = purposeAndDescription;
        this.reference = reference;
    }
    
    /**
     * Returns a String for printing the description.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        result.append(shortTitle);
        result.append(":");
        result.append(NEWLINE);
        StringBuffer desc = new StringBuffer();
        desc.append(NEWLINE);
        desc.append(longTitle);
        desc.append(NEWLINE);
        desc.append(NEWLINE);
        desc.append(purpose);
        desc.append(NEWLINE);
        desc.append(NEWLINE);
        desc.append("Reference:");
        desc.append(NEWLINE);
        desc.append(reference);
        desc.append(NEWLINE);
        Vector lines = prettyPrinter.breakLine(desc.toString(),1);
        for(int i = 0; i < lines.size(); i++)
        {
            result.append(indent);
            result.append((String) lines.get(i));
            result.append(NEWLINE);
        }        
        return result.toString();
    }

  /**
   * Returns the short title of this description.
   * @return the short title of this description
   */
  public String getShortTitle() {
    return shortTitle;
  }


}
