/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.tools.jarunpacker;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;


public class JUnpackWizard extends JWizard {

  JFile jf;
  JCheckBox doOpenCheckBox;
  JTextPane info;

  long nBytes = 0;

  JPanel compSelPanel;

  JCheckBox compBaseCB;
  JCheckBox compSrcCB;
  JCheckBox compHtdocsCB;
  JLabel    bytesLabel;

  public JUnpackWizard(final ZipFile file,
                       final File destDir,
                       final long nBytes,
                       final int nFiles) {
    super(Main.theMain.windowTitle != null
          ? Main.theMain.windowTitle
          : Strings.fmt("frame_title", Main.theMain.version));

    this.nBytes = nBytes;

    info = new JTextPane();
    info.setContentType("text/html");
    info.setFont(getFont());
    doOpenCheckBox = new JCheckBox(Strings.get("cb_open_dir"), true);

    jf = new JFile(destDir.getAbsolutePath(), true) {
        public void textChanged() {
          setInfoText();
        }
      };


    compSelPanel = new JPanel();
    BoxLayout bl = new BoxLayout(compSelPanel, BoxLayout.Y_AXIS);
    compSelPanel.setLayout(bl);

    bytesLabel = new JLabel();

    final ActionListener compUpdateAction = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          updateBytes(file);
        }
      };

    compBaseCB   = new JCheckBoxTT("install_comp_base")
        {{  addActionListener(compUpdateAction); }};

    compSrcCB    = new JCheckBoxTT("install_comp_src")
        {{  addActionListener(compUpdateAction); }};

    compHtdocsCB = new JCheckBoxTT("install_comp_htdocs")
        {{  addActionListener(compUpdateAction); }};

    updateBytes(file);

    final boolean showCompBaseCB
      = -1 != Main.theMain.optButtons.indexOf("base");
    final boolean showSrcBaseCB
      = -1 != Main.theMain.optButtons.indexOf("source");
    final boolean showHtdocsBaseCB
      = -1 != Main.theMain.optButtons.indexOf("htdocs");

    final int noOfCompSelectors = (showCompBaseCB ? 1 : 0)
      +(showSrcBaseCB ? 1 : 0) +(showHtdocsBaseCB ? 1 : 0);

    if(showCompBaseCB) {
      compSelPanel.add(compBaseCB);
      if (1==noOfCompSelectors) {
        compBaseCB.setEnabled(false);
      }
    }
    if(showSrcBaseCB) {
      compSelPanel.add(compSrcCB);
      if (1==noOfCompSelectors) {
        compSrcCB.setEnabled(false);
      }
    }
    if(showHtdocsBaseCB) {
      compSelPanel.add(compHtdocsCB);
      if (1==noOfCompSelectors) {
        compHtdocsCB.setEnabled(false);
      }
    }
    compSelPanel.add(new JLabel("  "));
    compSelPanel.add(bytesLabel);

    addPage(new JWizardPage("license") {
        {
          StringBuffer lic = new StringBuffer();

          BufferedReader in = null;
          String licName = Main.theMain.licenseResName;
          if(licName == null || "".equals(licName)) {
            licName = Strings.get("license_res_name");
          }
          try {
            in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(licName)));

            String line;
            while(null != (line = in.readLine())) {
              lic.append(line);
              lic.append("\n");
            }
          } catch (Exception e) {
            System.err.println("no license resource file=" + licName);
            e.printStackTrace();
          } finally {
            try {  in.close(); } catch (Exception ignored) {  }
          }

          JTextPane txt = new JTextPane();
          txt.setEditable(false);
          txt.setContentType(licName.endsWith(".html") ? "text/html" : "text/plain");
          String s = Strings.replace(lic.toString(), "$(face)", getFont().getFontName());
          txt.setText(s);

          JScrollPane scroll = new JScrollPane(txt);
          add(scroll, BorderLayout.CENTER);
        }
        public boolean canStepBack() {
          return false;
        }
        public boolean canStepForward() {
          return true;
        }
        public boolean canFinish() {
          return false;
        }
        public String getDescription() {
          return
            Main.theMain.licenseTitle != null
            ? Main.theMain.licenseTitle
            : Strings.get("page_license_title");
        }

      });
    addPage(new JWizardPage("installdir") {
        {
          add(jf, BorderLayout.NORTH);
          setInfoText();
          info.setEditable(false);
          info.setBackground(getBackground());

          add(info, BorderLayout.CENTER);
          if(Util.isWindows()) {
            add(doOpenCheckBox, BorderLayout.SOUTH);
          }
        }
        public boolean canStepBack() {
          return true;
        }
        public boolean canStepForward() {
          return 0<noOfCompSelectors;
        }
        public boolean canFinish() {
          return true;
        }
        public String getDescription() {
          return Strings.get("page_installdir_title");
        }
      });
    addPage(new JWizardPage("installselection") {
        {
          add(compSelPanel, BorderLayout.CENTER);
        }
        public boolean canStepBack() {
          return true;
        }
        public boolean canStepForward() {
          return false;
        }
        public boolean canFinish() {
          return true;
        }
        public String getDescription() {
          return Strings.get("page_installselection_title");
        }
      });
    addPage(new JWizardPage("finish") {
        public boolean canCancel() {
          return false;
        }
        public boolean canStepBack() {
          return false;
        }
        public boolean canStepForward() {
          return false;
        }
        public boolean canFinish() {
          return false;
        }
        public String getDescription() {
          return Strings.get("page_finish_title");
        }
      });
  }

  void updateBytes(ZipFile jarFile) {
    long nBytes = Main.theMain.calcSize(jarFile);
    String bytes = Strings.fmtByte(nBytes);

    bytesLabel.setText(Strings.fmt("comp_size", bytes));
  }

  public boolean isExcluded(String name) {
    boolean excluded = false;

    if(!compBaseCB.isSelected()) {
      if(name.startsWith("osgi/framework.jar") ||
         name.startsWith("osgi/jars/") ||
         name.startsWith("osgi/jars_") ||
         name.startsWith("osgi/test_jars/") ||
         name.endsWith(".xargs")
         ) {
        excluded = true;
      }
    }

    if(!compSrcCB.isSelected()) {
      if(name.startsWith("osgi/bundles") ||
         name.startsWith("osgi/framework/") ||
         name.startsWith("ant/src/") ||
         name.endsWith("build.xml") ||
         name.endsWith(".xargs.in")
         ) {
        excluded = true;
      }
    } else if (name.endsWith(".xargs")) {
      // If source is selected do not exclude .xargs-files; needed to
      // undo the exclude done above when compBaseCB i de-selected for
      // non-generated .xargs-files (harmless for generated files).
      // E.g., this preserves props.xargs
      excluded = false;
    }

    if(!compHtdocsCB.isSelected()) {
      if(name.startsWith("docs")) {
        excluded = true;
      }
    }

    if(!compBaseCB.isSelected() && !compSrcCB.isSelected() &&
       !compHtdocsCB.isSelected()) {
      excluded = true;
    }

    return excluded;
  }

  void setInfoText() {
    String bytes = Strings.fmtByte(nBytes);
    String s = Strings.fmt("fmt_install_info",
                           bytes,
                           Strings.replace(jf.getText(), "/", File.separator),
                           getFont().getFontName());

    s = Strings.replace(s, "$(sep)", File.separator);
    info.setText(s);
  }

  public File getDestDir() {
    return jf.getFile();
  }

  public boolean doOpenDir() {
    return doOpenCheckBox.isSelected();
  }

}

class JCheckBoxTT extends JCheckBox {
  public JCheckBoxTT(String resName) {
    super(Strings.get(resName), true);
    setToolTipText(Strings.get(resName + "_tt"));
  }
}
