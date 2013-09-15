/*
 * Copyright 2009-2013 the Fess Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.JApplet;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class FileLauncher extends JApplet {

    private static final long serialVersionUID = 1L;

    private String message;

    private transient ResourceBundle resources;

    private File file;

    @Override
    public void paint(final Graphics g) {
        final Dimension d = getSize();
        final Color frontColor = g.getColor();
        g.setColor(getBackground());
        g.fillRect(0, 0, d.width, d.height);

        g.setColor(frontColor);
        g.drawString(message, 0, 20);
    }

    @Override
    public void init() {
        resources = ResourceBundle.getBundle("messages", getLocale());

        try {
            file = getFile();
        } catch (final Exception e) {
            message = e.getLocalizedMessage();
        }
    }

    @Override
    public void start() {
        if (file != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (Desktop.isDesktopSupported()) {
                        final int ret = JOptionPane.showConfirmDialog(
                                getContentPane().getParent(),
                                getMsg("dialog.message"),
                                getMsg("dialog.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.PLAIN_MESSAGE);
                        if (ret == JOptionPane.YES_OPTION) {
                            launch(file);
                        } else if (ret == JOptionPane.NO_OPTION) {
                            message = getMsg("msg.cancel_open_file");
                        }
                    } else {
                        saveFile(file);
                    }
                }

            }).start();
        }

        if (message == null) {
            message = "";
        }
    }

    private String getMsg(final String key) {
        return resources.getString(key);
    }

    private String getMsg(final String key, final Object... arguments) {
        return MessageFormat.format(resources.getString(key), arguments);
    }

    private File getFile() {

        final String uriParam = getRequestParameter("uri");
        if (isBlank(uriParam)) {
            message = getMsg("msg.no_uri");
            return null;
        }

        File targetFile = null;

        final String path = uriParam.replaceFirst("file:/+", "");
        final int pos1 = path.indexOf(':');
        final int pos2 = path.indexOf('/');
        if (pos1 > 0 && pos2 > 0 && pos1 < pos2) {
            // ex. c:/...
            targetFile = new File(path);
        } else {
            targetFile = new File(uriParam.replace("file:", ""));
        }

        if (!targetFile.exists()) {
            message = getMsg("msg.not_found", targetFile.getAbsolutePath());
            return null;
        }

        message = getMsg("msg.open_file", targetFile.getAbsolutePath());

        return targetFile;
    }

    private void launch(final File file) {
        final Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(file);
            message = getMsg("msg.opened_file", file.getAbsolutePath());
        } catch (final Exception e) {
            message = getMsg("msg.save_file", file.getAbsolutePath());
            repaint();
            saveFile(file);
        }
    }

    private void saveFile(final File file) {
        final JFileChooser filechooser = new JFileChooser();
        filechooser.setSelectedFile(new File(file.getName()));

        final int selected = filechooser.showSaveDialog(getContentPane()
                .getParent());
        if (selected == JFileChooser.APPROVE_OPTION) {
            final File outputFile = filechooser.getSelectedFile();
            try {
                copyFile(file, outputFile);
                message = getMsg("msg.save_file", outputFile.getAbsolutePath());
            } catch (final IOException e) {
                message = e.getLocalizedMessage();
            }
        } else if (selected == JFileChooser.CANCEL_OPTION) {
            message = getMsg("msg.cancel_save_dialog");
        } else if (selected == JFileChooser.ERROR_OPTION) {
            message = getMsg("msg.error_save_file");
        }
        repaint();
    }

    private String getRequestParameter(final String key) {
        final String value = getParameter(key);
        if (isBlank(value)) {
            return null;
        }
        return value;
    }

    private boolean isBlank(final String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void copyFile(final File inFile, final File outFile)
            throws IOException {
        final byte[] bytes = new byte[1024 * 8];
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(inFile));
            bos = new BufferedOutputStream(new FileOutputStream(outFile));
            int length = bis.read(bytes);
            while (length != -1) {
                if (length != 0) {
                    bos.write(bytes, 0, length);
                }
                length = bis.read(bytes);
            }
            bos.flush();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

}
