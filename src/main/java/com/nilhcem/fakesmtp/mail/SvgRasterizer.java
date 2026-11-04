package com.nilhcem.fakesmtp.mail;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;

/**
 * Draws an SVG document to a PNG so the HTML renderer can show charts.
 *
 * @since 2.1
 */
public final class SvgRasterizer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SvgRasterizer.class);
	private static final int MAX_EDGE = 1600;

	private SvgRasterizer() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param svg an SVG document.
	 * @return PNG bytes, or {@code null} when the document cannot be drawn.
	 */
	public static byte[] toPng(String svg) {
		if (svg == null || svg.isBlank()) {
			return null;
		}
		try {
			SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
			if (document == null || document.size() == null) {
				return null;
			}
			int width = edge(document.size().getWidth());
			int height = edge(document.size().getHeight());
			if (width <= 0 || height <= 0) {
				return null;
			}
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			try {
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				document.render(null, graphics, new ViewBox(document.size()));
			} finally {
				graphics.dispose();
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (!ImageIO.write(image, "png", output)) {
				return null;
			}
			return output.toByteArray();
		} catch (Exception e) {
			LOGGER.warn("Could not draw an SVG image embedded in an email", e);
			return null;
		}
	}

	private static int edge(double value) {
		if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
			return 0;
		}
		int rounded = (int) Math.ceil(value);
		if (rounded > MAX_EDGE) {
			return MAX_EDGE;
		}
		return rounded;
	}
}
