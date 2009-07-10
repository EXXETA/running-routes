package com.exxeta.routing.marker;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MarkerServlet extends HttpServlet {

    @Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		res.setContentType("image/png");

		try {
			BufferedImage bufferedImage = ImageIO
					.read(new File(
							getServletContext()
									.getRealPath(
											"WEB-INF/classes/com/exxeta/routing/marker/marker.png")));

			String no = req.getParameter("no");
			if (no == null) {
				no = "x";
			}
			Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			Font exFont = new Font("Verdana", Font.PLAIN, 10);
			g.setFont(exFont);
			TextLayout layout = new TextLayout(no, exFont,
					new FontRenderContext(null, false, false));
			Rectangle2D bounds = layout.getBounds();
			int x = (int) ((bufferedImage.getWidth() - bounds.getWidth()) / 2 - 1);

			g.setColor(Color.black);
			g.drawString(no, x, 16);

			g.dispose();

			ImageIO.write(bufferedImage, "png", res.getOutputStream());
		} catch (IOException ioe) {

		}
	}
}
