/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Loads the classes from the given jars.
 */
public class NestedJarClassLoader extends ClassLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(NestedJarClassLoader.class);
    private final ClassLoader jarClassLoader;
    private final String[] excludes;
    private final File jarDir;
    private static final File TEMP_DIR = new File("data/njcl");

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(TEMP_DIR)));
    }

    public NestedJarClassLoader(URL jarURL, String... excludes) {
        this(jarURL, NestedJarClassLoader.class.getClassLoader(), excludes);
    }

    NestedJarClassLoader(URL jarURL, ClassLoader parentClassLoader, String... excludes) {
        super(parentClassLoader);
        this.jarDir = new File(TEMP_DIR, UUID.randomUUID().toString());
        this.jarClassLoader = createLoaderForJar(jarURL);
        this.excludes = excludes;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(jarDir)));
    }

    private ClassLoader createLoaderForJar(URL jarURL) {
        LOGGER.debug("Creating Loader For jar: {}", jarURL);
        return new URLClassLoader(enumerateJar(jarURL), this);
    }

    private URL[] enumerateJar(URL urlOfJar) {
        LOGGER.debug("Enumerating jar: {}", urlOfJar);
        List<URL> urls = new ArrayList<>();
        urls.add(urlOfJar);
        try (JarInputStream jarStream = new JarInputStream(urlOfJar.openStream())) {
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
                    urls.add(expandJarAndReturnURL(jarStream, entry));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to enumerate jar {}", urlOfJar, e);
        }
        return urls.toArray(new URL[0]);
    }

    private URL expandJarAndReturnURL(JarInputStream jarStream, JarEntry entry) throws IOException {
        File nestedJarFile = new File(jarDir, entry.getName());
        nestedJarFile.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(nestedJarFile)) {
            jarStream.transferTo(out);
        }
        LOGGER.info("Exploded Entry {} from to {}", entry.getName(), nestedJarFile);
        return nestedJarFile.toURI().toURL();
    }


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            return jarClassLoader.loadClass(name);
        }
        return super.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            throw new ClassNotFoundException(name);
        }
        return super.loadClass(name, resolve);
    }

    private boolean existsInTfsJar(String name) {
        if (jarClassLoader == null) {
            return false;
        }
        String classAsResourceName = name.replace('.', '/') + ".class";
        if (isExcluded(classAsResourceName)) {
            return false;
        }
        URL url = jarClassLoader.getResource(classAsResourceName);
        LOGGER.debug("Loading {} from jar returned {} for url: {}  ", name, url != null, url);
        return url != null;
    }

    @Override
    public URL getResource(String name) {
        if (isExcluded(name)) {
            return super.getResource(name);
        }
        return null;
    }

    private boolean isExcluded(String name) {
        for (String excluded : excludes) {
            if (name.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

}
