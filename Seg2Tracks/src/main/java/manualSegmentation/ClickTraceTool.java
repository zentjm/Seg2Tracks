package manualSegmentation;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import geometricTools.GeometricCalculations;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Click-to-trace freehand drawing, replacing ImageJ's hold-to-draw freehand tool.
 * <p>
 * <b>Tracing:</b> click once to start; the outline follows the cursor with the mouse button up.
 * Click again to finish, or the outline closes by itself when the cursor returns near the
 * starting point. Esc discards the trace in progress.
 * <p>
 * <b>Editing:</b> a finished trace is simplified (Douglas-Peucker) to a polygon with a
 * manageable number of nodes and handed to ImageJ's own polygon editing: drag a node to move
 * it, shift-click to add a node, alt-click to delete one, drag inside to move the whole
 * outline. Clicking away from the outline starts a new trace, replacing it.
 * <p>
 * ImageJ's tool is "hand" while tracing (so clicks don't also start a selection) and "polygon"
 * while a finished outline is editable. Canvas listeners run after ImageJ's own, so on a click
 * away from the outline ImageJ has already begun a new polygon; starting the trace replaces it.
 * <p>
 * The drawn outline is always left as the image's active {@code Roi}, which is where the rest
 * of the manual-segmentation workflow reads it from.
 */
public class ClickTraceTool {

	/** Auto-close distance from the start point, in screen pixels. */
	private static final double CLOSE_RADIUS_SCREEN = 6;
	/** The cursor must first travel this many close-radii from the start before auto-close can fire. */
	private static final double LEAVE_FACTOR = 3;
	/** Douglas-Peucker tolerance for the editable polygon, in screen pixels. */
	private static final double SIMPLIFY_SCREEN = 2;
	private static final int MIN_POINTS = 3;

	private ImagePlus image;
	private ImageCanvas canvas;
	/** Trace in progress, or null. */
	private FloatPolygon trace;
	private boolean leftStart;
	/** The finished, editable outline (as last seen after an edit), or null. */
	private Roi editable;
	/** Colour of the line while tracing (the user's ROI colour). */
	private Color traceColor;

