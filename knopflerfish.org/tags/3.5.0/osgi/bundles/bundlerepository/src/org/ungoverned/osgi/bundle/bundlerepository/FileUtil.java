/*
 * Oscar Bundle Repository
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
package org.ungoverned.osgi.bundle.bundlerepository;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class FileUtil
{
    public static void downloadSource(
        PrintStream out, PrintStream err,
        String srcURL, String dirStr, boolean extract)
    {
        // Get the file name from the URL.
        String fileName = (srcURL.lastIndexOf('/') > 0)
            ? srcURL.substring(srcURL.lastIndexOf('/') + 1)
            : srcURL;

        try
        {
            out.println("Connecting...");

            File dir = new File(dirStr);
            if (!dir.exists())
            {
                err.println("Destination directory does not exist.");
            }
            File file = new File(dir, fileName);

            OutputStream os = new FileOutputStream(file);
            URLConnection conn = new URL(srcURL).openConnection();
            int total = conn.getContentLength();
            InputStream is = conn.getInputStream();

            if (total > 0)
            {
                out.println("Downloading " + fileName
                    + " ( " + total + " bytes ).");
            }
            else
            {
                out.println("Downloading " + fileName + ".");
            }
            byte[] buffer = new byte[4096];
            int count = 0;
            for (int len = is.read(buffer); len > 0; len = is.read(buffer))
            {
                count += len;
                os.write(buffer, 0, len);
            }

            os.close();
            is.close();

            if (extract)
            {
                is = new FileInputStream(file);
                JarInputStream jis = new JarInputStream(is);
                out.println("Extracting...");
                unjar(jis, dir);
                jis.close();
                file.delete();
            }
        }
        catch (Exception ex)
        {
            err.println(ex);
        }
    }

    public static void unjar(JarInputStream jis, File dir)
        throws IOException
    {
        // Reusable buffer.
        byte[] buffer = new byte[4096];

        // Loop through JAR entries.
        for (JarEntry je = jis.getNextJarEntry();
             je != null;
             je = jis.getNextJarEntry())
        {
            if (je.getName().startsWith("/"))
            {
                throw new IOException("JAR resource cannot contain absolute paths.");
            }

            File target = new File(dir, je.getName());

            // Check to see if the JAR entry is a directory.
            if (je.isDirectory())
            {
                if (!target.exists())
                {
                    if (!target.mkdirs())
                    {
                        throw new IOException("Unable to create target directory: "
                            + target);
                    }
                }
                // Just continue since directories do not have content to copy.
                continue;
            }

            int lastIndex = je.getName().lastIndexOf('/');
            String name = (lastIndex >= 0) ?
                je.getName().substring(lastIndex + 1) : je.getName();
            String destination = (lastIndex >= 0) ?
                je.getName().substring(0, lastIndex) : "";

            // JAR files use '/', so convert it to platform separator.
            destination = destination.replace('/', File.separatorChar);
            copy(jis, dir, name, destination, buffer);
        }
    }

    public static void copy(
        InputStream is, File dir, String destName, String destDir, byte[] buffer)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(dir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int count = 0;
        while ((count = is.read(buffer)) > 0)
        {
            bos.write(buffer, 0, count);
        }
        bos.close();
    }
}