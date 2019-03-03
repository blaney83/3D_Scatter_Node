package io.github.blaney83;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.controllers.camera.AbstractCameraController;

@SuppressWarnings("serial")
public class ScatterPlot3DNodeViewPanel extends JPanel {


	// graph instance
	private ScatterPlot3DGraphPanel graphPanel;
	private TitlePanel titlePanel;

	 public ScatterPlot3DNodeViewPanel(final ScatterPlot3DNodeModel nodeModel) {
		setPreferredSize(new Dimension(ScatterPlot3DSettings.PANEL_WIDTH, ScatterPlot3DSettings.PANEL_HEIGHT));
		setLayout(new BorderLayout());
		StringBuilder graphTitle = new StringBuilder(nodeModel.m_settings.getXAxisVarColumn());
		graphTitle.append(" (X), " + nodeModel.m_settings.getYAxisVarColumn());
		graphTitle.append(" (Y), " + nodeModel.m_settings.getZAxisVarColumn() + " (Z)");
		titlePanel = new TitlePanel(graphTitle.toString());
		add(titlePanel, BorderLayout.NORTH);
		graphPanel = new ScatterPlot3DGraphPanel(nodeModel);
		graphPanel.init();
		Chart chart = graphPanel.getChart();
		chart.addController((AbstractCameraController)ChartLauncher.configureControllers(chart, "", true, false));
		add((Canvas)chart.getCanvas(), BorderLayout.CENTER);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
	}

	public void updateView(final ScatterPlot3DNodeModel nodeModel) {
		graphPanel = new ScatterPlot3DGraphPanel(nodeModel);
		graphPanel.init();
		Chart chart = graphPanel.getChart();
		chart.addController((AbstractCameraController)ChartLauncher.configureControllers(chart, "", true, false));
		add((Canvas)chart.getCanvas(), BorderLayout.CENTER);
	}

	private final class TitlePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final String graphTitle;
		private static final int WIDTH = 25;
		private static final int HEIGHT = 30;

		public TitlePanel(final String title) {
			this.graphTitle = title;
			setPreferredSize(new Dimension(WIDTH, HEIGHT));
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2D = (Graphics2D) g;

			Font font = new Font("Arial", Font.BOLD, 20);
			FontMetrics metrics = g2D.getFontMetrics(font);
			int width = metrics.stringWidth(this.graphTitle);

			g2D.setFont(font);

			g2D.drawString(this.graphTitle, (getWidth() - width) / 2, 20);
		}
	}
}
