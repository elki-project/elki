package de.lmu.ifi.dbs.elki.utilities.output;


import java.util.Vector;
import java.util.logging.Logger;

/**
 * Class for formatting output into table.
 *
 * @author Arthur Zimek
 */
public class PrettyPrinter {
    /**
     * The newline-String dependent on the system.
     */
    public final static String NEWLINE = System.getProperty("line.separator");

    /**
     * provides the column width for each column
     */
    private int[] columnWidth;

    /**
     * provides a separator to separate different columns (could be empty String)
     */
    private String separator;

    /**
     * Provides a PrettyPrinter with specified columnWidth and specified separator.
     *
     * @param columnWidth the column width for each column
     * @param separator   a separator to separate different columns (could be empty String)
     */
    public PrettyPrinter(int[] columnWidth, String separator) {
        this.columnWidth = columnWidth;
        this.separator = separator;
    }

    /**
     * Formats given lineEntries into formatted tableLine, filled with specified fillCharacter.
     *
     * @param lineEntries   the entries to be written into table
     * @param fillCharacter char to fill the table line with
     * @return formatted lineEntries
     */
    public String formattedLine(String[] lineEntries, char fillCharacter) {
        boolean[] leftBounded = new boolean[lineEntries.length];
        for (int i = 0; i < leftBounded.length; i++) {
            leftBounded[i] = true;
        }
        return formattedLine(lineEntries, fillCharacter, leftBounded);
    }

    /**
     * Formats given lineEntries into formatted tableLine, filled with specified fillCharacter.
     *
     * @param lineEntries   the entries to be written into table
     * @param fillCharacter char to fill the table line with
     * @param leftBounded   specifies, if column is leftBounded
     * @return formatted lineEntries
     */
    public String formattedLine(String[] lineEntries, char fillCharacter, boolean leftBounded[]) {
        StringBuffer line = new StringBuffer();
        if (lineEntries.length == columnWidth.length) {
            for (int i = 0; i < lineEntries.length; i++) {
                StringBuffer fill = new StringBuffer();
                for (int f = 0; f < (columnWidth[i] - lineEntries[i].length()); f++) {
                    fill.append(fillCharacter);
                }
                if (leftBounded[i]) {
                    line.append(lineEntries[i]);
                    line.append(fill.toString());
                }
                else {
                    line.append(fill.toString());
                    line.append(lineEntries[i]);
                }

                line.append(separator);
            }
        }
        else {
          Logger.getLogger(this.getClass().getName()).warning("Wrong number of entries!");
        }
        return line.toString();
    }

    /**
     * Breaks a line at near line-end spaces fitting to width of columnWidth[column].
     *
     * @param line   the line to break
     * @param column the index of column in columnWidth
     * @return Vector contains the lines made of the given line
     */
    public Vector<String> breakLine(String line, int column) {
        Vector<String> lines = new Vector<String>();

        Vector<String> splitLines = new Vector<String>();
        String[] split = line.split("\n");
        for (String s : split) {
            splitLines.add(s);
        }

        for (String s : splitLines) {
            // s is shorter than columnWidth[column] --> add s
            if (s.length() <= columnWidth[column]) {
                lines.add(s);
            }
            // split s at the last ' ' before columnWidth[column]
            else {
                String tmp1 = splitAtLastBlank(s, column);
                lines.add(tmp1);

                if (tmp1.length() < s.length()) {
                    String rest = s.substring(tmp1.length() + 1);
                    while (true) {
                        String tmp2 = splitAtLastBlank(rest, column);
                        lines.add(tmp2);
                        if (tmp2.length() < rest.length()) {
                            rest = rest.substring(tmp2.length() + 1);
                        }
                        else {
                            break;
                        }
                    }
                }

            }
        }
        return lines;
    }

    /**
     * Splits the specified string at the last blank before columnWidth[column].
     *
     * @param s      the string to be splitted
     * @param column the index of column in columnWidth
     * @return the splitted string
     */
    private String splitAtLastBlank(String s, int column) {
        if (s.length() <= columnWidth[column]) {
            return s;
        }

        String tmp = s;
        int index = tmp.lastIndexOf(' ');
        while (index > columnWidth[column]) {
            tmp = tmp.substring(0, index);
            index = tmp.lastIndexOf(' ');
        }

        if (index != -1) {
            tmp = tmp.substring(0, index);
        }

        return tmp;
    }
}
