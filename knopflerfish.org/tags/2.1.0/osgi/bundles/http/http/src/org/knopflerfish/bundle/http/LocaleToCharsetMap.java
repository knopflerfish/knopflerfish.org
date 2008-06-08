/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.http;

import java.util.Hashtable;
import java.util.Locale;

public class LocaleToCharsetMap {

    private static final Hashtable map = new Hashtable();

    static {
        map.put("ar", "8859_6");
        map.put("be", "8859_5");
        map.put("bg", "8859_5");
        map.put("ca", "8859_1");
        map.put("cs", "8859_2");
        map.put("da", "8859_1");
        map.put("de", "8859_1");
        map.put("el", "8859_7");
        map.put("en", "8859_1");
        map.put("es", "8859_1");
        map.put("et", "8859_1");
        map.put("fi", "8859_1");
        map.put("fr", "8859_1");
        map.put("hr", "8859_2");
        map.put("hu", "8859_2");
        map.put("is", "8859_1");
        map.put("it", "8859_1");
        map.put("iw", "8859_8");
        map.put("ja", "SJIS");
        // map.put("ko", "EUC-KR"); Not available according to SUN 1.1 supported
        map.put("lt", "8859_2");
        map.put("lv", "8859_2");
        map.put("mk", "8859_5");
        map.put("nl", "8859_1");
        map.put("no", "8859_1");
        map.put("pl", "8859_2");
        map.put("pt", "8859_1");
        map.put("ro", "8859_2");
        map.put("ru", "8859_5");
        map.put("sh", "8859_5");
        map.put("sk", "8859_2");
        map.put("sl", "8859_2");
        map.put("sq", "8859_2");
        map.put("sr", "8859_5");
        map.put("sv", "8859_1");
        map.put("tr", "8859_9");
        map.put("uk", "8859_5");
        map.put("zh", "GB2312");
        map.put("zh_TW", "Big5");
    }

    public static String getCharset(final Locale locale) {

        String charset;

        charset = (String) map.get(locale.toString());
        if (charset != null)
            return charset;

        charset = (String) map.get(locale.getLanguage());
        if (charset != null)
            return charset;

        return "8859_1";
    }
}