	private final MouseAdapter mouse = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) return;
			if (trace != null) { finish(); return; }
			if (isEditingClick(e)) return; // ImageJ's polygon editing handles it
			start(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// Node edits happen in place or replace the Roi; keep tracking the current one.
			if (trace == null && editable != null) {
				Roi r = image.getRoi();
				if (r != null && r.getState() != Roi.CONSTRUCTING) editable = r;
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (trace != null) {
				canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				extend(e);
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (trace != null) extend(e);
		}
	};

	private final KeyAdapter keys = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE && trace != null) discard();
		}
	};

	/**
	 * Starts listening on {@code imp}'s canvas. Replaces any existing activation.
	 * @param color colour of the line while tracing (normally the ROI colour)
	 */
	public void activate(ImagePlus imp, Color color) {
		deactivate();
		image = imp;
		traceColor = color;
		canvas = imp.getCanvas();
		if (canvas == null) { image = null; return; }
		// An outline already on the image (e.g. Previous Frame restoring one) is editable.
		Roi existing = imp.getRoi();
		editable = (existing instanceof PolygonRoi && existing.isArea()) ? existing : null;
		IJ.setTool(editable != null ? "polygon" : "hand");
		canvas.addMouseListener(mouse);
		canvas.addMouseMotionListener(mouse);
		canvas.addKeyListener(keys);
	}

	/** Stops listening. An unfinished trace is discarded. Safe to call when inactive. */
	public void deactivate() {
		if (canvas != null) {
			canvas.removeMouseListener(mouse);
			canvas.removeMouseMotionListener(mouse);
			canvas.removeKeyListener(keys);
			canvas.setCursor(Cursor.getDefaultCursor());
		}
		if (trace != null && image != null) image.killRoi();
		trace = null;
		editable = null;
		image = null;
		canvas = null;
	}

	public boolean isActive() {
		return image != null;
	}

	/**
	 * Closes a trace that is still in progress, so the controller can read it as the drawn
	 * outline (called before Next Frame, End Object and Apply Redraw).
	 */
	public void commitPending() {
		if (trace != null) finish();
	}

	/** Called by the controller after it moves to a new frame, which clears the outline. */
	public void resetForNewFrame() {
		if (!isActive()) return; // polygon mode: leave ImageJ's tool alone
		trace = null;
		Roi r = image.getRoi();
		editable = (r instanceof PolygonRoi && r.isArea()) ? r : null;
		IJ.setTool(editable != null ? "polygon" : "hand");
	}

	/**
	 * True if this click is ImageJ polygon editing on the finished outline: on a node, inside
	 * the outline (move), or with shift/alt held (add/delete a node).
	 */
	private boolean isEditingClick(MouseEvent e) {
		if (editable == null) return false;
		if (e.isShiftDown() || e.isAltDown()) return true;
		if (editable.isHandle(e.getX(), e.getY()) >= 0) return true;
		return editable.contains((int) x(e), (int) y(e));
	}

	private void start(MouseEvent e) {
		IJ.setTool("hand");
		editable = null;
		trace = new FloatPolygon();
		leftStart = false;
		trace.addPoint(x(e), y(e));
		showTrace();
	}

	private void extend(MouseEvent e) {
		float x = x(e), y = y(e);
		int last = trace.npoints - 1;
		if (trace.xpoints[last] == x && trace.ypoints[last] == y) return;
		trace.addPoint(x, y);

		double radius = CLOSE_RADIUS_SCREEN / canvas.getMagnification();
		double dx = x - trace.xpoints[0], dy = y - trace.ypoints[0];
		double dist = Math.sqrt(dx * dx + dy * dy);
		if (dist > radius * LEAVE_FACTOR) leftStart = true;
		if (leftStart && dist <= radius && trace.npoints >= MIN_POINTS) {
			finish();
			return;
		}
		showTrace();
	}

	/** Closes the trace and turns it into an editable polygon. */
	private void finish() {
		if (trace.npoints < MIN_POINTS) {
			discard();
			return;
		}
		PolygonRoi polygon = simplify(trace);
		trace = null;
		show(polygon);
		editable = polygon;
		IJ.setTool("polygon");
		canvas.setCursor(Cursor.getDefaultCursor());
	}

	private void discard() {
		trace = null;
		image.killRoi();
	}

	private void show(Roi roi) {
		image.setRoi(roi);
	}

	/**
	 * Shows the trace in progress as a FREELINE, which ImageJ draws without vertex handles
	 * (a POLYLINE draws a handle on every point, which on a dense trace looks solid black).
	 */
	private void showTrace() {
		PolygonRoi line = new PolygonRoi(trace.duplicate(), Roi.FREELINE);
		if (traceColor != null) line.setStrokeColor(traceColor);
		show(line);
	}

	/** Douglas-Peucker simplification to a POLYGON Roi with a draggable number of nodes. */
	private PolygonRoi simplify(FloatPolygon fp) {
		Point[] pts = new Point[fp.npoints];
		for (int i = 0; i < fp.npoints; i++) {
			pts[i] = new Point(Math.round(fp.xpoints[i]), Math.round(fp.ypoints[i]));
		}
		double epsilon = Math.max(0.5, SIMPLIFY_SCREEN / canvas.getMagnification());
		Point[] simple = GeometricCalculations.douglasPeucker(pts, epsilon);
		if (simple == null || simple.length < MIN_POINTS) simple = pts;
		float[] xs = new float[simple.length], ys = new float[simple.length];
		for (int i = 0; i < simple.length; i++) { xs[i] = simple[i].x; ys[i] = simple[i].y; }
		return new PolygonRoi(xs, ys, simple.length, Roi.POLYGON);
	}

	private float x(MouseEvent e) { return (float) canvas.offScreenXD(e.getX()); }
	private float y(MouseEvent e) { return (float) canvas.offScreenYD(e.getY()); }
}
