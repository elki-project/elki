/*
 * Created on Feb 21, 2005
 */
package experimentalcode.marisa.utils;

import java.text.DateFormat;
import java.util.Date;

/**
 * Contains pretty-print time measures and distances.
 * 
 * @author Marisa Thoma
 */
public class Zeit {

	/**
	 * @return String of time now
	 */
	public static String jetzt() {
		Date now = new Date();
		return DateFormat.getTimeInstance().format(now);
	}

	/**
	 * @param seit
	 *            reference time
	 * @return time spent since <code>seit</code>
	 */
	public static String wieLange(Date seit) {
		long distance = System.currentTimeMillis() - seit.getTime();
		return toTimeDiffString(distance);
	}
	
	/**
	 * @param distance	time spent in milliseconds
	 * @return pretty-printed time difference 
	 */
	public static String toTimeDiffString(long distance) {
		if (distance / 1000 == 0)
			return distance + " ms";
		if (distance / 60000 == 0)
			return (distance / 1000) + " sec " + (distance % 1000) + " ms";
		if (distance / 60000 < 60)
			return (distance / 60000) + " min " + ((distance % 60000) / 1000)
					+ " sec";
		if (distance / 60000 / 60 < 24)
			return (distance / 60000 / 60) + " h "
					+ ((distance % (60 * 60000)) / 60000) + " min";
		if (distance / 60000 / 60 / 24 < 7)
			return (distance / 60000 / 60 / 24) + " d "
					+ ((distance % (24 * 60 * 60000)) / 60000 / 60) + " h "
					+ ((distance % (24 * 60 * 60000) % (60 * 60000)) / 60000)
					+ " min";
		if (distance / 60000 / 60 / 24 / 7 < 5)
			return (distance / 60000 / 60 / 24 / 7)
					+ " w "
					+ ((distance % (7 * 24 * 60 * 60000)) / 60000 / 60 / 24)
					+ " d "
					+ ((distance % (7 * 24 * 60 * 60000) % (24 * 60 * 60000)) / 60000 / 60)
					+ " h "
					+ ((distance % (7 * 24 * 60 * 60000) % (24 * 60 * 60000) % (60 * 60000)) / 60000)
					+ " min";

		return distance + " milliseconds, lunatic!";
	}
}
