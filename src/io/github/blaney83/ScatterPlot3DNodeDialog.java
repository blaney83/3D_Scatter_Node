package io.github.blaney83;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the "ScatterPlot3D" Node.
 * A 3 Dimensional representation of points in space for a given data set.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring ScatterPlot3D node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ScatterPlot3DNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    ScatterPlot3DNodeModel.CFGKEY_COUNT,
                    ScatterPlot3DNodeModel.DEFAULT_COUNT,
                    Integer.MIN_VALUE, Integer.MAX_VALUE),
                    "Counter:", /*step*/ 1, /*componentwidth*/ 5));
                    
    }
}

