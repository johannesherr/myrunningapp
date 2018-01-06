package de.johannesherr.myrunningapp;

import static de.johannesherr.myrunningapp.Utils.getMeterPerSecond;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import android.annotation.SuppressLint;

public class UtilsTest {

	@Test
	public void min_per_km() throws Exception {
		assertThat(Utils.getMinPerKM(1000, 360_000), is(6.0));
		assertThat(Utils.getMinPerKM(2 * 1000, 2 * 360_000), is(6.0));
		assertThat(Utils.getMinPerKM(2 * 1000, 360_000), is(3.0));
	}

	@Test
	public void km_per_hour() throws Exception {
		assertThat(Utils.getKMPerHour(1000, 360_000), is(10.0));
		assertThat(Utils.getKMPerHour(2 * 1000, 2 * 360_000), is(10.0));
		assertThat(Utils.getKMPerHour(2 * 1000, 360_000), is(20.0));
	}

	@Test
	public void toMinutesPerKM() throws Exception {
		assertThat(Utils.mPerSec_to_mPerKM(5), is(1000.0 / 5 / 60));
	}

	@Test
	public void toKMPerHour() throws Exception {
		assertThat(Utils.mPerSec_to_kmPerH(5), is(5 * 60 * 60 / 1000.0));
	}

	@Test
	public void meterPerSecond() throws Exception {
		assertThat(getMeterPerSecond(1000, 1000), is(1000.0));
		assertThat(getMeterPerSecond(2000, 1000), is(2000.0));
		assertThat(getMeterPerSecond(1000, 500), is(2000.0));
	}

	@Test
	public void parseit() throws Exception {
		List<double[]> parse = Utils.parse("(123,231,123),(13,1321,231)");
		System.out.printf("parse = %s%n", parse);
	}

	@SuppressLint("NewApi")
	@Test
	public void parseit2() throws Exception {
		String content = slurp("../sample.txt");
		List<double[]> parse = Utils.parse(content);
		double dur = parse.get(parse.size() - 1)[2] - parse.get(0)[2];
		System.out.printf("dur = %s%n", dur / 1000 / 60.0);
//		System.out.printf("parse = %s%n", parse);

		String template = "      <trkpt lat=\"%s\" lon=\"%s\"></trkpt>\n";

		double[] avs = new double[30];
		Arrays.fill(avs, -1);
		int i = 0;

		StringBuilder sb = new StringBuilder();
		double[] prev = null;
		double total = 0.0;
		for (double[] loc : parse) {
			sb.append(String.format(template, loc[0], loc[1]));
			if (prev != null) {
				double[] ret = new double[2];
				computeDistanceAndBearing(loc[0], loc[1], prev[0], prev[1], ret);
				double distM = ret[0];
				if (distM > 0) {
					double speed = Utils.getMinPerKM(distM, loc[2] - prev[2]);
					avs[i++ % avs.length] = speed;
					double sum = 0; int cnt = 0;
					for (int j = 0; j < avs.length; j++) {
						double av = avs[j];
						if (av >= 0) {
							sum += av;
							cnt++;
						}
					}
					System.out.println(String.format("%5.2f", sum / cnt));
				}
				total += distM;
			}
			prev = loc;
		}

		System.out.printf("total = %s%n", total);
		
		Files.write(Paths.get("../mine.gpx"), String.format(slurp("../path.gpx"), sb.toString()).getBytes());
	}

	private static void computeDistanceAndBearing(double lat1, double lon1,
	                                              double lat2, double lon2, double[] results) {
		// Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
		// using the "Inverse Formula" (section 4)

		int MAXITERS = 20;
		// Convert lat/long to radians
		lat1 *= Math.PI / 180.0;
		lat2 *= Math.PI / 180.0;
		lon1 *= Math.PI / 180.0;
		lon2 *= Math.PI / 180.0;

		double a = 6378137.0; // WGS84 major axis
		double b = 6356752.3142; // WGS84 semi-major axis
		double f = (a - b) / a;
		double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

		double L = lon2 - lon1;
		double A = 0.0;
		double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
		double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

		double cosU1 = Math.cos(U1);
		double cosU2 = Math.cos(U2);
		double sinU1 = Math.sin(U1);
		double sinU2 = Math.sin(U2);
		double cosU1cosU2 = cosU1 * cosU2;
		double sinU1sinU2 = sinU1 * sinU2;

		double sigma = 0.0;
		double deltaSigma = 0.0;
		double cosSqAlpha = 0.0;
		double cos2SM = 0.0;
		double cosSigma = 0.0;
		double sinSigma = 0.0;
		double cosLambda = 0.0;
		double sinLambda = 0.0;

		double lambda = L; // initial guess
		for (int iter = 0; iter < MAXITERS; iter++) {
			double lambdaOrig = lambda;
			cosLambda = Math.cos(lambda);
			sinLambda = Math.sin(lambda);
			double t1 = cosU2 * sinLambda;
			double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
			double sinSqSigma = t1 * t1 + t2 * t2; // (14)
			sinSigma = Math.sqrt(sinSqSigma);
			cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
			sigma = Math.atan2(sinSigma, cosSigma); // (16)
			double sinAlpha = (sinSigma == 0) ? 0.0 :
							cosU1cosU2 * sinLambda / sinSigma; // (17)
			cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
			cos2SM = (cosSqAlpha == 0) ? 0.0 :
							cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

			double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
			A = 1 + (uSquared / 16384.0) * // (3)
							(4096.0 + uSquared *
											(-768 + uSquared * (320.0 - 175.0 * uSquared)));
			double B = (uSquared / 1024.0) * // (4)
							(256.0 + uSquared *
											(-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
			double C = (f / 16.0) *
							cosSqAlpha *
							(4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
			double cos2SMSq = cos2SM * cos2SM;
			deltaSigma = B * sinSigma * // (6)
							(cos2SM + (B / 4.0) *
											(cosSigma * (-1.0 + 2.0 * cos2SMSq) -
															(B / 6.0) * cos2SM *
																			(-3.0 + 4.0 * sinSigma * sinSigma) *
																			(-3.0 + 4.0 * cos2SMSq)));

			lambda = L +
							(1.0 - C) * f * sinAlpha *
											(sigma + C * sinSigma *
															(cos2SM + C * cosSigma *
																			(-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

			double delta = (lambda - lambdaOrig) / lambda;
			if (Math.abs(delta) < 1.0e-12) {
				break;
			}
		}

		float distance = (float) (b * A * (sigma - deltaSigma));
		results[0] = distance;
		float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
		                                          cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
		initialBearing *= 180.0 / Math.PI;
		float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
		                                        -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
		finalBearing *= 180.0 / Math.PI;
		results[1] = finalBearing;
	}

	private String slurp(String path) throws IOException {
		return new String(Files.readAllBytes(Paths.get(path)));
	}

	private static List<double[]> parse(final String locStr) {
		final List<double[]> locs = new ArrayList<>();
		class Parser {
			private int i = 0;

			private void parse() {
				System.out.printf("locs = %s%n", locStr.length());
				while (i < locStr.length()) {
					consume('(');
					String lat = readUntil(',');
					consume(',');
					String log = readUntil(',');
					consume(',');
					String ts = readUntil(')');
					consume(')');
					locs.add(new double[]{Double.parseDouble(lat), Double.parseDouble(log), Double.parseDouble(ts)});
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