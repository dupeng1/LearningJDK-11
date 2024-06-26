/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import sun.net.www.MessageHeader;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

/**
 * The abstract class {@code URLConnection} is the superclass
 * of all classes that represent a communications link between the
 * application and a URL. Instances of this class can be used both to
 * read from and to write to the resource referenced by the URL.
 *
 * <p>
 * In general, creating a connection to a URL is a multistep process:
 * <ol>
 * <li>The connection object is created by invoking the
 *     {@link URL#openConnection() openConnection} method on a URL.
 * <li>The setup parameters and general request properties are manipulated.
 * <li>The actual connection to the remote object is made, using the
 *    {@link #connect() connect} method.
 * <li>The remote object becomes available. The header fields and the contents
 *     of the remote object can be accessed.
 * </ol>
 * <p>
 * The setup parameters are modified using the following methods:
 * <ul>
 *   <li>{@code setAllowUserInteraction}
 *   <li>{@code setDoInput}
 *   <li>{@code setDoOutput}
 *   <li>{@code setIfModifiedSince}
 *   <li>{@code setUseCaches}
 * </ul>
 * <p>
 * and the general request properties are modified using the method:
 * <ul>
 *   <li>{@code setRequestProperty}
 * </ul>
 * <p>
 * Default values for the {@code AllowUserInteraction} and
 * {@code UseCaches} parameters can be set using the methods
 * {@code setDefaultAllowUserInteraction} and
 * {@code setDefaultUseCaches}.
 * <p>
 * Each of the above {@code set} methods has a corresponding
 * {@code get} method to retrieve the value of the parameter or
 * general request property. The specific parameters and general
 * request properties that are applicable are protocol specific.
 * <p>
 * The following methods are used to access the header fields and
 * the contents after the connection is made to the remote object:
 * <ul>
 *   <li>{@code getContent}
 *   <li>{@code getHeaderField}
 *   <li>{@code getInputStream}
 *   <li>{@code getOutputStream}
 * </ul>
 * <p>
 * Certain header fields are accessed frequently. The methods:
 * <ul>
 *   <li>{@code getContentEncoding}
 *   <li>{@code getContentLength}
 *   <li>{@code getContentType}
 *   <li>{@code getDate}
 *   <li>{@code getExpiration}
 *   <li>{@code getLastModified}
 * </ul>
 * <p>
 * provide convenient access to these fields. The
 * {@code getContentType} method is used by the
 * {@code getContent} method to determine the type of the remote
 * object; subclasses may find it convenient to override the
 * {@code getContentType} method.
 * <p>
 * In the common case, all of the pre-connection parameters and
 * general request properties can be ignored: the pre-connection
 * parameters and request properties default to sensible values. For
 * most clients of this interface, there are only two interesting
 * methods: {@code getInputStream} and {@code getContent},
 * which are mirrored in the {@code URL} class by convenience methods.
 * <p>
 * More information on the request properties and header fields of
 * an {@code http} connection can be found at:
 * <blockquote><pre>
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">http://www.ietf.org/rfc/rfc2616.txt</a>
 * </pre></blockquote>
 *
 * Invoking the {@code close()} methods on the {@code InputStream} or {@code OutputStream} of an
 * {@code URLConnection} after a request may free network resources associated with this
 * instance, unless particular protocol specifications specify different behaviours
 * for it.
 *
 * @author James Gosling
 * @see java.net.URL#openConnection()
 * @see java.net.URLConnection#connect()
 * @see java.net.URLConnection#getContent()
 * @see java.net.URLConnection#getContentEncoding()
 * @see java.net.URLConnection#getContentLength()
 * @see java.net.URLConnection#getContentType()
 * @see java.net.URLConnection#getDate()
 * @see java.net.URLConnection#getExpiration()
 * @see java.net.URLConnection#getHeaderField(int)
 * @see java.net.URLConnection#getHeaderField(java.lang.String)
 * @see java.net.URLConnection#getInputStream()
 * @see java.net.URLConnection#getLastModified()
 * @see java.net.URLConnection#getOutputStream()
 * @see java.net.URLConnection#setAllowUserInteraction(boolean)
 * @see java.net.URLConnection#setDefaultUseCaches(boolean)
 * @see java.net.URLConnection#setDoInput(boolean)
 * @see java.net.URLConnection#setDoOutput(boolean)
 * @see java.net.URLConnection#setIfModifiedSince(long)
 * @see java.net.URLConnection#setRequestProperty(java.lang.String, java.lang.String)
 * @see java.net.URLConnection#setUseCaches(boolean)
 * @since 1.0
 */
// URL资源连接的公共父类
// 指向URL指定资源的活动连接，与URL相比，它对与服务器的交互提供了更多控制，可以检查服务器发送的首部，并做出响应，可以设置请求服务器的首部，
// 可以用POST、PUT、其他HTTP方法向服务器发送数据

/**
 * URL和URLConnection这两个类最大的不同在于
 * URLConnection提供了对HTTP首部的访问
 * URLConnection可以配置发送给服务器的请求参数
 * URLConnection出来读取服务器的数据外，还可以向服务器写入数据
 */
public abstract class URLConnection {
    
    private static final String contentClassPrefix = "sun.net.www.content";
    private static final String contentPathProp = "java.content.handler.pkgs";
    
    private static final ConcurrentHashMap<String, Boolean> defaultCaching = new ConcurrentHashMap<>();
    
    /**
     * @since 1.1
     */
    // (全局)文件名到MIME类型的映射
    private static volatile FileNameMap fileNameMap;
    
    /**
     * The ContentHandler factory.
     */
    // (全局)资源内容句柄工厂
    private static volatile ContentHandlerFactory factory;
    
    // (全局)缓存"content-type"(MIME类型)到资源内容句柄的映射
    private static final Hashtable<String, ContentHandler> handlers = new Hashtable<>();
    
    private static boolean defaultAllowUserInteraction = false;
    
    private static volatile boolean defaultUseCaches = true;
    
    /**
     * The URL represents the remote object on the World Wide Web to
     * which this connection is opened.
     * <p>
     * The value of this field can be accessed by the
     * {@code getURL} method.
     * <p>
     * The default value of this variable is the value of the URL
     * argument in the {@code URLConnection} constructor.
     *
     * @see java.net.URLConnection#getURL()
     * @see java.net.URLConnection#url
     */
    // 指向资源的URL
    // 构造函数会在创建URLConnection时设置这个字段，此后不能再改变
    protected URL url;
    
    /**
     * @since 1.6
     */
    // 当前协议下的发起连接的请求信息
    private MessageHeader requests;
    
    /**
     * If {@code false}, this connection object has not created a
     * communications link to the specified URL. If {@code true},
     * the communications link has been established.
     */
    // 是否已建立连接
    // 如果连接关闭则字段为false，由于在创建一个新的URLConnection对象时连接尚未打开，所以其初始值为false
    // 任何导致URLConnection连接的方法都会将该变量设置为true，connect()、getInputstream、getOutputstream
    // 任何导致URLConnection断开连接的方法会将该变量设置为false，例如子类HttpURLConnection的disconnect()方法
    // 如果要派生URLConnection子类来编写一个协议处理器，需要在连接时将该变量设置为true，在连接关闭时将该变量设置为false
    protected boolean connected = false;
    
    /**
     * This variable is set by the {@code setDoInput} method. Its
     * value is returned by the {@code getDoInput} method.
     * <p>
     * A URL connection can be used for input and/or output. Setting the
     * {@code doInput} flag to {@code true} indicates that
     * the application intends to read data from the URL connection.
     * <p>
     * The default value of this field is {@code true}.
     *
     * @see java.net.URLConnection#getDoInput()
     * @see java.net.URLConnection#setDoInput(boolean)
     */
    // 是否允许从当前URL资源连接读取数据，默认为true；在某些协议的URL下，需要使用该参数做限制
    protected boolean doInput = true;
    
