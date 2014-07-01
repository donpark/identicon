package com.docuverse.identicon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * 9-block Identicon renderer.
 * 
 * <p>
 * Current implementation uses only the lower 32 bits of identicon code.
 * </p>
 * 
 * @author don
 * 
 */
public class NineBlockIdenticonRenderer implements IdenticonRenderer {
	private static final int DEFAULT_PATCH_SIZE = 20;

	/*
	 * Each patch is a polygon created from a list of vertices on a 5 by 5 grid.
	 * Vertices are numbered from 0 to 24, starting from top-left corner of the
	 * grid, moving left to right and top to bottom.
	 */

	private static final int PATCH_CELLS = 4;

	private static final int PATCH_GRIDS = PATCH_CELLS + 1;

	private static final byte PATCH_SYMMETRIC = 1;

	private static final byte PATCH_INVERTED = 2;

	private static final byte[] patch0 = { 0, 4, 24, 20, 0 };

	private static final byte[] patch1 = { 0, 4, 20, 0 };

	private static final byte[] patch2 = { 2, 24, 20, 2 };

	private static final byte[] patch3 = { 0, 2, 20, 22, 0 };

	private static final byte[] patch4 = { 2, 14, 22, 10, 2 };

	private static final byte[] patch5 = { 0, 14, 24, 22, 0 };

	private static final byte[] patch6 = { 2, 24, 22, 13, 11, 22, 20, 2 };

	private static final byte[] patch7 = { 0, 14, 22, 0 };

	private static final byte[] patch8 = { 6, 8, 18, 16, 6 };

	private static final byte[] patch9 = { 4, 20, 10, 12, 2, 4 };

	private static final byte[] patch10 = { 0, 2, 12, 10, 0 };

	private static final byte[] patch11 = { 10, 14, 22, 10 };

	private static final byte[] patch12 = { 20, 12, 24, 20 };

	private static final byte[] patch13 = { 10, 2, 12, 10 };

	private static final byte[] patch14 = { 0, 2, 10, 0 };

	private static final byte[] patchTypes[] = { patch0, patch1, patch2,
			patch3, patch4, patch5, patch6, patch7, patch8, patch9, patch10,
			patch11, patch12, patch13, patch14, patch0 };

	private static final byte patchFlags[] = { PATCH_SYMMETRIC, 0, 0, 0,
			PATCH_SYMMETRIC, 0, 0, 0, PATCH_SYMMETRIC, 0, 0, 0, 0, 0, 0,
			PATCH_SYMMETRIC + PATCH_INVERTED };

	private static int centerPatchTypes[] = { 0, 4, 8, 15 };

	private int patchSize;

	private Shape[] patchShapes;

	// used to center patch shape at origin because shape rotation works
	// correctly.
	private int patchOffset;

	private Color backgroundColor = Color.WHITE;

	/**
	 * Constructor.
	 * 
	 */
	public NineBlockIdenticonRenderer() {
		setPatchSize(DEFAULT_PATCH_SIZE);
	}

	/**
	 * Returns the size in pixels at which each patch will be rendered before
	 * they are scaled down to requested identicon size.
	 * 
	 * @return
	 */
	public int getPatchSize() {
		return patchSize;
	}

