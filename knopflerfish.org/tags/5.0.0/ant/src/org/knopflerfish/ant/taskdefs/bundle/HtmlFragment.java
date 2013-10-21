/*
 * Copyright (c) 2010, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.ant.taskdefs.bundle;

import java.io.File;


/**
 * <p>An Html formated text fragment the the MakeHTMLTask can be
 * configured with.</p>
 *
 * <p>When <code>$(name)</code> is found in the document to be generated
 * that string will be replaced with the contents of the file given by
 * <code>fromFile</code>.</p>
 *
 * <p>When <code>$(name_LINK)</code> is found in the document to be
 * generated that string will be replaced with a link to the
 * fragment. The title of the link is given by
 * <code>linkText</code>.</p>
 *
 * @author Gunnar Ekolin
 */
public class HtmlFragment {

  private String name;
  private File fromFile;
  private String linkText;

  /**
   * @param name the name that is the substitution key for this fragment.
   */
  public void setName(final String name)
  {
    this.name = name;
  }
  /**
   * @return the name of this fragment.
   */
  public String getName()
  {
    return name;
  }

  /**
   * @param fromFile the file holding the substitution text.
   */
  public void setFromFile(final File fromFile) {
    this.fromFile = fromFile;
  }
  /**
   * @return the file to load the fragment from.
   */
  public File getFromFile() {
    return fromFile;
  }

  /**
   * @param linkText the clickable text of the link.
   */
  public void setLinkText(final String linkText)
  {
    this.linkText = linkText;
  }
  /**
   * @return the link text.
   */
  public String getLinkText()
  {
    return linkText;
  }

} // HtmlFragment
