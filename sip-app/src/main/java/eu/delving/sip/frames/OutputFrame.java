/*
 * Copyright 2011, 2012 Delving BV
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

import eu.delving.sip.base.FrameBase;
import eu.delving.sip.model.SipModel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

import static eu.delving.sip.base.SwingHelper.scrollVH;
import static eu.delving.sip.base.SwingHelper.setError;

/**
 * This frame shows the entire output record so that the user can see the "big picture" with only the
 * input tree and the output XML.  This frame also shows the errors as reported by the compile/validate
 * system.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class OutputFrame extends FrameBase {
    private static final Font MONOSPACED = new Font("Monospaced", Font.BOLD, 12);

    public OutputFrame(final SipModel sipModel) {
        super(Which.OUTPUT, sipModel, "Output");
        sipModel.getRecordCompileModel().setEnabled(false);
    }

    @Override
    protected void onOpen(boolean opened) {
        sipModel.getRecordCompileModel().setEnabled(opened);
    }

    @Override
    protected void buildContent(Container content) {
        content.add(createOutputPanel(), BorderLayout.CENTER);
    }

    private JPanel createOutputPanel() {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Output record"));
        final JTextArea area = new JTextArea(sipModel.getRecordCompileModel().getOutputDocument());
        area.setLineWrap(true);
        area.setFont(MONOSPACED);
        area.setWrapStyleWord(true);
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                try {
                    String first = documentEvent.getDocument().getText(0, 1);
                    final boolean error = first.startsWith("#");
                    setError(area, error);
                    area.setCaretPosition(0);
                }
                catch (BadLocationException e) {
                    // who cares
                }
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
            }
        });
        p.add(scrollVH(area));
        return p;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(400, 250);
    }
}