    /**
     * This variable is set by the {@code setDoOutput} method. Its
     * value is returned by the {@code getDoOutput} method.
     * <p>
     * A URL connection can be used for input and/or output. Setting the
     * {@code doOutput} flag to {@code true} indicates
     * that the application intends to write data to the URL connection.
     * <p>
     * The default value of this field is {@code false}.
     *
     * @see java.net.URLConnection#getDoOutput()
     * @see java.net.URLConnection#setDoOutput(boolean)
     */
    // 是否允许向当前URL资源连接写入数据，默认为false；在某些协议的URL下，需要使用该参数做限制
    // 如果程序需要使用POST方法向服务器发送数据，可以通过从URLConnection获取输出流来完成
    protected boolean doOutput = false;
    
    /**
     * If {@code true}, this {@code URL} is being examined in
     * a context in which it makes sense to allow user interactions such
     * as popping up an authentication dialog. If {@code false},
     * then no user interaction is allowed.
     * <p>
     * The value of this field can be set by the
     * {@code setAllowUserInteraction} method.
     * Its value is returned by the
     * {@code getAllowUserInteraction} method.
     * Its default value is the value of the argument in the last invocation
     * of the {@code setDefaultAllowUserInteraction} method.
     *
     * @see java.net.URLConnection#getAllowUserInteraction()
     * @see java.net.URLConnection#setAllowUserInteraction(boolean)
     * @see java.net.URLConnection#setDefaultAllowUserInteraction(boolean)
     */
    protected boolean allowUserInteraction = defaultAllowUserInteraction;

    // false会绕过本地缓存，重新从服务器下载文件
    // 如果有缓存，该变量确定了是否可以使用缓存，默认值为true，表示将使用缓存；false表示不适用缓存
    /**
     * If {@code true}, the protocol is allowed to use caching
     * whenever it can. If {@code false}, the protocol must always
     * try to get a fresh copy of the object.
     * <p>
     * This field is set by the {@code setUseCaches} method. Its
     * value is returned by the {@code getUseCaches} method.
     * <p>
     * Its default value is the value given in the last invocation of the
     * {@code setDefaultUseCaches} method.
     * <p>
     * The default setting may be overridden per protocol with
     * {@link #setDefaultUseCaches(String, boolean)}.
     *
     * @see java.net.URLConnection#setUseCaches(boolean)
     * @see java.net.URLConnection#getUseCaches()
     * @see java.net.URLConnection#setDefaultUseCaches(boolean)
     */
    protected boolean useCaches;

    // 客户端会保留以前获取的文档缓存，如果再次要求相同文档，可以从缓存种获取，不过在最后一次获取这个文档后，服务器上的文档可能改变
    // 要判断是否有变化，就要询问服务器，客户端可以在客户端请求的HTTP首部中包括一个If-Modified-Since，这个首部包含一个日期和时间
    // 如果文档在这个时间之后有所改变，服务器就发送该文档，否则就不发送，一般情况下这个时间是最后获得文档的时间
    // 该字段指示了放置在If-Modified-Since首部字段中的日期
    /**
     * Some protocols support skipping the fetching of the object unless
     * the object has been modified more recently than a certain time.
     * <p>
     * A nonzero value gives a time as the number of milliseconds since
     * January 1, 1970, GMT. The object is fetched only if it has been
     * modified more recently than that time.
     * <p>
     * This variable is set by the {@code setIfModifiedSince}
     * method. Its value is returned by the
     * {@code getIfModifiedSince} method.
     * <p>
     * The default value of this field is {@code 0}, indicating
     * that the fetching must always occur.
     *
     * @see java.net.URLConnection#getIfModifiedSince()
     * @see java.net.URLConnection#setIfModifiedSince(long)
     */
    protected long ifModifiedSince = 0;
    
    /**
     * @since 1.5
     */
    private int connectTimeout;
    
    private int readTimeout;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    protected URLConnection(URL url) {
        this.url = url;
        
        if(url == null) {
            this.useCaches = defaultUseCaches;
        } else {
            this.useCaches = getDefaultUseCaches(url.getProtocol());
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 连接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens a communications link to the resource referenced by this
     * URL, if such a connection has not already been established.
     * <p>
     * If the {@code connect} method is called when the connection
     * has already been opened (indicated by the {@code connected}
     * field having the value {@code true}), the call is ignored.
     * <p>
     * URLConnection objects go through two phases: first they are
     * created, then they are connected.  After being created, and
     * before being connected, various options can be specified
     * (e.g., doInput and UseCaches).  After connecting, it is an
     * error to try to set them.  Operations that depend on being
     * connected, like getContentLength, will implicitly perform the
     * connection, if necessary.
     *
     * @throws SocketTimeoutException if the timeout expires before
     *                                the connection can be established
     * @throws IOException            if an I/O error occurs while opening the
     *                                connection.
     * @see java.net.URLConnection#connected
     * @see #getConnectTimeout()
     * @see #setConnectTimeout(int)
     */
    // 连接到当前URL指向的资源
    // 第一次构建URLConnection，它是未连接的，没有socket连接这两个主机，方法在本地和远程主机之间建立一个连接（一般使用TCP socket），并未实际发送请求
    // 对于getInputStream、getContent、getHeaderField和其他要求打开连接的方法，如果连接尚未打开，会调用connect方法
    public abstract void connect() throws IOException;
    
    /*▲ 连接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an input stream that reads from this open connection.
     *
     * A SocketTimeoutException can be thrown when reading from the
     * returned input stream if the read timeout expires before data
     * is available for read.
     *
     * @return an input stream that reads from this open connection.
     *
     * @throws IOException             if an I/O error occurs while creating the input stream.
     * @throws UnknownServiceException if the protocol does not support input.
     * @see #setReadTimeout(int)
     * @see #getReadTimeout()
     */
    // 返回指向当前URL资源的输入流，可以从中读取数据
    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support input");
    }
    
    /**
     * Returns an output stream that writes to this connection.
     *
     * @return an output stream that writes to this connection.
     *
     * @throws IOException             if an I/O error occurs while creating the output stream.
     * @throws UnknownServiceException if the protocol does not support output.
     */
    // 返回指向当前URL资源的输出流，可以向其写入数据
    // 返回一个OutputStream，用来写入数据传送给服务器
    // 默认情况下URLConnection不允许输出，所以在请求输出流之前必须调用setDoOutput(true)
    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support output");
    }
    
    /*▲ 字节流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 资源内容 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves the contents of this URL connection.
     * <p>
     * This method first determines the content type of the object by
     * calling the {@code getContentType} method. If this is
     * the first time that the application has seen that specific content
     * type, a content handler for that content type is created.
     * <p> This is done as follows:
     * <ol>
     * <li>If the application has set up a content handler factory instance
     *     using the {@code setContentHandlerFactory} method, the
     *     {@code createContentHandler} method of that instance is called
     *     with the content type as an argument; the result is a content
     *     handler for that content type.
     * <li>If no {@code ContentHandlerFactory} has yet been set up,
     *     or if the factory's {@code createContentHandler} method
     *     returns {@code null}, then the {@linkplain java.util.ServiceLoader
     *     ServiceLoader} mechanism is used to locate {@linkplain
     *     java.net.ContentHandlerFactory ContentHandlerFactory}
     *     implementations using the system class
     *     loader. The order that factories are located is implementation
     *     specific, and an implementation is free to cache the located
     *     factories. A {@linkplain java.util.ServiceConfigurationError
     *     ServiceConfigurationError}, {@code Error} or {@code RuntimeException}
     *     thrown from the {@code createContentHandler}, if encountered, will
     *     be propagated to the calling thread. The {@code
     *     createContentHandler} method of each factory, if instantiated, is
     *     invoked, with the content type, until a factory returns non-null,
     *     or all factories have been exhausted.
     * <li>Failing that, this method tries to load a content handler
     *     class as defined by {@link java.net.ContentHandler ContentHandler}.
     *     If the class does not exist, or is not a subclass of {@code
     *     ContentHandler}, then an {@code UnknownServiceException} is thrown.
     * </ol>
     *
     * @return the object fetched. The {@code instanceof} operator
     * should be used to determine the specific kind of object
     * returned.
     *
     * @throws IOException             if an I/O error occurs while
     *                                 getting the content.
     * @throws UnknownServiceException if the protocol does not support
     *                                 the content type.
     * @see java.net.ContentHandlerFactory#createContentHandler(java.lang.String)
     * @see java.net.URLConnection#getContentType()
     * @see java.net.URLConnection#setContentHandlerFactory(java.net.ContentHandlerFactory)
     */
    // 返回目标资源的内容，返回的形式取决于资源的类型(不一定总是输入流)
    public Object getContent() throws IOException {
        
        /*
         * Must call getInputStream before GetHeaderField gets called
         * so that FileNotFoundException has a chance to be thrown up
         * from here without being caught.
         */
        getInputStream();
        
        // 获取当前URL指向的内容对应的资源内容句柄
        ContentHandler contentHandler = getContentHandler();
        
        // 返回目标资源的内容，返回的形式取决于资源的类型
        return contentHandler.getContent(this);
    }
    
