/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.net.www.protocol.file;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import sun.net.www.ParseUtil;

/**
 * Open an file input stream given a URL.
 *
 * @author James Gosling
 */
// "file"协议处理器
public class Handler extends URLStreamHandler {
    
    // 打开URL处的资源连接
    public synchronized URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }
    
    // 使用指定的代理打开URL处的资源连接
    public synchronized URLConnection openConnection(URL url, java.net.Proxy proxy) throws IOException {
        
        String path;
        String file = url.getFile();
        String host = url.getHost();
        
        // 解析路径中的转义符
        path = ParseUtil.decode(file);
        path = path.replace('/', '\\');
        path = path.replace('|', ':');
        
        if((host == null) || host.equals("") || host.equalsIgnoreCase("localhost") || host.equals("~")) {
            return createFileURLConnection(url, new File(path));
        }
        
        /*
         * attempt to treat this as a UNC path. See 4180841
         */
        path = "\\\\" + host + path;
        File f = new File(path);
        if(f.exists()) {
            // 如果URL指向的资源存在于本地，则返回"file"协议下对应的URL连接
            return createFileURLConnection(url, f);
        }
        
        /*
         * Now attempt an ftp connection.
         */
        URLConnection uc;
        URL newurl;
        
        try {
            // 构造"ftp"资源连接
            newurl = new URL("ftp", host, file + (url.getRef() == null ? "" : "#" + url.getRef()));
            if(proxy != null) {
                uc = newurl.openConnection(proxy);
            } else {
                uc = newurl.openConnection();
            }
        } catch(IOException e) {
            uc = null;
        }
        
        if(uc == null) {
            throw new IOException("Unable to connect to: " + url.toExternalForm());
        }
        
        return uc;
    }
    
    // 基于url解析spec中指定范围内的字符串，从spec中解析到的URL组件会覆盖到url中以形成新的URL返回，如果spec是相对路径，则会追加在url的原有路径上
    protected void parseURL(URL u, String spec, int start, int limit) {
        /*
         * Ugly backwards compatibility. Flip any file separator
         * characters to be forward slashes. This is a nop on Unix
         * and "fixes" win32 file paths. According to RFC 2396,
         * only forward slashes may be used to represent hierarchy
         * separation in a URL but previous releases unfortunately
         * performed this "fixup" behavior in the file URL parsing code
         * rather than forcing this to be fixed in the caller of the URL
         * class where it belongs. Since backslash is an "unwise"
         * character that would normally be encoded if literally intended
         * as a non-seperator character the damage of veering away from the
         * specification is presumably limited.
         */
        super.parseURL(u, spec.replace(File.separatorChar, '/'), start, limit);
    }
    
    /**
     * Template method to be overriden by Java Plug-in. [stanleyh]
     */
    // 构造并返回指定url和file对应的"file"资源连接
    protected URLConnection createFileURLConnection(URL url, File file) {
        return new FileURLConnection(url, file);
    }
    
    /**
     * Compares the host components of two URLs.
     *
     * @param u1 the URL of the first host to compare
     * @param u2 the URL of the second host to compare
     *
     * @return {@code true} if and only if they
     * are equal, {@code false} otherwise.
     */
    // 比较两个URL的host组件是否相同(未设置host等价于使用了"localhost")
    protected boolean hostsEqual(URL u1, URL u2) {
        /*
         * Special case for file: URLs
         * per RFC 1738 no hostname is equivalent to 'localhost'
         * i.e. file:///path is equal to file://localhost/path
         */
        String s1 = u1.getHost();
        String s2 = u2.getHost();
        if("localhost".equalsIgnoreCase(s1) && (s2 == null || "".equals(s2))) {
            return true;
        }
    
        if("localhost".equalsIgnoreCase(s2) && (s1 == null || "".equals(s1))) {
            return true;
        }
    
        return super.hostsEqual(u1, u2);
    }
    
    // 返回指定URL的host
    private String getHost(URL url) {
        String host = url.getHost();
        if(host == null) {
            host = "";
        }
        return host;
    }
    
}
