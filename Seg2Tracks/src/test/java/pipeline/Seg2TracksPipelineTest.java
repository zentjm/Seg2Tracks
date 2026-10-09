package pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.awt.Point;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Properties;

import org.junit.Test;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import util.UserInputException;

/**
 * Tests for the GUI-independent pipeline core.
 * <p>
 * The golden test pins the full automatic pipeline's output on Phantom28: any change to any
 * algorithm's results changes the fingerprint. If a change is intended (e.g. a deliberate
 * algorithm fix), confirm the new results are right, then update {@link #PHANTOM28_FINGERPRINT}.
 */
public class Seg2TracksPipelineTest {

	/** Phantom28 location; override with -Dseg2tracks.phantom28=/path/to/Phantom28_with_voids.tif */
	private static final String PHANTOM28 = System.getProperty("seg2tracks.phantom28",
			System.getProperty("user.home") + "/Desktop/Testing/Input/Phantom28_with_voids.tif");

	/**
	 * Fingerprint of {@link #phantomSettings()} on Phantom28, recorded 2026-10-08 and checked
	 * identical to the GUI's pre-refactor step sequence (OperationModel before the pipeline core).
	 */
	private static final String PHANTOM28_FINGERPRINT = "72e80bad1a35a737";

	static PipelineSettings phantomSettings() {
		return new PipelineSettings(15.0, 0.05, 10.0, false, 0.15, 400, 1.5, false,
				"sarn.OneWayContraction", "segmentation.RestrictedLiMethod", "linkage.GlobalNearestNeighbor");
	}

	@Test
	public void settingsSurvivePropertiesRoundTrip() {
		PipelineSettings s = phantomSettings();
		Properties p = new Properties();
		s.toProperties(p, "panel0.");
		PipelineSettings back = PipelineSettings.fromProperties(p, "panel0.");
		assertEquals(s.toString(), back.toString());
	}

	@Test
	public void missingSettingIsNamed() {
		Properties p = new Properties();
		phantomSettings().toProperties(p, "");
		p.remove("linkageMethod");
		try {
			PipelineSettings.fromProperties(p, "");
			fail("expected an error");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("linkageMethod"));
		}
	}

	@Test
	public void sameDimensionsPass() {
		InputChecks.requireSameDimensions(new DataSet(10, 20, 3), new DataSet(10, 20, 3));
	}

	@Test(expected = UserInputException.class)
	public void differentDimensionsAreRejected() {
		InputChecks.requireSameDimensions(new DataSet(10, 20, 3), new DataSet(10, 20, 4));
	}

	@Test(expected = UserInputException.class)
	public void missingInputIsRejected() {
		InputChecks.requireSameDimensions(new DataSet(10, 20, 3), null);
	}

	@Test(expected = UserInputException.class)
	public void unknownMethodIsReported() {
		Seg2TracksPipeline.createSarn(phantomSettings().withMethods("sarn.NoSuchMethod",
				"segmentation.RestrictedLiMethod", "linkage.GlobalNearestNeighbor"));
	}

	@Test
	public void phantom28GoldenResult() throws Exception {
		assumeTrue("Phantom28 not found at " + PHANTOM28 + " (skipping)", new File(PHANTOM28).isFile());
		PipelineSettings s = phantomSettings();
		DataSet result = Seg2TracksPipeline.runAll(
				Seg2TracksPipeline.loadStack(PHANTOM28, s.getInvertIntensity()), s, true, true, null);
		assertEquals("Pipeline output on Phantom28 changed", PHANTOM28_FINGERPRINT, fingerprint(result));
	}

	/** Short hash of every frame's segments: frame, centre, track, external and internal outlines. */
	static String fingerprint(DataSet d) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (FrameSet fs : d.getFrameSetList()) {
			if (fs == null) { sb.append("null;"); continue; }
			for (Segment seg : fs) {
				sb.append(seg.getFrame()).append('|').append(seg.getCenterPoint()).append('|')
				  .append(d.getLinkSetList().indexOf(seg.getLinkSet())).append('|');
				points(sb, seg.getExternalPerimeter());
				sb.append('|');
				points(sb, seg.getInternalPerimeter());
				sb.append(';');
			}
		}
		byte[] h = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder();
		for (int i = 0; i < 8; i++) hex.append(String.format("%02x", h[i]));
		return hex.toString();
	}

	private static void points(StringBuilder sb, Point[] pts) {
		if (pts == null) { sb.append("null"); return; }
		for (Point q : pts) sb.append(q.x).append(',').append(q.y).append(' ');
	}
}