	/**
	 * Set the size in pixels at which each patch will be rendered before they
	 * are scaled down to requested identicon size. Default size is 20 pixels
	 * which means, for 9-block identicon, a 60x60 image will be rendered and
	 * scaled down.
	 * 
	 * @param size
	 *            patch size in pixels
	 */
	public void setPatchSize(int size) {
		this.patchSize = size;
		this.patchOffset = patchSize / 2; // used to center patch shape at
		// origin.
		int scale = patchSize / PATCH_CELLS;
		this.patchShapes = new Polygon[patchTypes.length];
		for (int i = 0; i < patchTypes.length; i++) {
			Polygon patch = new Polygon();
			byte[] patchVertices = patchTypes[i];
			for (int j = 0; j < patchVertices.length; j++) {
				int v = (int) patchVertices[j];
				int vx = (v % PATCH_GRIDS * scale) - patchOffset;
				int vy = (v / PATCH_GRIDS * scale) - patchOffset;
				patch.addPoint(vx, vy);
			}
			this.patchShapes[i] = patch;
		}
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public BufferedImage render(BigInteger code, int size) {
		return renderQuilt(code.intValue(), size);
	}

	/**
	 * Returns rendered identicon image for given identicon code.
	 * 
	 * <p>
	 * Size of the returned identicon image is determined by patchSize set using
	 * {@link setPatchSize}. Since a 9-block identicon consists of 3x3 patches,
	 * width and height will be 3 times the patch size.
	 * </p>
	 * 
	 * @param code
	 *            identicon code
	 * @param size
	 *            image size
	 * @return identicon image
	 */
	public BufferedImage render(int code, int size) {
		return renderQuilt(code, size);
	}

	protected BufferedImage renderQuilt(int code, int size) {
		// -------------------------------------------------
		// PREPARE
		//

		// decode the code into parts
		// bit 0-1: middle patch type
		// bit 2: middle invert
		// bit 3-6: corner patch type
		// bit 7: corner invert
		// bit 8-9: corner turns
		// bit 10-13: side patch type
		// bit 14: side invert
		// bit 15: corner turns
		// bit 16-20: blue color component
		// bit 21-26: green color component
		// bit 27-31: red color component
		int middleType = centerPatchTypes[code & 0x3];
		boolean middleInvert = ((code >> 2) & 0x1) != 0;
		int cornerType = (code >> 3) & 0x0f;
		boolean cornerInvert = ((code >> 7) & 0x1) != 0;
		int cornerTurn = (code >> 8) & 0x3;
		int sideType = (code >> 10) & 0x0f;
		boolean sideInvert = ((code >> 14) & 0x1) != 0;
		int sideTurn = (code >> 15) & 0x3;
		int blue = (code >> 16) & 0x01f;
		int green = (code >> 21) & 0x01f;
		int red = (code >> 27) & 0x01f;

		// color components are used at top of the range for color difference
		// use white background for now.
		// TODO: support transparency.
		Color fillColor = new Color(red << 3, green << 3, blue << 3);

		// outline shapes with a noticeable color (complementary will do) if
		// shape color and background color are too similar (measured by color
		// distance).
		Color strokeColor = null;
		if (getColorDistance(fillColor, backgroundColor) < 32.0f)
			strokeColor = getComplementaryColor(fillColor);

		// -------------------------------------------------
		// RENDER AT SOURCE SIZE
		//

		int sourceSize = patchSize * 3;
		BufferedImage sourceImage = new BufferedImage(sourceSize, sourceSize,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = sourceImage.createGraphics();

		// middle patch
		drawPatch(g, patchSize, patchSize, middleType, 0, middleInvert,
				fillColor, strokeColor);

		// side patchs, starting from top and moving clock-wise
		drawPatch(g, patchSize, 0, sideType, sideTurn++, sideInvert, fillColor,
				strokeColor);
		drawPatch(g, patchSize * 2, patchSize, sideType, sideTurn++,
				sideInvert, fillColor, strokeColor);
		drawPatch(g, patchSize, patchSize * 2, sideType, sideTurn++,
				sideInvert, fillColor, strokeColor);
		drawPatch(g, 0, patchSize, sideType, sideTurn++, sideInvert, fillColor,
				strokeColor);

		// corner patchs, starting from top left and moving clock-wise
		drawPatch(g, 0, 0, cornerType, cornerTurn++, cornerInvert, fillColor,
				strokeColor);
		drawPatch(g, patchSize * 2, 0, cornerType, cornerTurn++, cornerInvert,
				fillColor, strokeColor);
		drawPatch(g, patchSize * 2, patchSize * 2, cornerType, cornerTurn++,
				cornerInvert, fillColor, strokeColor);
		drawPatch(g, 0, patchSize * 2, cornerType, cornerTurn++, cornerInvert,
				fillColor, strokeColor);

		g.dispose();

		// -------------------------------------------------
		// SCALE TO TARGET SIZE
		//
		// Bicubic algorithm is used for quality scaling

		BufferedImage targetImage = new BufferedImage(size, size,
				BufferedImage.TYPE_INT_RGB);
		g = targetImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(sourceImage, 0, 0, size, size, null);
		g.dispose();

		return targetImage;
	}

	private void drawPatch(Graphics2D g, int x, int y, int patch, int turn,
			boolean invert, Color fillColor, Color strokeColor) {
		assert patch >= 0;
		assert turn >= 0;
		patch %= patchTypes.length;
		turn %= 4;
		if ((patchFlags[patch] & PATCH_INVERTED) != 0)
			invert = !invert;

		// paint background
		g.setBackground(invert ? fillColor : backgroundColor);
		g.clearRect(x, y, patchSize, patchSize);

		// offset and rotate coordinate space by patch position (x, y) and
		// 'turn' before rendering patch shape
		AffineTransform saved = g.getTransform();
		g.translate(x + patchOffset, y + patchOffset);
		g.rotate(Math.toRadians(turn * 90));

		// if stroke color was specified, apply stroke
		// stroke color should be specified if fore color is too close to the
		// back color.
		if (strokeColor != null) {
			g.setColor(strokeColor);
			g.draw(patchShapes[patch]);
		}

		// render rotated patch using fore color (back color if inverted)
		g.setColor(invert ? backgroundColor : fillColor);
		g.fill(patchShapes[patch]);

		// restore rotation
		g.setTransform(saved);
	}

	/**
	 * Returns distance between two colors.
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	private float getColorDistance(Color c1, Color c2) {
		float dx = c1.getRed() - c2.getRed();
		float dy = c1.getGreen() - c2.getGreen();
		float dz = c1.getBlue() - c2.getBlue();
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Returns complementary color.
	 * 
	 * @param color
	 * @return
	 */
	private Color getComplementaryColor(Color color) {
		return new Color(color.getRGB() ^ 0x00FFFFFF);
	}
}