    /**
     * Retrieves the contents of this URL connection.
     *
     * @param classes the {@code Class} array
     *                indicating the requested types
     *
     * @return the object fetched that is the first match of the type
     * specified in the classes array. null if none of
     * the requested types are supported.
     * The {@code instanceof} operator should be used to
     * determine the specific kind of object returned.
     *
     * @throws IOException             if an I/O error occurs while
     *                                 getting the content.
     * @throws UnknownServiceException if the protocol does not support
     *                                 the content type.
     * @see java.net.URLConnection#getContent()
     * @see java.net.ContentHandlerFactory#createContentHandler(java.lang.String)
     * @see java.net.URLConnection#getContent(java.lang.Class[])
     * @see java.net.URLConnection#setContentHandlerFactory(java.net.ContentHandlerFactory)
     * @since 1.3
     */
    // 返回目标资源的内容，且限定该"内容"只能是指定的类型；内容的返回形式取决于资源的类型(不一定总是输入流)
    public Object getContent(Class<?>[] classes) throws IOException {
        
        /*
         * Must call getInputStream before GetHeaderField gets called
         * so that FileNotFoundException has a chance to be thrown up
         * from here without being caught.
         */
        getInputStream();
        
        // 获取当前URL指向的内容对应的资源内容句柄
        ContentHandler contentHandler = getContentHandler();
        
        // 返回目标资源的内容，且限定该"内容"只能是指定的类型；内容的返回形式取决于资源的类型
        return contentHandler.getContent(this, classes);
    }
    
    /*▲ 资源内容 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 请求头 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the general request property.
     * If a property with the key already exists, overwrite its value with the new value.
     *
     * <p> NOTE: HTTP requires all request properties which can
     * legally have multiple instances with the same key
     * to use a comma-separated list syntax which enables multiple
     * properties to be appended into a single property.
     *
     * @param key   the keyword by which the request is known (e.g., "{@code Accept}").
     * @param value the value associated with it.
     *
     * @throws IllegalStateException if already connected
     * @throws NullPointerException  if key is {@code null}
     * @see #getRequestProperty(java.lang.String)
     */
    // 设置请求头，通常在http(s)协议中被使用
    // 为http首部增加首部字段，指定名和值为这个URLConnection的首部增加一个字段，只能在连接打开之前使用
    // HTTP允许一个指定名字的属性有多个值，这种情况下各个值用逗号隔开
    public void setRequestProperty(String key, String value) {
        checkConnected();
        
        if(key == null) {
            throw new NullPointerException("key is null");
        }
        
        if(requests == null) {
            requests = new MessageHeader();
        }
        
        requests.set(key, value);
    }
    
    /**
     * Adds a general request property specified by a
     * key-value pair.  This method will not overwrite
     * existing values associated with the same key.
     *
     * @param key   the keyword by which the request is known
     *              (e.g., "{@code Accept}").
     * @param value the value associated with it.
     *
     * @throws IllegalStateException if already connected
     * @throws NullPointerException  if key is null
     * @see #getRequestProperties()
     * @since 1.4
     */
    // 添加请求头，通常在http(s)协议中被使用
    public void addRequestProperty(String key, String value) {
        checkConnected();
        
        if(key == null) {
            throw new NullPointerException("key is null");
        }
        
        if(requests == null) {
            requests = new MessageHeader();
        }
        
        requests.add(key, value);
    }
    
    /**
     * Returns an unmodifiable Map of general request
     * properties for this connection. The Map keys
     * are Strings that represent the request-header
     * field names. Each Map value is a unmodifiable List
     * of Strings that represents the corresponding
     * field values.
     *
     * @return a Map of the general request properties for this connection.
     *
     * @throws IllegalStateException if already connected
     * @since 1.4
     */
    // 获取请求头，通常在http(s)协议中被使用
    // 返回这个URLConnection所用HTTP首部中指定字段和值
    public Map<String, List<String>> getRequestProperties() {
        checkConnected();
        
        if(requests == null) {
            return Collections.emptyMap();
        }
        
        return requests.getHeaders(null);
    }
    
    /**
     * Returns the value of the named general request property for this
     * connection.
     *
     * @param key the keyword by which the request is known (e.g., "Accept").
     *
     * @return the value of the named general request property for this
     * connection. If key is null, then null is returned.
     *
     * @throws IllegalStateException if already connected
     * @see #setRequestProperty(java.lang.String, java.lang.String)
     */
    // 获取指定key的请求头，通常在http(s)协议中被使用
    public String getRequestProperty(String key) {
        checkConnected();
        
        if(requests == null) {
            return null;
        }
        
        return requests.findValue(key);
    }
    
    /*▲ 请求头 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 头信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value of the named header field.
     * <p>
     * If called on a connection that sets the same header multiple times
     * with possibly different values, only the last value is returned.
     *
     * @param name the name of a header field.
     *
     * @return the value of the named header field, or {@code null}
     * if there is no such field in the header.
     */
    // 返回【指定名称】的头信息的值，由子类覆盖实现
    public String getHeaderField(String name) {
        return null;
    }
    
    /**
     * Returns the key for the {@code n}<sup>th</sup> header field.
     * It returns {@code null} if there are fewer than {@code n+1} fields.
     *
     * @param n an index, where {@code n>=0}
     *
     * @return the key for the {@code n}<sup>th</sup> header field,
     * or {@code null} if there are fewer than {@code n+1}
     * fields.
     */
    // 返回第n(>=0)条头信息的key，由子类覆盖实现
    // 返回第n个首部的字段的键，请求方法是第0个首部，它的键为null，第一个首部即编号为1
    public String getHeaderFieldKey(int n) {
        return null;
    }
    
    /**
     * Returns the value for the {@code n}<sup>th</sup> header field.
     * It returns {@code null} if there are fewer than
     * {@code n+1}fields.
     * <p>
     * This method can be used in conjunction with the
     * {@link #getHeaderFieldKey(int) getHeaderFieldKey} method to iterate through all
     * the headers in the message.
     *
     * @param n an index, where {@code n>=0}
     *
     * @return the value of the {@code n}<sup>th</sup> header field
     * or {@code null} if there are fewer than {@code n+1} fields
     *
     * @see java.net.URLConnection#getHeaderFieldKey(int)
     */
    // 返回第n(>=0)条头信息的value，由子类覆盖实现
    // 返回第n个首部字段的值，请求方法是第0个首部，第一个首部即编号为1
    public String getHeaderField(int n) {
        return null;
    }
    
