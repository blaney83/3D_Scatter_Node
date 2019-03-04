package io.github.blaney83;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.colors.Color;
import org.jzy3d.plot3d.primitives.Scatter;


public class ScatterPlot3DGraphPanel extends AbstractAnalysis {
	
	private final ScatterPlot3DNodeModel m_nodeModel;

	public ScatterPlot3DGraphPanel(final ScatterPlot3DNodeModel nodeModel) {
		this.m_nodeModel = nodeModel;
	}


	@Override
	public void init() {
		
		Color[] pointColors = new Color[m_nodeModel.getDataPointColorIndicies().length];
		int count = 0;
		for(Short index: m_nodeModel.getDataPointColorIndicies()) {
			pointColors[count] = m_nodeModel.getDataPointColors()[(int) index];
			count ++;
		}
		
		Scatter scatter = new Scatter(m_nodeModel.getDataPoints(), m_nodeModel.getDataPointColors());
		scatter.setWidth(m_nodeModel.getSettings().getDataPointSize());
		chart.getScene().getGraph().add(scatter);
		if(m_nodeModel.getSettings().getPrototypesProvided()) {
			Scatter protoScatter = new Scatter(m_nodeModel.getProtoTypePoints(), new Color(255, 255, 255));
			protoScatter.setWidth(10);
			chart.getScene().getGraph().add(protoScatter);
		}
		chart.getAxeLayout().setXAxeLabel(m_nodeModel.getSettings().getXAxisVarColumn());
		chart.getAxeLayout().setYAxeLabel(m_nodeModel.getSettings().getXAxisVarColumn());
		chart.getAxeLayout().setZAxeLabel(m_nodeModel.getSettings().getXAxisVarColumn());
	}

}


