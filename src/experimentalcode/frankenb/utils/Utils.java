package experimentalcode.frankenb.utils;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class Utils {
  private Utils() {}
  

  public static long sumFormular(final long i) {
    return ((i - 1L) * i) / 2;
  }
  
  private static long[] timeAmounts = new long[] {1L, 1000L, 60000L, 3600000L, 86400000L};
  private static String[] timeNames = new String[] {"ms", "s", "m", "h", "d"};
  private static int[] timeDigits = new int[] {3, 2, 2, 2, 2};
  
  /**
   * Formats the runtime in ms to a human readable format
   * 
   * @param time
   * @return
   */
  public static String formatRunTime(long time) {
    StringBuilder sb = new StringBuilder();
    
    for (int i = timeAmounts.length - 1; i >= 0; --i) {

      //if we have minutes already then we can ignore the ms
      if (i == 0 && sb.length() > 4) continue;
      if (sb.length() > 0) {
        sb.append(" ");
      }
      
      long acValue = time / timeAmounts[i];
      time = time % timeAmounts[i];
      if (!(acValue == 0 && sb.length() == 0)) {
        sb.append(String.format("%0" + timeDigits[i] + "d%s", acValue, timeNames[i]));
      }
    }
    return sb.toString();
  }  
  
}
