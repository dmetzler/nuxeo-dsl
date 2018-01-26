package org.nuxeo.dsl.builder;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.XMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.io.Files;

public class BuildContext implements Closeable {

    private XMap xmap;

    private static final Log log = LogFactory.getLog(BuildContext.class);

    private Map<String, Map<String, List<Object>>> xp = new HashMap<>();

    private File buildDir;

    private File extension;

    private File osgi;

    private String projectId;

    private BuildContext(String projectId) {
        this.projectId = projectId;
        xmap = new XMap();

        buildDir = Files.createTempDir();
        log.info("Starting build in " + buildDir.getAbsolutePath());

        File file = new File(buildDir, "META-INF");
        file.mkdir();
        File manifestFile = new File(file, "MANIFEST.MF");
        createManifest(manifestFile, "dsl_studio");
        osgi = new File(buildDir, "OSGI-INF");
        osgi.mkdir();

        extension = new File(osgi, "extensions.xml");

        try {
            if (!extension.createNewFile()) {
                log.warn("Extension file was not created properly: " + extension);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public File getBuildDir() {
        return buildDir;
    }

    protected void createManifest(File manifestFile, String id) {
        try {
            String manifestContent = generateManifestContent(id);
            FileUtils.writeStringToFile(manifestFile, manifestContent, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateManifestContent(String projectId) throws IOException {
        String id = "dslstudio.extensions." + projectId;

        TextTemplate tt = new TextTemplate();
        tt.setVariable("id", id);
        String content = null;
        try (InputStream in = getClass().getResourceAsStream("/templates/MANIFEST.MF")) {
            content = tt.processText(in);
        }

        return content;
    }

    @Override
    public void close() {
        FileUtils.deleteQuietly(buildDir);
    }

    public File buildJar(File destDir) throws IOException {
        writeExtensions();

        File jar = new File(destDir, getProjectId() + "-1.0.0-SNAPSHOT.jar");
        ZipUtils.zip(buildDir.listFiles(), jar);
        log.info("Builded " + jar);
        return jar;
    }

    public String getProjectId() {
        return projectId;
    }

    private void writeExtensions() throws IOException {

        DocumentBuilderFactory dbfac = XMap.getFactory();
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbfac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        Document doc = docBuilder.newDocument();
        // create root element
        Element component = doc.createElement("component");
        component.setAttribute("name", "org.nuxeo.ecm.dsl.contrib");

        doc.appendChild(component);

        for (Entry<String, Map<String, List<Object>>> entry : xp.entrySet()) {
            String componentName = entry.getKey();
            for (Entry<String, List<Object>> xpEntry : entry.getValue().entrySet()) {
                String xpName = xpEntry.getKey();

                Element contrib = doc.createElement("extension");
                contrib.setAttribute("target", componentName);
                contrib.setAttribute("point", xpName);

                component.appendChild(contrib);

                for (Object contribution : xpEntry.getValue()) {
                    if (contribution instanceof XmlWriter) {
                        ((XmlWriter) contribution).toXml(doc, contrib);
                    } else {
                        xmap.toXML(contribution, contrib);
                    }
                }

            }
        }

        FileUtils.writeStringToFile(extension, DOMSerializer.toString(component), Charset.forName("UTF-8"));
    }

    public void registerXMap(Class<?> klass) {
        xmap.register(klass);
    }

    public static BuildContext newContext(String projectId) {
        return new BuildContext(projectId);
    }

    public void registerXP(String componentName, String extensionPoint, Object contribution) {
        if (!xp.containsKey(componentName)) {
            xp.put(componentName, new HashMap<>());
        }

        if (!xp.get(componentName).containsKey(extensionPoint)) {
            xp.get(componentName).put(extensionPoint, new ArrayList<>());
        }

        xp.get(componentName).get(extensionPoint).add(contribution);
    }

}
