package io.github.blaney83;

import java.util.Set;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.knime.core.data.property.ColorAttr;

public class ScatterPlot3DGraphPanel extends AbstractAnalysis {
	
	// panel internal fields
	private String m_targetName;
	private String m_xName;
	private String m_yName;

	// external view data
//	private Set<FunctionTerm> m_termSet;
//	private CalculatedPoint[] m_calcPoints;
	private boolean m_showRegModel;

	public ScatterPlot3DGraphPanel(final ScatterPlot3DNodeModel nodeModel) {
		m_termSet = nodeModel.m_termSet;
		m_calcPoints = nodeModel.m_calcPoints;
		m_targetName = nodeModel.getSettings().getColName();
		m_xName = nodeModel.getSettings().getXAxisVarColumn();
		m_yName = nodeModel.getSettings().getYAxisVarColumn();
		m_showRegModel = nodeModel.getSettings().getShowRegModel();
	}


	@Override
	public void init() {
		FunctionTerm xTerm = new FunctionTerm();
		FunctionTerm yTerm = new FunctionTerm();
		int count = 0;
		for (FunctionTerm fnTerm : m_termSet) {
			if(count == 2) {
				break;
			}
			if (fnTerm.getVarName() != null) {
				if (fnTerm.getVarName().equals(m_xName)) {
					xTerm = fnTerm;
					count++;
				}
				if (fnTerm.getVarName().equals(m_yName)) {
					yTerm = fnTerm;
					count++;
				}
			}
		}

		Mapper mapper = new Mapper() {
			@Override
			public double f(double x, double y) {
				double z = 0;
				for (FunctionTerm fnTerm : m_termSet) {
					if (fnTerm.getVarName() != null) {
						if (!fnTerm.getVarName().equals(m_xName) && !fnTerm.getVarName().equals(m_yName)
								&& fnTerm.getIsConstant()) {
							z += fnTerm.evaluateTerm(0);
						} else if (fnTerm.getVarName().equals(m_xName) && !fnTerm.getIsConstant()) {
							z += fnTerm.evaluateTerm(x);
						} else if (fnTerm.getVarName().equals(m_yName) && !fnTerm.getIsConstant()) {
							z += fnTerm.evaluateTerm(y);
						}
					}
				}
				return z;
			}
		};

		double xMin = xTerm.getLowerBound();
		double xMax = xTerm.getUpperBound();

		double yMin = yTerm.getLowerBound();
		double yMax = yTerm.getUpperBound();

		Range xRange = new Range((float) xMin, (float) xMax);
		Range yRange = new Range((float) yMin, (float) yMax);

		int steps = 80;
		chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType());

		if (m_showRegModel) {
			Shape surface = Builder.buildOrthonormal(new OrthonormalGrid(xRange, steps, yRange, steps), mapper);
			surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(),
					surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
			surface.setFaceDisplayed(true);
			surface.setWireframeDisplayed(false);

			chart.getScene().getGraph().add(surface);
		}

		// scatter
		Coord3d[] points = new Coord3d[m_calcPoints.length];
		Color[] colors = new Color[m_calcPoints.length];

		float x = 0;
		float y = 0;
		float z = 0;

		for (int i = 0; i < m_calcPoints.length; i++) {

			x = (float) m_calcPoints[i].getXValue();
			y = (float) m_calcPoints[i].getYValue();
			z = (float) m_calcPoints[i].getZValue();
			Coord3d pointCoord = new Coord3d(x, y, z);
			points[i] = pointCoord;
			colors[i] = new Color((int) (255 * Math.abs(m_calcPoints[i].getPercentError())),
					(int) (255 * (1 - Math.abs(m_calcPoints[i].getPercentError()))), 0);
			if (m_calcPoints[i].isHilited()) {
				colors[i] = new Color(ColorAttr.HILITE.getRed(), ColorAttr.HILITE.getGreen(),
						ColorAttr.HILITE.getBlue(), ColorAttr.HILITE.getAlpha());
			} else if (m_calcPoints[i].isSelected()) {
				colors[i] = new Color(ColorAttr.SELECTED.getRed(), ColorAttr.SELECTED.getGreen(),
						ColorAttr.SELECTED.getBlue(), ColorAttr.SELECTED.getAlpha());
			} else if (m_calcPoints[i].isSelected() && m_calcPoints[i].isHilited()) {
				colors[i] = new Color(ColorAttr.SELECTED_HILITE.getRed(), ColorAttr.SELECTED_HILITE.getGreen(),
						ColorAttr.SELECTED_HILITE.getBlue(), ColorAttr.SELECTED_HILITE.getAlpha());
			}
		}

		Scatter scatter = new Scatter(points, colors);
		scatter.setWidth(5);
		chart.getScene().getGraph().add(scatter);
		chart.getAxeLayout().setXAxeLabel(m_xName);
		chart.getAxeLayout().setYAxeLabel(m_yName);
		chart.getAxeLayout().setZAxeLabel(m_targetName);
	}

}


