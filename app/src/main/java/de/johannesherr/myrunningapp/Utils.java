package de.johannesherr.myrunningapp;

import java.util.ArrayList;
import java.util.List;

public class Utils {
	public static double getKMPerHour(double curDist, double duration) {
		return curDist * 60 * 60 / duration;
	}

	public static double getMinPerKM(double curDist, double duration) {
		return 1 / (curDist / duration) / 60;
	}

	public static double mPerSec_to_mPerKM(double mPerSec) {
		return 1000 / mPerSec / 60.0;
	}

	public static double mPerSec_to_kmPerH(double mPerSec) {
		return mPerSec * 3.6;
	}

	public static double getMeterPerSecond(double dist, long ms) {
		return dist / ms * 1000;
	}

	static List<double[]> parse(final String locStr) {
		final List<double[]> locs = new ArrayList<>();
		class Parser {
			private int i = 0;

			private void parse() {
				while (i < locStr.length()) {
					consume('(');
					String lat = readUntil(',');
					consume(',');
					String log = readUntil(',');
					consume(',');
					String ts = readUntil(')');
					consume(')');
					locs.add(new double[]{Double.parseDouble(lat), Double.parseDouble(log), Double.parseDouble(ts)});
					if (i + 1 < locStr.length()) {
						consume(',');
					}
				}
			}

			private String readUntil(char c) {
				int start = i;
				while (locStr.charAt(i) != c) i++;
				return locStr.substring(start, i);
			}

			private void consume(char c) {
				if (locStr.charAt(i) != c) throw new AssertionError("[" + i + "] " + c + " expected");
				i++;
			}
		}
		new Parser().parse();
		return locs;
	}
}
