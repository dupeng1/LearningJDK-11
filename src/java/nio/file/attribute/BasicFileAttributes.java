/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

/**
 * Basic attributes associated with a file in a file system.
 *
 * <p> Basic file attributes are attributes that are common to many file systems
 * and consist of mandatory and optional file attributes as defined by this
 * interface.
 *
 * <p> <b>Usage Example:</b>
 * <pre>
 *    Path file = ...
 *    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
 * </pre>
 *
 * @see BasicFileAttributeView
 * @since 1.7
 */
// 【"basic"文件属性】，提供了一些基本的文件metadata，它的实现类因平台而已有：DosFileAttributes、PosixFileAttribute、UnixFileAttribute；不同平台所能支持的属性有所不同

/**
 * 1、读取属性
 * Path path = Paths.get("/data/logs/web.log");
 * BasicFileAttributes attributes = Files.readAttributes(path,BasicFileAttributes.class);
 * System.out.println("regular file:" + attributes.isRegularFile());
 * System.out.println("directory:" + attributes.isDirectory());
 * System.out.println("symbolic link:" + attributes.isSymbolicLink());
 * System.out.println("modified time:" + attributes.lastModifiedTime().toMillis());
 * 2、修改系统更新属性
 * Files.setLastModifiedTime(path,FileTime.fromMillis(System.currentTimeMillis()));
 * 3、修改其他属性
 * Files.setAttribute(path,"dos:hidden",true);
 * 属性名格式为“view-name:attribute-name”，比如“dos:hidden”；
 * 其中合法的view-name目前有“basic”、“posix”、“unix”、“owner”（所有者信息，权限），
 * 属性的列表需要根据自己的平台对应相应的Attributes类，否则会导致设置异常。
 */

public interface BasicFileAttributes {
    
    /**
     * Returns the creation time. The creation time is the time that the file
     * was created.
     *
     * <p> If the file system implementation does not support a time stamp
     * to indicate the time when the file was created then this method returns
     * an implementation specific default value, typically the {@link
     * #lastModifiedTime() last-modified-time} or a {@code FileTime}
     * representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time the file was created
     */
    // 返回【创建时间】
    FileTime creationTime();
    
    /**
     * Returns the time of last access.
     *
     * <p> If the file system implementation does not support a time stamp
     * to indicate the time of last access then this method returns
     * an implementation specific default value, typically the {@link
     * #lastModifiedTime() last-modified-time} or a {@code FileTime}
     * representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time of last access
     */
    // 返回【最后访问时间】
    FileTime lastAccessTime();
    
    /**
     * Returns the time of last modification.
     *
     * <p> If the file system implementation does not support a time stamp
     * to indicate the time of last modification then this method returns an
     * implementation specific default value, typically a {@code FileTime}
     * representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time the file was last
     * modified
     */
    // 返回【最后修改时间】
    FileTime lastModifiedTime();
    
    /**
     * Tells whether the file is a symbolic link.
     *
     * @return {@code true} if the file is a symbolic link
     */
    // 判断当前属性的宿主资源是否为【符号链接】
    boolean isSymbolicLink();
    
    /**
     * Tells whether the file is a directory.
     *
     * @return {@code true} if the file is a directory
     */
    // 判断当前属性的宿主资源是否为【非符号链接的目录】
    boolean isDirectory();
    
    /**
     * Tells whether the file is a regular file with opaque content.
     *
     * @return {@code true} if the file is a regular file with opaque content
     */
    /*
     * 判断当前属性的宿主资源是否为"不透明的"【常规文件】
     *
     * 在类unix系统中，该类文件是永久存储在文件系统中的字节序列；
     * 在windows上，比如普通文件、文件硬链接，均属于"不透明的"常规文件；
     * 对于符号链接，如果需要将其链接到目标文件，那么文件的符号链接也属于"不透明的"常规文件。
     */
    boolean isRegularFile();
    
    /**
     * Tells whether the file is something other than a regular file, directory, or symbolic link.
     *
     * @return {@code true} if the file something other than a regular file, directory or symbolic link
     */
    /*
     * 判断当前属性的宿主资源是否为符号链接/常规文件/目录之外的其他文件
     *
     * 在windows上，如果不需要链接到符号链接的目标文件，那么目录硬链接(mklink /J link target)会被认为属于Other。
     */
    boolean isOther();
    
    /**
     * Returns the size of the file (in bytes). The size may differ from the
     * actual size on the file system due to compression, support for sparse
     * files, or other reasons. The size of files that are not {@link
     * #isRegularFile regular} files is implementation specific and
     * therefore unspecified.
     *
     * @return the file size, in bytes
     */
    // 返回【文件大小(字节数)】
    long size();
    
    /**
     * Returns an object that uniquely identifies the given file, or {@code
     * null} if a file key is not available. On some platforms or file systems
     * it is possible to use an identifier, or a combination of identifiers to
     * uniquely identify a file. Such identifiers are important for operations
     * such as file tree traversal in file systems that support <a
     * href="../package-summary.html#links">symbolic links</a> or file systems
     * that allow a file to be an entry in more than one directory. On UNIX file
     * systems, for example, the <em>device ID</em> and <em>inode</em> are
     * commonly used for such purposes.
     *
     * <p> The file key returned by this method can only be guaranteed to be
     * unique if the file system and files remain static. Whether a file system
     * re-uses identifiers after a file is deleted is implementation dependent and
     * therefore unspecified.
     *
     * <p> File keys returned by this method can be compared for equality and are
     * suitable for use in collections. If the file system and files remain static,
     * and two files are the {@link java.nio.file.Files#isSameFile same} with
     * non-{@code null} file keys, then their file keys are equal.
     *
     * @return an object that uniquely identifies the given file, or {@code null}
     *
     * @see java.nio.file.Files#walkFileTree
     */
    // 返回【唯一标识】给定文件的对象。如果文件标识不可用(例如windows上)，则返回null
    Object fileKey();
    
}
