package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import junit.framework.TestCase;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.commons.ClassRemapper;
import org.jetbrains.org.objectweb.asm.commons.Remapper;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;

public class CsvTableTransferHandlerClassLoaderTest extends TestCase {
    public void testClipboardFlavorUsesPluginClassLoader() throws Exception {
        String originalName = CsvTableTransferHandler.class.getName();
        String handlerName = originalName + "Isolated";
        ClassLoader parent = CsvTableTransferHandler.class.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader context = thread.getContextClassLoader();
        ClassLoader pluginLoader = new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(handlerName) && !name.startsWith(handlerName + "$")) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        String sourceName = originalName + name.substring(handlerName.length());
                        String resource = sourceName.substring(sourceName.lastIndexOf('.') + 1) + ".class";
                        try (InputStream input = CsvTableTransferHandler.class.getResourceAsStream(resource)) {
                            if (input == null) throw new ClassNotFoundException(name);
                            ClassWriter writer = new ClassWriter(0);
                            new ClassReader(input).accept(new ClassRemapper(writer, new Remapper() {
                                @Override
                                public String map(String type) {
                                    // Keep the copied plugin classes invisible to the test/application class loader.
                                    String prefix = originalName.replace('.', '/');
                                    return type.equals(prefix) || type.startsWith(prefix + "$")
                                            ? handlerName.replace('.', '/') + type.substring(prefix.length()) : type;
                                }
                            }), 0);
                            byte[] bytes = writer.toByteArray();
                            type = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException exception) {
                            throw new ClassNotFoundException(name, exception);
                        }
                    }
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
        };
        try {
            thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
            Class<?> handler = Class.forName(handlerName, true, pluginLoader);
            var field = handler.getDeclaredField("CELLS_FLAVOR");
            field.setAccessible(true);
            DataFlavor flavor = (DataFlavor) field.get(null);
            assertTrue(flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType));
            assertSame(pluginLoader.loadClass(handlerName + "$Cells"), flavor.getRepresentationClass());
        } finally {
            thread.setContextClassLoader(context);
        }
    }
}