    /**
     * Returns an unmodifiable Map of the header fields.
     * The Map keys are Strings that represent the
     * response-header field names. Each Map value is an
     * unmodifiable List of Strings that represents
     * the corresponding field values.
     *
     * @return a Map of header fields
     *
     * @since 1.4
     */
    // 返回头信息，由子类覆盖实现
    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }
    
    /**
     * Returns the value of the named field parsed as a number.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param name    the name of the header field.
     * @param Default the default value.
     *
     * @return the value of the named field, parsed as an integer. The
     * {@code Default} value is returned if the field is
     * missing or malformed.
     */
    // 返回指定名称的int类型的头信息；如果不存在，则解析默认值Default
    // 1、首先获取由name参数指定的首部字段，2、然后尝试将这个字符串转换为一个int，可以用来表示日期的首部字段
    public int getHeaderFieldInt(String name, int Default) {
        String value = getHeaderField(name);
        try {
            return Integer.parseInt(value);
        } catch(Exception e) {
        }
    
        return Default;
    }
    
    /**
     * Returns the value of the named field parsed as a number.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param name    the name of the header field.
     * @param Default the default value.
     *
     * @return the value of the named field, parsed as a long. The
     * {@code Default} value is returned if the field is
     * missing or malformed.
     *
     * @since 1.7
     */
    // 返回指定名称的long类型的头信息；如果不存在，则解析默认值Default
    // 首先获取由name参数指定的首部字段，然后尝试将这个字符串转换为一个long，可以用来表示日期的首部字段
    public long getHeaderFieldLong(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch(Exception e) {
        }
    
        return Default;
    }
    
    /**
     * Returns the value of the named field parsed as date.
     * The result is the number of milliseconds since January 1, 1970 GMT
     * represented by the named field.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param name    the name of the header field.
     * @param Default a default value.
     *
     * @return the value of the field, parsed as a date. The value of the
     * {@code Default} argument is returned if the field is
     * missing or malformed.
     */
    // 返回指定名称的日期类型的头信息；如果不存在，则将默认值Default解析为日期返回
    // 1、首先获取由name参数指定的首部字段，2、然后尝试将这个字符串转换为一个long，可以用来表示日期的首部字段
    @SuppressWarnings("deprecation")
    public long getHeaderFieldDate(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Date.parse(value);
        } catch(Exception e) {
        }
        
        return Default;
    }
    
    /*▲ 头信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 消息头 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value of the {@code content-length} header field.
     * <P>
     * <B>Note</B>: {@link #getContentLengthLong() getContentLengthLong()}
     * should be preferred over this method, since it returns a {@code long}
     * instead and is therefore more portable.</P>
     *
     * @return the content length of the resource that this connection's URL
     * references, {@code -1} if the content length is not known,
     * or if the content length is greater than Integer.MAX_VALUE.
     */
    // 返回"content-length"
    // 返回内容种有多少字节，如果没有这个首部，则返回-1
    public int getContentLength() {
        long l = getContentLengthLong();
        if(l>Integer.MAX_VALUE) {
            return -1;
        }
        
        return (int) l;
    }
    
    /**
     * Returns the value of the {@code content-length} header field as a
     * long.
     *
     * @return the content length of the resource that this connection's URL
     * references, or {@code -1} if the content length is
     * not known.
     *
     * @since 1.7
     */
    // 返回"content-length"，以long形式返回
    public long getContentLengthLong() {
        return getHeaderFieldLong("content-length", -1);
    }
    
    /**
     * Returns the value of the {@code content-type} header field.
     *
     * @return the content type of the resource that the URL references,
     * or {@code null} if not known.
     *
     * @see java.net.URLConnection#getHeaderField(java.lang.String)
     */
    // 返回MIME信息头中的"content-type"(MIME类型)

    /**
     * MIME是一个互联网标准，它扩展了电子邮件标准，使其能够支持文本、音频、视频、应用程序软件等多种文件格式的发送和接收
     * MIME最初是为了在电子邮件系统中发送非ASCII字符设计的，后来其应用范围被扩展到支持各种类型的数据在互联网上传输
     *
     * MIME通过使用“类型/子类型”的格式来标识文件类型，例如text/plain：标识纯文本文件，text/html：表示HTML文档
     * MIME类型不仅用于电子邮件，还广泛用于web开发，特别是在HTTP协议的content-type头部中
     * 当浏览器向服务器请求一个页面或资源时，服务器会在相应的HTTP头部中指定资源的MIME类型，这样浏览器就知道如何正确处理和显示该资源
     * @return
     */
    public String getContentType() {
        return getHeaderField("content-type");
    }
    
    /**
     * Returns the value of the {@code content-encoding} header field.
     *
     * @return the content encoding of the resource that the URL references,
     * or {@code null} if not known.
     *
     * @see java.net.URLConnection#getHeaderField(java.lang.String)
     */
    // 返回"content-encoding"
    // 指出内容如何编码，web上最常用的内容编码可能是x-gzip，可以使用GZipInputStream直接解码
    // 内容编码指出字节如何编码为其他字节
    // 字符编码由Content-type首部或文档内部的信息确定，指出如何将字符编码为字节
    public String getContentEncoding() {
        return getHeaderField("content-encoding");
    }
    
    /**
     * Returns the value of the {@code expires} header field.
     *
     * @return the expiration date of the resource that this URL references,
     * or 0 if not known. The value is the number of milliseconds since
     * January 1, 1970 GMT.
     *
     * @see java.net.URLConnection#getHeaderField(java.lang.String)
     */
    // 返回"expires"
    public long getExpiration() {
        return getHeaderFieldDate("expires", 0);
    }
    
    /**
     * Returns the value of the {@code date} header field.
     *
     * @return the sending date of the resource that the URL references,
     * or {@code 0} if not known. The value returned is the
     * number of milliseconds since January 1, 1970 GMT.
     *
     * @see java.net.URLConnection#getHeaderField(java.lang.String)
     */
    // 返回"date"，指出文档何时发送
    public long getDate() {
        return getHeaderFieldDate("date", 0);
    }
    
    /**
     * Returns the value of the {@code last-modified} header field.
     * The result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return the date the resource referenced by this
     * {@code URLConnection} was last modified, or 0 if not known.
     *
     * @see java.net.URLConnection#getHeaderField(java.lang.String)
     */
    // 返回"last-modified"
    public long getLastModified() {
        return getHeaderFieldDate("last-modified", 0);
    }
    
    /*▲ 消息头 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value of this {@code URLConnection}'s {@code URL}
     * field.
     *
     * @return the value of this {@code URLConnection}'s {@code URL}
     * field.
     *
     * @see java.net.URLConnection#url
     */
    // 返回当前(资源)连接的URL
    public URL getURL() {
        return url;
    }
    
    /**
     * Loads filename map (a mimetable) from a data file.
     * It will first try to load the user-specific table, defined by content.types.user.table property.
     * If that fails, it tries to load the default built-in table.
     *
     * @return the FileNameMap
     *
     * @see #setFileNameMap(java.net.FileNameMap)
     * @since 1.2
     */
    // 返回文件名到MIME类型的映射
    public static FileNameMap getFileNameMap() {
        if(fileNameMap != null) {
            return fileNameMap;
        }
        
        fileNameMap = new FileNameMap() {
            // 获取一个MIME信息表
            private FileNameMap internalMap = sun.net.www.MimeTable.loadTable();
            
            // 返回指定文件名对应的MIME类型
            public String getContentTypeFor(String fileName) {
                return internalMap.getContentTypeFor(fileName);
            }
        };
        
        return fileNameMap;
    }
    
    /**
     * Tries to determine the content type of an object, based
     * on the specified "file" component of a URL.
     * This is a convenience method that can be used by
     * subclasses that override the {@code getContentType} method.
     *
     * @param fname a filename.
     *
     * @return a guess as to what the content type of the object is,
     * based upon its file name.
     *
     * @see java.net.URLConnection#getContentType()
     */
    // 返回(猜测)指定文件名对应的MIME类型
    // 尝试根据对象URL的文件扩展名部分猜测对象的内容类型
    public static String guessContentTypeFromName(String fname) {
        // 获取文件名到MIME类型的映射
        FileNameMap fileNameMap = getFileNameMap();
        
        // 返回指定文件名对应的MIME类型
        return fileNameMap.getContentTypeFor(fname);
    }
    
    /**
     * Tries to determine the type of an input stream based on the characters at the beginning of the input stream.
     * This method can be used by subclasses that override the {@code getContentType} method.
     * <p>
     * Ideally, this routine would not be needed. But many
     * {@code http} servers return the incorrect content type; in
     * addition, there are many nonstandard extensions. Direct inspection
     * of the bytes to determine the content type is often more accurate
     * than believing the content type claimed by the {@code http} server.
     *
     * @param is an input stream that supports marks.
     *
     * @return a guess at the content type, or {@code null} if none
     * can be determined.
     *
     * @throws IOException if an I/O error occurs while reading the input stream.
     * @see java.io.InputStream#mark(int)
     * @see java.io.InputStream#markSupported()
     * @see java.net.URLConnection#getContentType()
     */
    // 对于特定类型的文件，可以通过解析其输入流的前几个字节来猜测该文件的真实类型
    // 尝试查看流中前几个字节猜测内容类型
    public static String guessContentTypeFromStream(InputStream is) throws IOException {
        // If we can't read ahead safely, just give up on guessing
        if(!is.markSupported()) {
            return null;
        }
        
        is.mark(16);
        int c1 = is.read();
        int c2 = is.read();
        int c3 = is.read();
        int c4 = is.read();
        int c5 = is.read();
        int c6 = is.read();
        int c7 = is.read();
        int c8 = is.read();
        int c9 = is.read();
        int c10 = is.read();
        int c11 = is.read();
        int c12 = is.read();
        int c13 = is.read();
        int c14 = is.read();
        int c15 = is.read();
        int c16 = is.read();
        is.reset();
        
        if(c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE) {
            return "application/java-vm";
        }
        
        if(c1 == 0xAC && c2 == 0xED) {
            // next two bytes are version number, currently 0x00 0x05
            return "application/x-java-serialized-object";
        }
        
        if(c1 == '<') {
            if(c2 == '!' || ((c2 == 'h' && (c3 == 't' && c4 == 'm' && c5 == 'l' || c3 == 'e' && c4 == 'a' && c5 == 'd') || (c2 == 'b' && c3 == 'o' && c4 == 'd' && c5 == 'y'))) || ((c2 == 'H' && (c3 == 'T' && c4 == 'M' && c5 == 'L' || c3 == 'E' && c4 == 'A' && c5 == 'D') || (c2 == 'B' && c3 == 'O' && c4 == 'D' && c5 == 'Y')))) {
                return "text/html";
            }
            
            if(c2 == '?' && c3 == 'x' && c4 == 'm' && c5 == 'l' && c6 == ' ') {
                return "application/xml";
            }
        }
        
        // big and little (identical) endian UTF-8 encodings, with BOM
        if(c1 == 0xef && c2 == 0xbb && c3 == 0xbf) {
            if(c4 == '<' && c5 == '?' && c6 == 'x') {
                return "application/xml";
            }
        }
        
        // big and little endian UTF-16 encodings, with byte order mark
        if(c1 == 0xfe && c2 == 0xff) {
            if(c3 == 0 && c4 == '<' && c5 == 0 && c6 == '?' && c7 == 0 && c8 == 'x') {
                return "application/xml";
            }
        }
        
        if(c1 == 0xff && c2 == 0xfe) {
            if(c3 == '<' && c4 == 0 && c5 == '?' && c6 == 0 && c7 == 'x' && c8 == 0) {
                return "application/xml";
            }
        }
        
        // big and little endian UTF-32 encodings, with BOM
        if(c1 == 0x00 && c2 == 0x00 && c3 == 0xfe && c4 == 0xff) {
            if(c5 == 0 && c6 == 0 && c7 == 0 && c8 == '<' && c9 == 0 && c10 == 0 && c11 == 0 && c12 == '?' && c13 == 0 && c14 == 0 && c15 == 0 && c16 == 'x') {
                return "application/xml";
            }
        }
        
        if(c1 == 0xff && c2 == 0xfe && c3 == 0x00 && c4 == 0x00) {
            if(c5 == '<' && c6 == 0 && c7 == 0 && c8 == 0 && c9 == '?' && c10 == 0 && c11 == 0 && c12 == 0 && c13 == 'x' && c14 == 0 && c15 == 0 && c16 == 0) {
                return "application/xml";
            }
        }
        
        if(c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8') {
            return "image/gif";
        }
        
        if(c1 == '#' && c2 == 'd' && c3 == 'e' && c4 == 'f') {
            return "image/x-bitmap";
        }
        
        if(c1 == '!' && c2 == ' ' && c3 == 'X' && c4 == 'P' && c5 == 'M' && c6 == '2') {
            return "image/x-pixmap";
        }
        
        if(c1 == 137 && c2 == 80 && c3 == 78 && c4 == 71 && c5 == 13 && c6 == 10 && c7 == 26 && c8 == 10) {
            return "image/png";
        }
        
        if(c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF) {
            if(c4 == 0xE0 || c4 == 0xEE) {
                return "image/jpeg";
            }
            
            /**
             * File format used by digital cameras to store images.
             * Exif Format can be read by any application supporting
             * JPEG. Exif Spec can be found at:
             * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
             */
            if((c4 == 0xE1) && (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 == 'f' && c11 == 0)) {
                return "image/jpeg";
            }
        }
        
        if((c1 == 0x49 && c2 == 0x49 && c3 == 0x2a && c4 == 0x00) || (c1 == 0x4d && c2 == 0x4d && c3 == 0x00 && c4 == 0x2a)) {
            return "image/tiff";
        }
        
        if(c1 == 0xD0 && c2 == 0xCF && c3 == 0x11 && c4 == 0xE0 && c5 == 0xA1 && c6 == 0xB1 && c7 == 0x1A && c8 == 0xE1) {
            
            /* Above is signature of Microsoft Structured Storage.
             * Below this, could have tests for various SS entities.
             * For now, just test for FlashPix.
             */
            if(checkfpx(is)) {
                return "image/vnd.fpx";
            }
        }
        
        if(c1 == 0x2E && c2 == 0x73 && c3 == 0x6E && c4 == 0x64) {
            return "audio/basic";  // .au format, big endian
        }
        
        if(c1 == 0x64 && c2 == 0x6E && c3 == 0x73 && c4 == 0x2E) {
            return "audio/basic";  // .au format, little endian
        }
        
        if(c1 == 'R' && c2 == 'I' && c3 == 'F' && c4 == 'F') {
            /* I don't know if this is official but evidence
             * suggests that .wav files start with "RIFF" - brown
             */
            return "audio/x-wav";
        }
        
        return null;
    }
    
    
    /**
     * Returns the value of this {@code URLConnection}'s {@code doInput} flag.
     *
     * @return the value of this {@code URLConnection}'s {@code doInput} flag.
     *
     * @see #setDoInput(boolean)
     */
    // 返回doInput参数
    public boolean getDoInput() {
        return doInput;
    }
    
    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code doOutput} flag.
     *
     * @return the value of this {@code URLConnection}'s
     * {@code doOutput} flag.
     *
     * @see #setDoOutput(boolean)
     */
    // 返回doOutput参数
    public boolean getDoOutput() {
        return doOutput;
    }
    
    /**
     * Returns the default value of the {@code allowUserInteraction}
     * field.
     * <p>
     * This default is "sticky", being a part of the static state of all
     * URLConnections.  This flag applies to the next, and all following
     * URLConnections that are created.
     *
     * @return the default value of the {@code allowUserInteraction}
     * field.
     *
     * @see #setDefaultAllowUserInteraction(boolean)
     */
    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }
    
    /**
     * Returns the default value of the {@code useCaches} flag for the given protocol. If
     * {@link #setDefaultUseCaches(String, boolean)} was called for the given protocol,
     * then that value is returned. Otherwise, if {@link #setDefaultUseCaches(boolean)}
     * was called, then that value is returned. If neither method was called,
     * the return value is {@code true}. The protocol name is case insensitive.
     *
     * @param protocol the protocol whose defaultUseCaches setting is required
     *
     * @return the default value of the {@code useCaches} flag for the given protocol.
     *
     * @since 9
     */
    public static boolean getDefaultUseCaches(String protocol) {
        Boolean protoDefault = defaultCaching.get(protocol.toLowerCase(Locale.US));
        
        if(protoDefault != null) {
            return protoDefault.booleanValue();
        } else {
            return defaultUseCaches;
        }
    }

    // 控制socket等待建立连接的时间，0表示永远不超时
    /**
     * Returns setting for connect timeout.
     * <p>
     * 0 return implies that the option is disabled
     * (i.e., timeout of infinity).
     *
     * @return an {@code int} that indicates the connect timeout
     * value in milliseconds
     *
     * @see #setConnectTimeout(int)
     * @see #connect()
     * @since 1.5
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    // 控制输入流等待数据到达的时间，0表示永远不超时
    /**
     * Returns setting for read timeout. 0 return implies that the
     * option is disabled (i.e., timeout of infinity).
     *
     * @return an {@code int} that indicates the read timeout
     * value in milliseconds
     *
     * @see #setReadTimeout(int)
     * @see InputStream#read()
     * @since 1.5
     */
    public int getReadTimeout() {
        return readTimeout;
    }
    
    /**
     * Returns the value of the {@code allowUserInteraction} field for this object.
     *
     * @return the value of the {@code allowUserInteraction} field for this object.
     *
     * @see #setAllowUserInteraction(boolean)
     */
    public boolean getAllowUserInteraction() {
        return allowUserInteraction;
    }
    
    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code useCaches} field.
     *
     * @return the value of this {@code URLConnection}'s
     * {@code useCaches} field.
     *
     * @see #setUseCaches(boolean)
     */
    public boolean getUseCaches() {
        return useCaches;
    }
    
    /**
     * Returns the default value of a {@code URLConnection}'s
     * {@code useCaches} flag.
     * <p>
     * This default is "sticky", being a part of the static state of all
     * URLConnections.  This flag applies to the next, and all following
     * URLConnections that are created. This default value can be over-ridden
     * per protocol using {@link #setDefaultUseCaches(String, boolean)}
     *
     * @return the default value of a {@code URLConnection}'s
     * {@code useCaches} flag.
     *
     * @see #setDefaultUseCaches(boolean)
     */
    public boolean getDefaultUseCaches() {
        return defaultUseCaches;
    }
    
    /**
     * Returns the value of this object's {@code ifModifiedSince} field.
     *
     * @return the value of this object's {@code ifModifiedSince} field.
     *
     * @see #setIfModifiedSince(long)
     */
    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    // 在尝试连接一个URL前，可能想知道是否允许连接，返回的Permission指出连接这个URL所需的权限
    /**
     * Returns a permission object representing the permission
     * necessary to make the connection represented by this
     * object. This method returns null if no permission is
     * required to make the connection. By default, this method
     * returns {@code java.security.AllPermission}. Subclasses
     * should override this method and return the permission
     * that best represents the permission required to make
     * a connection to the URL. For example, a {@code URLConnection}
     * representing a {@code file:} URL would return a
     * {@code java.io.FilePermission} object.
     *
     * <p>The permission returned may dependent upon the state of the
     * connection. For example, the permission before connecting may be
     * different from that after connecting. For example, an HTTP
     * sever, say foo.com, may redirect the connection to a different
     * host, say bar.com. Before connecting the permission returned by
     * the connection will represent the permission needed to connect
     * to foo.com, while the permission returned after connecting will
     * be to bar.com.
     *
     * <p>Permissions are generally used for two purposes: to protect
     * caches of objects obtained through URLConnections, and to check
     * the right of a recipient to learn about a particular URL. In
     * the first case, the permission should be obtained
     * <em>after</em> the object has been obtained. For example, in an
     * HTTP connection, this will represent the permission to connect
     * to the host from which the data was ultimately fetched. In the
     * second case, the permission should be obtained and tested
     * <em>before</em> connecting.
     *
     * @return the permission object representing the permission
     * necessary to make the connection represented by this
     * URLConnection.
     *
     * @throws IOException if the computation of the permission
     *                     requires network or file I/O and an exception occurs while
     *                     computing it.
     */
    public Permission getPermission() throws IOException {
        return SecurityConstants.ALL_PERMISSION;
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ set ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the FileNameMap.
     * <p>
     * If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param map the FileNameMap to be set
     *
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkSetFactory} method doesn't allow the operation.
     * @see SecurityManager#checkSetFactory
     * @see #getFileNameMap()
     * @since 1.2
     */
    // 设置文件名到MIME类型映射的缓存
    public static void setFileNameMap(FileNameMap map) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkSetFactory();
        }
        
        fileNameMap = map;
    }
    
    /**
     * Sets the {@code ContentHandlerFactory} of an
     * application. It can be called at most once by an application.
     * <p>
     * The {@code ContentHandlerFactory} instance is used to
     * construct a content handler from a content type.
     * <p>
     * If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param fac the desired factory.
     *
     * @throws Error             if the factory has already been defined.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkSetFactory} method doesn't allow the operation.
     * @see java.net.ContentHandlerFactory
     * @see java.net.URLConnection#getContent()
     * @see SecurityManager#checkSetFactory
     */
    // 设置资源内容句柄工厂
    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
        if(factory != null) {
            throw new Error("factory already defined");
        }
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkSetFactory();
        }
        
        factory = fac;
    }
    
    
    /**
     * Sets the value of the {@code doInput} field for this {@code URLConnection} to the specified value.
     * <p>
     * A URL connection can be used for input and/or output.
     * Set the doInput flag to true if you intend to use the URL connection for input, false if not.
     * The default is true.
     *
     * @param doinput the new value.
     *
     * @throws IllegalStateException if already connected
     * @see java.net.URLConnection#doInput
     * @see #getDoInput()
     */
    // 设置doInput参数
    public void setDoInput(boolean doinput) {
        checkConnected();
        doInput = doinput;
    }
    
    /**
     * Sets the value of the {@code doOutput} field for this
     * {@code URLConnection} to the specified value.
     * <p>
     * A URL connection can be used for input and/or output.  Set the doOutput
     * flag to true if you intend to use the URL connection for output,
     * false if not.  The default is false.
     *
     * @param dooutput the new value.
     *
     * @throws IllegalStateException if already connected
     * @see #getDoOutput()
     */
    // 设置doOutput参数
    public void setDoOutput(boolean dooutput) {
        checkConnected();
        doOutput = dooutput;
    }
    
    /**
     * Sets the default value of the
     * {@code allowUserInteraction} field for all future
     * {@code URLConnection} objects to the specified value.
     *
     * @param defaultallowuserinteraction the new value.
     *
     * @see #getDefaultAllowUserInteraction()
     */
    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {
        defaultAllowUserInteraction = defaultallowuserinteraction;
    }
    
    /**
     * Sets the default value of the {@code useCaches} field for the named
     * protocol to the given value. This value overrides any default setting
     * set by {@link #setDefaultUseCaches(boolean)} for the given protocol.
     * Successive calls to this method change the setting and affect the
     * default value for all future connections of that protocol. The protocol
     * name is case insensitive.
     *
     * @param protocol   the protocol to set the default for
     * @param defaultVal whether caching is enabled by default for the given protocol
     *
     * @since 9
     */
    public static void setDefaultUseCaches(String protocol, boolean defaultVal) {
        protocol = protocol.toLowerCase(Locale.US);
        defaultCaching.put(protocol, defaultVal);
    }
    
    /**
     * Sets a specified timeout value, in milliseconds, to be used
     * when opening a communications link to the resource referenced
     * by this URLConnection.  If the timeout expires before the
     * connection can be established, a
     * java.net.SocketTimeoutException is raised. A timeout of zero is
     * interpreted as an infinite timeout.
     *
     * <p> Some non-standard implementation of this method may ignore
     * the specified timeout. To see the connect timeout set, please
     * call getConnectTimeout().
     *
     * @param timeout an {@code int} that specifies the connect
     *                timeout value in milliseconds
     *
     * @throws IllegalArgumentException if the timeout parameter is negative
     * @see #getConnectTimeout()
     * @see #connect()
     * @since 1.5
     */
    public void setConnectTimeout(int timeout) {
        if(timeout<0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        
        connectTimeout = timeout;
    }
    
    /**
     * Sets the read timeout to a specified timeout, in
     * milliseconds. A non-zero value specifies the timeout when
     * reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available
     * for read, a java.net.SocketTimeoutException is raised. A
     * timeout of zero is interpreted as an infinite timeout.
     *
     * <p> Some non-standard implementation of this method ignores the
     * specified timeout. To see the read timeout set, please call
     * getReadTimeout().
     *
     * @param timeout an {@code int} that specifies the timeout
     *                value to be used in milliseconds
     *
     * @throws IllegalArgumentException if the timeout parameter is negative
     * @see #getReadTimeout()
     * @see InputStream#read()
     * @since 1.5
     */
    public void setReadTimeout(int timeout) {
        if(timeout<0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        
        readTimeout = timeout;
    }
    
    /**
     * Set the value of the {@code allowUserInteraction} field of
     * this {@code URLConnection}.
     *
     * @param allowuserinteraction the new value.
     *
     * @throws IllegalStateException if already connected
     * @see #getAllowUserInteraction()
     */
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        checkConnected();
        allowUserInteraction = allowuserinteraction;
    }
    
    /**
     * Sets the value of the {@code useCaches} field of this
     * {@code URLConnection} to the specified value.
     * <p>
     * Some protocols do caching of documents.  Occasionally, it is important
     * to be able to "tunnel through" and ignore the caches (e.g., the
     * "reload" button in a browser).  If the UseCaches flag on a connection
     * is true, the connection is allowed to use whatever caches it can.
     * If false, caches are to be ignored.
     * The default value comes from defaultUseCaches, which defaults to
     * true. A default value can also be set per-protocol using
     * {@link #setDefaultUseCaches(String, boolean)}.
     *
     * @param usecaches a {@code boolean} indicating whether
     *                  or not to allow caching
     *
     * @throws IllegalStateException if already connected
     * @see #getUseCaches()
     */
    public void setUseCaches(boolean usecaches) {
        checkConnected();
        useCaches = usecaches;
    }
    
    /**
     * Sets the default value of the {@code useCaches} field to the
     * specified value. This default value can be over-ridden
     * per protocol using {@link #setDefaultUseCaches(String, boolean)}
     *
     * @param defaultusecaches the new value.
     *
     * @see #getDefaultUseCaches()
     */
    public void setDefaultUseCaches(boolean defaultusecaches) {
        defaultUseCaches = defaultusecaches;
    }
    
    /**
     * Sets the value of the {@code ifModifiedSince} field of
     * this {@code URLConnection} to the specified value.
     *
     * @param ifmodifiedsince the new value.
     *
     * @throws IllegalStateException if already connected
     * @see #getIfModifiedSince()
     */
    public void setIfModifiedSince(long ifmodifiedsince) {
        checkConnected();
        ifModifiedSince = ifmodifiedsince;
    }
    
    /*▲ set ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value of the default request property. Default request
     * properties are set for every connection.
     *
     * @param key the keyword by which the request is known (e.g., "Accept").
     *
     * @return the value of the default request property
     * for the specified key.
     *
     * @see java.net.URLConnection#getRequestProperty(java.lang.String)
     * @see #setDefaultRequestProperty(java.lang.String, java.lang.String)
     * @deprecated The instance specific getRequestProperty method
     * should be used after an appropriate instance of URLConnection
     * is obtained.
     */
    @Deprecated
    public static String getDefaultRequestProperty(String key) {
        return null;
    }
    
    /**
     * Sets the default value of a general request property. When a
     * {@code URLConnection} is created, it is initialized with
     * these properties.
     *
     * @param key   the keyword by which the request is known
     *              (e.g., "{@code Accept}").
     * @param value the value associated with the key.
     *
     * @see java.net.URLConnection#setRequestProperty(java.lang.String, java.lang.String)
     * @see #getDefaultRequestProperty(java.lang.String)
     * @deprecated The instance specific setRequestProperty method
     * should be used after an appropriate instance of URLConnection
     * is obtained. Invoking this method will have no effect.
     */
    @Deprecated
    public static void setDefaultRequestProperty(String key, String value) {
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a {@code String} representation of this URL connection.
     *
     * @return a string representation of this {@code URLConnection}.
     */
    public String toString() {
        return this.getClass().getName() + ":" + url;
    }
    
    
    /**
     * Gets the Content Handler appropriate for this connection.
     */
    // 获取当前URL指向的内容对应的资源内容句柄
    private ContentHandler getContentHandler() throws UnknownServiceException {
        // 返回MIME信息头中的"content-type"(MIME类型)
        String type = getContentType();
    
        // 去除"content-type"后面的参数
        String contentType = stripOffParameters(type);
        if(contentType == null) {
            throw new UnknownServiceException("no content-type");
        }
    
        // 尝试从缓存中获取资源内容句柄
        ContentHandler handler = handlers.get(contentType);
        if(handler != null) {
            return handler;
        }
    
        // 如果存在资源内容句柄工厂(系统有一个内置的实现：MultimediaContentHandlers)
        if(factory != null) {
            // 创建指定MIME协议类型的资源内容句柄工厂
            handler = factory.createContentHandler(contentType);
            if(handler != null) {
                return handler;
            }
        }
    
        // 在注册服务中加载指定MIME类型的资源内容句柄
        handler = lookupContentHandlerViaProvider(contentType);
        if(handler != null) {
            // 加入缓存
            ContentHandler h = handlers.putIfAbsent(contentType, handler);
        
            // 优先返回旧的handler
            return Objects.requireNonNullElse(h, handler);
        }
    
        try {
            // 从预设的位置加载指定MIME类型的资源内容句柄
            handler = lookupContentHandlerClassFor(contentType);
        } catch(Exception e) {
            e.printStackTrace();
            handler = UnknownContentHandler.INSTANCE;
        }
    
        assert handler != null;
    
        // 加入缓存
        ContentHandler h = handlers.putIfAbsent(contentType, handler);
    
        // 优先返回旧的handler
        return Objects.requireNonNullElse(h, handler);
    }
    
    /*
     * Media types are in the format: type/subtype*(; parameter).
     * For looking up the content handler, we should ignore those parameters.
     */
    // 去除"content-type"后面的参数
    private String stripOffParameters(String contentType) {
        if(contentType == null) {
            return null;
        }
        
        int index = contentType.indexOf(';');
        
        if(index>0) {
            return contentType.substring(0, index);
        } else {
            return contentType;
        }
    }
    
    /**
     * Looks for a content handler in a user-definable set of places.
     * By default it looks in {@value #contentClassPrefix}, but users can define
     * a vertical-bar delimited set of class prefixes to search through in
     * addition by defining the {@value #contentPathProp} property.
     * The class name must be of the form:
     * <pre>
     *     {package-prefix}.{major}.{minor}
     * e.g.
     *     YoyoDyne.experimental.text.plain
     * </pre>
     */
    // 从预设的位置加载指定MIME类型的资源内容句柄
    private ContentHandler lookupContentHandlerClassFor(String contentType) {
        // 将指定的MIME类型转换为对应的包名
        String contentHandlerClassName = typeToPackageName(contentType);
    
        // 获取ContentHandler所在的包
        String contentHandlerPkgPrefixes = getContentHandlerPkgPrefixes();
    
        StringTokenizer packagePrefixIter = new StringTokenizer(contentHandlerPkgPrefixes, "|");
    
        // 遍历所有可用的包
        while(packagePrefixIter.hasMoreTokens()) {
            // 获取ContentHandler实现类的完整类名
            String clsName = packagePrefixIter.nextToken().trim() + "." + contentHandlerClassName;
        
            try {
                Class<?> cls = null;
                try {
                    cls = Class.forName(clsName);
                } catch(ClassNotFoundException e) {
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    if(cl != null) {
                        cls = cl.loadClass(clsName);
                    }
                }
            
                if(cls != null) {
                    return (ContentHandler) cls.newInstance();
                }
            } catch(Exception ignored) {
            }
        }
    
        return UnknownContentHandler.INSTANCE;
    }
    
    // 在注册服务中加载指定MIME类型的资源内容句柄
    private ContentHandler lookupContentHandlerViaProvider(String contentType) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public ContentHandler run() {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                
                // 使用指定的类加载器加载"ContentHandlerFactory"服务
                ServiceLoader<ContentHandlerFactory> sl = ServiceLoader.load(ContentHandlerFactory.class, cl);
                
                // 返回服务提供者迭代器
                Iterator<ContentHandlerFactory> iterator = sl.iterator();
                
                ContentHandler handler = null;
                
                // 遍历所有"ContentHandlerFactory"服务的提供者
                while(iterator.hasNext()) {
                    ContentHandlerFactory f;
                    
                    try {
                        f = iterator.next();
                    } catch(ServiceConfigurationError e) {
                        if(e.getCause() instanceof SecurityException) {
                            continue;
                        }
                        throw e;
                    }
                    
                    // 创建指定MIME协议类型的资源内容句柄工厂
                    handler = f.createContentHandler(contentType);
                    if(handler != null) {
                        break;
                    }
                }
                
                return handler;
            }
        });
    }
    
    /**
     * Utility function to map a MIME content type into an equivalent pair of class name components.
     * For example: "text/html" would be returned as "text.html"
     */
    // 将指定的MIME类型转换为对应的包名
    private String typeToPackageName(String contentType) {
        // make sure we canonicalize the class name: all lower case
        contentType = contentType.toLowerCase();
        int len = contentType.length();
        char[] nm = new char[len];
        contentType.getChars(0, len, nm, 0);
        for(int i = 0; i<len; i++) {
            char c = nm[i];
            if(c == '/') {
                nm[i] = '.';
            } else if(!('A'<=c && c<='Z' || 'a'<=c && c<='z' || '0'<=c && c<='9')) {
                nm[i] = '_';
            }
        }
    
        return new String(nm);
    }
    
    /**
     * Returns a vertical bar separated list of package prefixes for potential content handlers.
     * Tries to get the java.content.handler.pkgs property to use as a set of package prefixes to search.
     * Whether or not that property has been defined, the {@value #contentClassPrefix} is always the last one on the returned package list.
     */
    // 返回ContentHandler所在的包
    private String getContentHandlerPkgPrefixes() {
        String packagePrefixList = GetPropertyAction.privilegedGetProperty(contentPathProp, "");
        
        if(packagePrefixList != "") {
            packagePrefixList += "|";
        }
        
        return packagePrefixList + contentClassPrefix;
    }
    
    // 检查是否连接到资源
    private void checkConnected() {
        if(connected) {
            throw new IllegalStateException("Already connected");
        }
    }
    
    /**
     * Check for FlashPix image data in InputStream is.  Return true if
     * the stream has FlashPix data, false otherwise.  Before calling this
     * method, the stream should have already been checked to be sure it
     * contains Microsoft Structured Storage data.
     */
    private static boolean checkfpx(InputStream is) throws IOException {
        
        /* Test for FlashPix image data in Microsoft Structured Storage format.
         * In general, should do this with calls to an SS implementation.
         * Lacking that, need to dig via offsets to get to the FlashPix
         * ClassID.  Details:
         *
         * Offset to Fpx ClsID from beginning of stream should be:
         *
         * FpxClsidOffset = rootEntryOffset + clsidOffset
         *
         * where: clsidOffset = 0x50.
         *        rootEntryOffset = headerSize + sectorSize*sectDirStart
         *                          + 128*rootEntryDirectory
         *
         *        where:  headerSize = 0x200 (always)
         *                sectorSize = 2 raised to power of uSectorShift,
         *                             which is found in the header at
         *                             offset 0x1E.
         *                sectDirStart = found in the header at offset 0x30.
         *                rootEntryDirectory = in general, should search for
         *                                     directory labelled as root.
         *                                     We will assume value of 0 (i.e.,
         *                                     rootEntry is in first directory)
         */
        
        // Mark the stream so we can reset it. 0x100 is enough for the first
        // few reads, but the mark will have to be reset and set again once
        // the offset to the root directory entry is computed. That offset
        // can be very large and isn't know until the stream has been read from
        is.mark(0x100);
        
        // Get the byte ordering located at 0x1E. 0xFE is Intel,
        // 0xFF is other
        long toSkip = 0x1C;
        long posn;
        
        if((posn = skipForward(is, toSkip))<toSkip) {
            is.reset();
            return false;
        }
        
        int[] c = new int[16];
        if(readBytes(c, 2, is)<0) {
            is.reset();
            return false;
        }
        
        int byteOrder = c[0];
        
        posn += 2;
        int uSectorShift;
        if(readBytes(c, 2, is)<0) {
            is.reset();
            return false;
        }
        
        if(byteOrder == 0xFE) {
            uSectorShift = c[0];
            uSectorShift += c[1] << 8;
        } else {
            uSectorShift = c[0] << 8;
            uSectorShift += c[1];
        }
        
        posn += 2;
        toSkip = (long) 0x30 - posn;
        long skipped = 0;
        if((skipped = skipForward(is, toSkip))<toSkip) {
            is.reset();
            return false;
        }
        posn += skipped;
        
        if(readBytes(c, 4, is)<0) {
            is.reset();
            return false;
        }
        
        int sectDirStart;
        if(byteOrder == 0xFE) {
            sectDirStart = c[0];
            sectDirStart += c[1] << 8;
            sectDirStart += c[2] << 16;
            sectDirStart += c[3] << 24;
        } else {
            sectDirStart = c[0] << 24;
            sectDirStart += c[1] << 16;
            sectDirStart += c[2] << 8;
            sectDirStart += c[3];
        }
        posn += 4;
        is.reset(); // Reset back to the beginning
        
        toSkip = 0x200L + (long) (1 << uSectorShift) * sectDirStart + 0x50L;
        
        // Sanity check!
        if(toSkip<0) {
            return false;
        }
        
        /*
         * How far can we skip? Is there any performance problem here?
         * This skip can be fairly long, at least 0x4c650 in at least
         * one case. Have to assume that the skip will fit in an int.
         * Leave room to read whole root dir
         */
        is.mark((int) toSkip + 0x30);
        
        if((skipForward(is, toSkip))<toSkip) {
            is.reset();
            return false;
        }
        
        /* should be at beginning of ClassID, which is as follows
         * (in Intel byte order):
         *    00 67 61 56 54 C1 CE 11 85 53 00 AA 00 A1 F9 5B
         *
         * This is stored from Windows as long,short,short,char[8]
         * so for byte order changes, the order only changes for
         * the first 8 bytes in the ClassID.
         *
         * Test against this, ignoring second byte (Intel) since
         * this could change depending on part of Fpx file we have.
         */
        
        if(readBytes(c, 16, is)<0) {
            is.reset();
            return false;
        }
        
        // intel byte order
        if(byteOrder == 0xFE && c[0] == 0x00 && c[2] == 0x61 && c[3] == 0x56 && c[4] == 0x54 && c[5] == 0xC1 && c[6] == 0xCE && c[7] == 0x11 && c[8] == 0x85 && c[9] == 0x53 && c[10] == 0x00 && c[11] == 0xAA && c[12] == 0x00 && c[13] == 0xA1 && c[14] == 0xF9 && c[15] == 0x5B) {
            is.reset();
            return true;
        }
        
        // non-intel byte order
        else if(c[3] == 0x00 && c[1] == 0x61 && c[0] == 0x56 && c[5] == 0x54 && c[4] == 0xC1 && c[7] == 0xCE && c[6] == 0x11 && c[8] == 0x85 && c[9] == 0x53 && c[10] == 0x00 && c[11] == 0xAA && c[12] == 0x00 && c[13] == 0xA1 && c[14] == 0xF9 && c[15] == 0x5B) {
            is.reset();
            return true;
        }
        is.reset();
        return false;
    }
    
    /**
     * Tries to read the specified number of bytes from the stream
     * Returns -1, If EOF is reached before len bytes are read, returns 0
     * otherwise
     */
    private static int readBytes(int[] c, int len, InputStream is) throws IOException {
        
        byte[] buf = new byte[len];
        if(is.read(buf, 0, len)<len) {
            return -1;
        }
        
        // fill the passed in int array
        for(int i = 0; i<len; i++) {
            c[i] = buf[i] & 0xff;
        }
        
        return 0;
    }
    
    /**
     * Skips through the specified number of bytes from the stream
     * until either EOF is reached, or the specified
     * number of bytes have been skipped
     */
    private static long skipForward(InputStream is, long toSkip) throws IOException {
        
        long eachSkip = 0;
        long skipped = 0;
        
        while(skipped != toSkip) {
            eachSkip = is.skip(toSkip - skipped);
            
            // check if EOF is reached
            if(eachSkip<=0) {
                if(is.read() == -1) {
                    return skipped;
                } else {
                    skipped++;
                }
            }
            
            skipped += eachSkip;
        }
        
        return skipped;
    }
    
}
