/*
 * Copyright 2022 Matthieu Casanova
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kpouer.roadwork.ui.opendatainfo;

import com.kpouer.mapview.MapView;
import com.kpouer.mapview.marker.Circle;
import com.kpouer.mapview.tile.DefaultTileServer;
import com.kpouer.roadwork.opendata.OpendataService;
import com.kpouer.roadwork.opendata.json.model.Metadata;
import com.kpouer.roadwork.service.LocalizationService;
import com.kpouer.roadwork.service.OpendataServiceManager;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Matthieu Casanova
 */
public class OpendataInformationDialog extends JDialog {
    private final OpendataServiceManager opendataServiceManager;
    private final MapView mapView;
    private final OpendataInformationPanel opendataInformationPanel;

    public OpendataInformationDialog(JFrame parent,
                                     OpendataServiceManager opendataServiceManager,
                                     @Qualifier("WazeTileServer") DefaultTileServer tileServer,
                                     LocalizationService localizationService) {
        super(parent);
        this.opendataServiceManager = opendataServiceManager;
        mapView = new MapView(tileServer);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JTree licenceTree = buildLicenceTree();
        licenceTree.setPreferredSize(new Dimension(150, 0));
        opendataInformationPanel = new OpendataInformationPanel(localizationService);
        opendataInformationPanel.setPreferredSize(new Dimension(150, 0));
        licenceTree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = leaf.getUserObject();
            if (userObject instanceof Metadata) {
                var metadata = (Metadata) userObject;
                opendataInformationPanel.setMetadata(metadata);
            }
        });
        splitPane.setLeftComponent(new JScrollPane(licenceTree));
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setLeftComponent(opendataInformationPanel);
        rightSplit.setRightComponent(mapView);
        splitPane.setRightComponent(rightSplit);

        getContentPane().add(splitPane);
        mapView.setZoom(3);
        setSize(800, 480);
    }

    private JTree buildLicenceTree() {
        var services = opendataServiceManager.getServices();
        var root = new DefaultMutableTreeNode();
        Map<String, DefaultMutableTreeNode> countryNodes = new HashMap<>();

        var nodes = services
                .parallelStream()
                .map(opendataServiceManager::getOpendataService)
                .map(OpendataService::getMetadata)
                .map(DefaultMutableTreeNode::new)
                .toList();

        for (var node : nodes) {
            var metadata = (Metadata) node.getUserObject();
            mapView.addMarker(new Circle(metadata.getCenter().getLat(),
                    metadata.getCenter().getLon(), 5, Color.RED));

            String country = metadata.getCountry();
            var countryNode = countryNodes.get(country);
            if (countryNode == null) {
                countryNode = new DefaultMutableTreeNode(country);
                countryNodes.put(country, countryNode);
            }
            countryNode.add(node);
        }
        countryNodes.keySet().stream().sorted()
                .map(countryNodes::get)
                .forEach(root::add);
        return new JTree(root);
    }
}