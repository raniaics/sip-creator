/*
 * Copyright 2011 DELVING BV
 *
 * Licensed under the EUPL, Version 1.0 or as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.delving.sip.frames;

import eu.delving.metadata.MappingFunction;
import eu.delving.metadata.RecMapping;
import eu.delving.sip.base.Exec;
import eu.delving.sip.base.FrameBase;
import eu.delving.sip.model.FunctionCompileModel;
import eu.delving.sip.model.MappingModel;
import eu.delving.sip.model.SipModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This frame shows the entire builder that is responsible for transforming the input to output,
 * as well as the constants and dictionaries that it may have.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class FunctionFrame extends FrameBase {
    private static final Pattern FUNCTION_NAME = Pattern.compile("[a-z]+[a-zA-z]*");
    private static final Font MONOSPACED = new Font("Monospaced", Font.BOLD, 18);
    private FunctionListModel functionListModel = new FunctionListModel();
    private JList functionList = new JList(functionListModel);
    private JTextArea inputArea = new JTextArea();
    private JTextArea codeArea = new JTextArea();
    private JTextArea outputArea = new JTextArea();

    public FunctionFrame(JDesktopPane desktop, SipModel swipModel) {
        super(desktop, swipModel, "Functions", false);
        inputArea.setFont(MONOSPACED);
        codeArea.setFont(MONOSPACED);
        outputArea.setFont(MONOSPACED);
        wireUp();
    }

    private void wireUp() {
        functionList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        functionList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                Exec.work(new Runnable() {
                    @Override
                    public void run() {
                        String name = (String) functionList.getSelectedValue();
                        sipModel.getFunctionCompileModel().setFunctionName(name);
                    }
                });
            }
        });
        inputArea.setDocument(sipModel.getFunctionCompileModel().getInputDocument());
        codeArea.setDocument(sipModel.getFunctionCompileModel().getCodeDocument());
        outputArea.setDocument(sipModel.getFunctionCompileModel().getOutputDocument());
        sipModel.getMappingModel().addSetListener(new MappingModel.SetListener() {
            @Override
            public void recMappingSet(final MappingModel mappingModel) {
                Exec.swing(new Runnable() {
                    @Override
                    public void run() {
                        functionListModel.setList(mappingModel.getRecMapping());
                    }
                });
            }
        });
        sipModel.getFunctionCompileModel().addListener(new ModelStateListener());
    }

    @Override
    protected void buildContent(Container content) {
        JPanel center = new JPanel(new GridLayout(0, 1));
        center.add(createInputPanel());
        center.add(createCodePanel());
        center.add(createOutputPanel());
        content.add(center, BorderLayout.CENTER);
        content.add(createListPanel(), BorderLayout.WEST);
    }
    
    private JPanel createListPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Functions"));
        p.add(scroll(functionList));
        p.add(new JButton(new CreateAction()), BorderLayout.SOUTH);
        return p;
    }

    private JPanel createInputPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Input Lines"));
        p.add(scroll(inputArea));
        return p;
    }

    private JPanel createCodePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Function Code"));
        p.add(scroll(codeArea));
        return p;
    }

    private JPanel createOutputPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Output Lines"));
        p.add(scroll(outputArea));
        return p;
    }
    
    private class CreateAction extends AbstractAction {

        private CreateAction() {
            super("Create a new function");
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            final String name = JOptionPane.showInputDialog(FunctionFrame.this, "Please enter the function name");
            if (name != null) {
                if (!FUNCTION_NAME.matcher(name).matches()) {
                    JOptionPane.showMessageDialog(FunctionFrame.this, "Sorry, the name must be of the form 'aaaaa' or 'aaaaAaaa'");
                    return;
                }
                if (functionListModel.contains(name)) {
                    JOptionPane.showMessageDialog(FunctionFrame.this, "Sorry, but this function name already exists");
                    return;
                }
                Exec.work(new Runnable() {
                    @Override
                    public void run() {
                        MappingFunction mappingFunction = sipModel.getMappingModel().getRecMapping().getFunction(name.trim());
                        sipModel.getMappingModel().notifyFunctionChanged(mappingFunction);
                    }
                });
            }
        }
    }

    private class FunctionListModel extends AbstractListModel {
        private List<String> names = new ArrayList<String>();

        public void setList(RecMapping recMapping) {
            if (!names.isEmpty()) {
                int size = getSize();
                names.clear();
                fireIntervalRemoved(this, 0, size);
            }
            for (MappingFunction function : recMapping.getFunctions()) names.add(function.name);
            Collections.sort(names);
            if (!names.isEmpty()) {
                fireIntervalRemoved(this, 0, getSize());
            }
        }

        @Override
        public int getSize() {
            return names.size();
        }

        @Override
        public Object getElementAt(int i) {
            return names.get(i);
        }

        public boolean contains(String name) {
            return names.contains(name);
        }
    }

    private class ModelStateListener implements FunctionCompileModel.Listener {

        @Override
        public void stateChanged(final FunctionCompileModel.State state) {
            Exec.swing(new Runnable() {

                @Override
                public void run() {
                    switch (state) {
                        case ORIGINAL:
                            codeArea.setBackground(new Color(1.0f, 1.0f, 1.0f));
                            break;
                        case EDITED:
                            codeArea.setBackground(new Color(1.0f, 1.0f, 0.9f));
                            break;
                        case ERROR:
                            codeArea.setBackground(new Color(1.0f, 0.9f, 0.9f));
                            break;
                    }
                }
            });
        }
    }


}
