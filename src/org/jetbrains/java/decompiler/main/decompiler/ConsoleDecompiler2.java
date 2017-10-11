//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.jetbrains.java.decompiler.main.decompiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger.Severity;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

public class ConsoleDecompiler2 implements IBytecodeProvider, IResultSaver {
    private final File root;
    private final Fernflower fernflower;
    private final Map<String, ZipOutputStream> mapArchiveStreams;
    private final Map<String, Set<String>> mapArchiveEntries;

    public static void main(String[] args) {
        if(args.length<2||args==null||"".equals(args)){
            args=new String[]{"D:/web.jar ","D:/zzCode/"};
        }
        if(args.length < 2) {
            System.out.println("Usage: java -jar fernflower.jar [-<option>=<value>]* [<source>]+ <destination>\nExample: java -jar fernflower.jar -dgs=true c:\\my\\source\\ c:\\my.jar d:\\decompiled\\");
        } else {
            HashMap mapOptions = new HashMap();
            ArrayList lstSources = new ArrayList();
            ArrayList lstLibraries = new ArrayList();
            boolean isOption = true;

            for(int destination = 0; destination < args.length - 1; ++destination) {
                String logger = args[destination];
                if(isOption && logger.length() > 5 && logger.charAt(0) == 45 && logger.charAt(4) == 61) {
                    String decompiler = logger.substring(5);
                    if("true".equalsIgnoreCase(decompiler)) {
                        decompiler = "1";
                    } else if("false".equalsIgnoreCase(decompiler)) {
                        decompiler = "0";
                    }

                    mapOptions.put(logger.substring(1, 4), decompiler);
                } else {
                    isOption = false;
                    if(logger.startsWith("-e=")) {
                        addPath(lstLibraries, logger.substring(3));
                    } else {
                        addPath(lstSources, logger);
                    }
                }
            }

            if(lstSources.isEmpty()) {
                System.out.println("error: no sources given");
            } else {
                File var10 = new File(args[args.length - 1]);
                if(!var10.isDirectory()) {
                    System.out.println("error: destination \'" + var10 + "\' is not a directory");
                } else {
                    PrintStreamLogger var11 = new PrintStreamLogger(System.out);
                    ConsoleDecompiler var12 = new ConsoleDecompiler(var10, mapOptions, var11);
                    Iterator var8 = lstSources.iterator();

                    File library;
                    while(var8.hasNext()) {
                        library = (File)var8.next();
                        var12.addSpace(library, true);
                    }

                    var8 = lstLibraries.iterator();

                    while(var8.hasNext()) {
                        library = (File)var8.next();
                        var12.addSpace(library, false);
                    }

                    var12.decompileContext();
                }
            }
        }
    }

    private static void addPath(List<File> list, String path) {
        File file = new File(path);
        if(file.exists()) {
            list.add(file);
        } else {
            System.out.println("warn: missing \'" + path + "\', ignored");
        }

    }

    public ConsoleDecompiler2(File destination, Map<String, Object> options) {
        this(destination, options, new PrintStreamLogger(System.out));
    }

    protected ConsoleDecompiler2(File destination, Map<String, Object> options, IFernflowerLogger logger) {
        this.mapArchiveStreams = new HashMap();
        this.mapArchiveEntries = new HashMap();
        this.root = destination;
        this.fernflower = new Fernflower(this, this, options, logger);
    }

    public void addSpace(File file, boolean isOwn) {
        this.fernflower.getStructContext().addSpace(file, isOwn);
    }

    public void decompileContext() {
        try {
            this.fernflower.decompileContext();
        } finally {
            this.fernflower.clearContext();
        }

    }

