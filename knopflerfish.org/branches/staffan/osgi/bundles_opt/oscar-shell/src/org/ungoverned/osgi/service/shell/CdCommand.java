/*
 * Oscar Shell Service
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.ungoverned.osgi.service.shell;

/**
 * This interface defines the <tt>cd</tt> command service interface for the
 * Oscar shell service. The <tt>cd</tt> command does not really change the
 * directory of the shell, rather it maintains a base URL for
 * simplifying URL entry.
 * <p>
 * For example, if the base URL is <tt>http://www.foo.com/<tt> and you
 * try to install a bundle <tt>foo.jar</tt>, the actual URL will be
 * expanded to <tt>http://www.foo.com/foo.jar</tt>. Any bundles wishing
 * to retrieve or set the current directory of the shell can use this
 * service interface.
**/
public interface CdCommand extends Command
{
    /**
     * Property used to configure the base URL.
    **/
    public static final String BASE_URL_PROPERTY = "oscar.shell.baseurl";

    /**
     * Returns the current <i>directory</i> of the Oscar shell service.
     * @return the current shell directory.
    **/
    public String getBaseURL();

    /**
     * Sets the current <i>directory</i> of the Oscar shell service.
     * @param s the new value for the base URL.
    **/
    public void setBaseURL(String s);
}
