package io.github.blaney83;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;


public class ScatterPlot3DGraphPanel extends AbstractAnalysis {
	
	private final ScatterPlot3DNodeModel m_nodeModel;

	public ScatterPlot3DGraphPanel(final ScatterPlot3DNodeModel nodeModel) {
		this.m_nodeModel = nodeModel;
	}


	@Override
	public void init() {
		//this chunkVVVVVVVV
		Color[] pointColors = new Color[m_nodeModel.getDataPointColorIndicies().length];
		int count = 0;
		for(Short index: m_nodeModel.getDataPointColorIndicies()) {
			pointColors[count] = m_nodeModel.getDataPointColors()[(int) index];
			count ++;
		}
		//^^^^^^^^^^^^^^^ should be moved to NodeModel
		chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType());
		Scatter scatter = new Scatter(m_nodeModel.getDataPoints(), pointColors);
		scatter.setWidth(m_nodeModel.getSettings().getDataPointSize());
		chart.getScene().getGraph().add(scatter);
		if(m_nodeModel.getSettings().getPrototypesProvided()) {
			Color protoColor = new Color(m_nodeModel.getSettings().getPrototypePointColor().getRed(), 
					m_nodeModel.getSettings().getPrototypePointColor().getGreen(), 
					m_nodeModel.getSettings().getPrototypePointColor().getBlue(),
					m_nodeModel.getSettings().getPrototypePointColor().getAlpha());
			Scatter protoScatter = new Scatter(m_nodeModel.getPrototypePoints(), protoColor);
			protoScatter.setWidth(m_nodeModel.getSettings().getPrototypePointSize());
			chart.getScene().getGraph().add(protoScatter);
		}
		chart.getAxeLayout().setXAxeLabel(m_nodeModel.getSettings().getXAxisVarColumn());
		chart.getAxeLayout().setYAxeLabel(m_nodeModel.getSettings().getYAxisVarColumn());
		chart.getAxeLayout().setZAxeLabel(m_nodeModel.getSettings().getZAxisVarColumn());
	}

}