    public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
        File file = new File(externalPath);
        if(internalPath == null) {
            return InterpreterUtil.getBytes(file);
        } else {
            ZipFile archive = new ZipFile(file);
            Throwable var5 = null;

            byte[] var7;
            try {
                ZipEntry entry = archive.getEntry(internalPath);
                if(entry == null) {
                    throw new IOException("Entry not found: " + internalPath);
                }

                var7 = InterpreterUtil.getBytes(archive, entry);
            } catch (Throwable var16) {
                var5 = var16;
                throw var16;
            } finally {
                if(archive != null) {
                    if(var5 != null) {
                        try {
                            archive.close();
                        } catch (Throwable var15) {
                            var5.addSuppressed(var15);
                        }
                    } else {
                        archive.close();
                    }
                }

            }

            return var7;
        }
    }

    private String getAbsolutePath(String path) {
        return (new File(this.root, path)).getAbsolutePath();
    }

    public void saveFolder(String path) {
        File dir = new File(this.getAbsolutePath(path));
        if(!dir.mkdirs() && !dir.isDirectory()) {
            throw new RuntimeException("Cannot create directory " + dir);
        }
    }

    public void copyFile(String source, String path, String entryName) {
        try {
            InterpreterUtil.copyFile(new File(source), new File(this.getAbsolutePath(path), entryName));
        } catch (IOException var5) {
            DecompilerContext.getLogger().writeMessage("Cannot copy " + source + " to " + entryName, var5);
        }

    }

    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        File file = new File(this.getAbsolutePath(path), entryName);

        try {
            OutputStreamWriter ex = new OutputStreamWriter(new FileOutputStream(file), "GBK");
            Throwable var8 = null;

            try {
                ex.write(content);
            } catch (Throwable var18) {
                var8 = var18;
                throw var18;
            } finally {
                if(ex != null) {
                    if(var8 != null) {
                        try {
                            ex.close();
                        } catch (Throwable var17) {
                            var8.addSuppressed(var17);
                        }
                    } else {
                        ex.close();
                    }
                }

            }
        } catch (IOException var20) {
            DecompilerContext.getLogger().writeMessage("Cannot write class file " + file, var20);
        }

    }

    public void createArchive(String path, String archiveName, Manifest manifest) {
        File file = new File(this.getAbsolutePath(path), archiveName);

        try {
            if(!file.createNewFile() && !file.isFile()) {
                throw new IOException("Cannot create file " + file);
            }

            FileOutputStream ex = new FileOutputStream(file);
            Object zipStream = manifest != null?new JarOutputStream(ex, manifest):new ZipOutputStream(ex);
            this.mapArchiveStreams.put(file.getPath(), (ZipOutputStream)zipStream);
        } catch (IOException var7) {
            DecompilerContext.getLogger().writeMessage("Cannot create archive " + file, var7);
        }

    }

    public void saveDirEntry(String path, String archiveName, String entryName) {
        this.saveClassEntry(path, archiveName, (String)null, entryName, (String)null);
    }

    public void copyEntry(String source, String path, String archiveName, String entryName) {
        String file = (new File(this.getAbsolutePath(path), archiveName)).getPath();
        if(this.checkEntry(entryName, file)) {
            try {
                ZipFile ex = new ZipFile(new File(source));
                Throwable message1 = null;

                try {
                    ZipEntry entry = ex.getEntry(entryName);
                    if(entry != null) {
                        InputStream in = ex.getInputStream(entry);
                        Throwable var10 = null;

                        try {
                            ZipOutputStream out = (ZipOutputStream)this.mapArchiveStreams.get(file);
                            out.putNextEntry(new ZipEntry(entryName));
                            InterpreterUtil.copyStream(in, out);
                        } catch (Throwable var35) {
                            var10 = var35;
                            throw var35;
                        } finally {
                            if(in != null) {
                                if(var10 != null) {
                                    try {
                                        in.close();
                                    } catch (Throwable var34) {
                                        var10.addSuppressed(var34);
                                    }
                                } else {
                                    in.close();
                                }
                            }

                        }
                    }
                } catch (Throwable var37) {
                    message1 = var37;
                    throw var37;
                } finally {
                    if(ex != null) {
                        if(message1 != null) {
                            try {
                                ex.close();
                            } catch (Throwable var33) {
                                message1.addSuppressed(var33);
                            }
                        } else {
                            ex.close();
                        }
                    }

                }
            } catch (IOException var39) {
                String message = "Cannot copy entry " + entryName + " from " + source + " to " + file;
                DecompilerContext.getLogger().writeMessage(message, var39);
            }

        }
    }

    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        String file = (new File(this.getAbsolutePath(path), archiveName)).getPath();
        if(this.checkEntry(entryName, file)) {
            try {
                ZipOutputStream ex = (ZipOutputStream)this.mapArchiveStreams.get(file);
                ex.putNextEntry(new ZipEntry(entryName));
                if(content != null) {
                    ex.write(content.getBytes("GBK"));
                }
            } catch (IOException var9) {
                String message = "Cannot write entry " + entryName + " to " + file;
                DecompilerContext.getLogger().writeMessage(message, var9);
            }

        }
    }

    private boolean checkEntry(String entryName, String file) {
        Object set = (Set)this.mapArchiveEntries.get(file);
        if(set == null) {
            this.mapArchiveEntries.put(file, (Set<String>)(set = new HashSet()));
        }

        boolean added = ((Set)set).add(entryName);
        if(!added) {
            String message = "Zip entry " + entryName + " already exists in " + file;
            DecompilerContext.getLogger().writeMessage(message, Severity.WARN);
        }

        return added;
    }

    public void closeArchive(String path, String archiveName) {
        String file = (new File(this.getAbsolutePath(path), archiveName)).getPath();

        try {
            this.mapArchiveEntries.remove(file);
            ((ZipOutputStream)this.mapArchiveStreams.remove(file)).close();
        } catch (IOException var5) {
            DecompilerContext.getLogger().writeMessage("Cannot close " + file, Severity.WARN);
        }

    }
}
