package io.github.blaney83;

import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import io.github.blaney83.mvlrgraph.MVLRGraphNodeModel;
import io.github.blaney83.mvlrgraph.MVLRGraphNodeViewPanel;

/**
 * <code>NodeView</code> for the "ScatterPlot3D" Node.
 * A 3 Dimensional representation of points in space for a given data set.
 *
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeView extends NodeView<ScatterPlot3DNodeModel> {

	// view config variables
	public static final String CFGKEY_GRAPH_TITLE = "graphTitle";
	public static final String CFGKEY_GRAPH_EQUATION = "graphEquation";

	// view defaults
	static final String DEFAULT_GRAPH_TITLE = "Regression Model (plane) & Real Data (scatter)";
	static final boolean DEFAULT_GRAPH_EQUATION = true;

	// settings models
	final SettingsModelString m_graphTitle = new SettingsModelString(CFGKEY_GRAPH_EQUATION, DEFAULT_GRAPH_TITLE);
	
	private ScatterPlot3DNodeViewPanel m_borderLayout;

	protected ScatterPlot3DNodeView(final ScatterPlot3DNodeView nodeModel) {
		super(nodeModel);
		m_borderLayout = new ScatterPlot3DNodeViewPanel(nodeModel);
		setComponent(m_borderLayout);
	}

	@Override
	protected void modelChanged() {

		ScatterPlot3DNodeModel updatedModel = getNodeModel();
		if(updatedModel != null) {
			m_borderLayout.updateView(updatedModel);
			setComponent(m_borderLayout);
		}

	}

	@Override
	protected void onClose() {

	}

	@Override
	protected void onOpen() {

	}

}

