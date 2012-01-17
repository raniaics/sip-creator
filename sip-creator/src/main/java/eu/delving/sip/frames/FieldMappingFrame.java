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

import eu.delving.sip.base.Exec;
import eu.delving.sip.base.FrameBase;
import eu.delving.sip.base.Utility;
import eu.delving.sip.menus.EditHistory;
import eu.delving.sip.model.CompileModel;
import eu.delving.sip.model.SipModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;

/**
 * Refining the mapping interactively
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class FieldMappingFrame extends FrameBase {
    private JTextArea groovyCodeArea;
    private JTextArea outputArea;
    private JEditorPane helpView;
    private DictionaryPanel dictionaryPanel;
    private EditHistory editHistory;

    public FieldMappingFrame(JDesktopPane desktop, SipModel sipModel, final EditHistory editHistory) {
        super(desktop, sipModel, "Field Mapping", false);
        this.editHistory = editHistory;
        try {
            helpView = new JEditorPane(getClass().getResource("/groovy-help.html"));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        dictionaryPanel = new DictionaryPanel(sipModel.getCreateModel());
        groovyCodeArea = new JTextArea(sipModel.getFieldCompileModel().getCodeDocument());
        groovyCodeArea.setFont(new Font("Monospaced", Font.BOLD, 12));
        groovyCodeArea.setTabSize(3);
        groovyCodeArea.getDocument().addUndoableEditListener(editHistory);
        groovyCodeArea.addFocusListener(
                new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent focusEvent) {
                        editHistory.setTarget(groovyCodeArea);
                    }

                    @Override
                    public void focusLost(FocusEvent focusEvent) {
                        editHistory.setTarget(null);
                    }
                }
        );
        outputArea = new JTextArea(sipModel.getFieldCompileModel().getOutputDocument());
        Utility.attachUrlLauncher(outputArea);
        wireUp();
    }

    @Override
    protected void onOpen(boolean opened) {
        sipModel.getFieldCompileModel().setEnabled(opened);
    }

    @Override
    protected void buildContent(Container content) {
        add(createPanel(), BorderLayout.CENTER);
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(createGroovyPanel());
        p.add(createOutputPanel());
        return p;
    }

    private JComponent createGroovyPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Code", scroll(groovyCodeArea));
        tabs.addTab("Dictionary", dictionaryPanel);
        tabs.addTab("Help", scroll(helpView));
        return tabs;
    }

    private JPanel createOutputPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Output Record"));
        p.add(scroll(outputArea), BorderLayout.CENTER);
        p.add(new JLabel("Note: URLs can be launched by double-clicking them.", JLabel.CENTER), BorderLayout.SOUTH);
        return p;
    }

    private void wireUp() {
        sipModel.getFieldCompileModel().getCodeDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setCode();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setCode();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setCode();
            }

            private void setCode() {
                Exec.work(new Runnable() {
                    @Override
                    public void run() {
                        sipModel.getFieldCompileModel().setCode(groovyCodeArea.getText());
                    }
                });
            }
        });
        groovyCodeArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                Exec.work(new Runnable() {
                    @Override
                    public void run() {
                        sipModel.getRecordCompileModel().refreshCode(); // todo: somebody else do this?
                    }
                });
            }
        });
        sipModel.getFieldCompileModel().addListener(new ModelStateListener());
        outputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                outputArea.setCaretPosition(0);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
            }
        });

    }

    private class ModelStateListener implements CompileModel.Listener {

        @Override
        public void stateChanged(final CompileModel.State state) {
            Exec.swing(new Runnable() {

                @Override
                public void run() {
                    switch (state) {
                        case ORIGINAL:
                            editHistory.discardAllEdits();
                            // fall through
                        case SAVED:
                        case UNCOMPILED:
                            groovyCodeArea.setBackground(new Color(1.0f, 1.0f, 1.0f));
                            break;
                        case EDITED:
                            groovyCodeArea.setBackground(new Color(1.0f, 1.0f, 0.9f));
                            break;
                        case ERROR:
                            groovyCodeArea.setBackground(new Color(1.0f, 0.9f, 0.9f));
                            break;
                        case COMMITTED:
                            groovyCodeArea.setBackground(new Color(0.9f, 1.0f, 0.9f));
                            break;
                    }
                }
            });
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(400, 250);
    }
    
}
