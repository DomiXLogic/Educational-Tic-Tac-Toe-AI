package com.ai.tictactoe.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.net.URI;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;

public final class HtmlContentDialog extends JDialog {

    public HtmlContentDialog(JFrame owner, String title, String html, int width, int height) {
        super(owner, title, false);
        setLayout(new BorderLayout());

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setText(html);
        editorPane.setCaretPosition(0);
        editorPane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null) {
                openUrl(event.getURL().toString());
            }
        });

        JScrollPane scrollPane = new JScrollPane(editorPane);
        add(scrollPane, BorderLayout.CENTER);

        setPreferredSize(new Dimension(width, height));
        pack();
        setLocationRelativeTo(owner);
    }

    private void openUrl(String url) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {
            // Intentionally silent: the dialog content remains readable even if
            // external browser navigation is unavailable.
        }
    }
}
