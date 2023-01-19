import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DataCreator {
    public void create() throws Exception {
        final Path dir = getDir();
        final Path jsonLocation = dir.resolve("input.json");

        String json = Files.readString(jsonLocation, StandardCharsets.UTF_8);

//        String out = modifyJson(json);
//        final Path outp = dir.resolve("output.json");
//        Files.writeString(outp, out, StandardCharsets.UTF_8);
//        json = out;


        // Write a fragment sample.
        final Path outLocationFragment = dir.resolve("sample-fragment-small.xml");
        try (final Writer writer = Files.newBufferedWriter(outLocationFragment, StandardCharsets.UTF_8)) {
            addRows(writer, json);
        }

        // Write a full XML 1.0 file.
        final Path outLocation10 = dir.resolve("sample-small-10.xml");
        try (final Writer writer = Files.newBufferedWriter(outLocation10, StandardCharsets.UTF_8)) {
            writeFull(writer, json, "1.0");
        }

        // Write a full XML 1.1 file.
        final Path outLocation11 = dir.resolve("sample-small-11.xml");
        try (final Writer writer = Files.newBufferedWriter(outLocation11, StandardCharsets.UTF_8)) {
            writeFull(writer, json, "1.1");
        }

        // Rewrite the XML 1.0 file with the XML parser.
        final Path outLocation10Rewrite = dir.resolve("sample-small-10-rewrite.xml");
        rewrite(outLocation10, outLocation10Rewrite);

        // Because we think parsing XML 1.1 is flawed create an XML 1.1 by doing a version replacement on 1.0.
        final Path outLocation11Rewrite = dir.resolve("sample-small-11-rewrite.xml");
        replace(outLocation10Rewrite, outLocation11Rewrite, "version=\"1.0\"", "version=\"1.1\"");

        // Rewrite a deliberately flawed XML 1.1 file with the XML parser.
        final Path outLocation11BadRewrite = dir.resolve("sample-small-11-bad-rewrite.xml");
        rewrite(outLocation11, outLocation11BadRewrite);
    }

    private String modifyJson(String json) {
        int start = 0;
        start = json.indexOf("\"", start);
        final Set<String> names = new HashSet<>();
        while (start  != -1) {
            int end = json.indexOf("\"", start + 1);
            if (end != -1) {
                final String text = json.substring(start, end + 1);
                names.add(text);
            }
            start = json.indexOf("\"", end + 1);
        }

        final Map<String, String> mappings = new HashMap<>();
        names.forEach(name -> {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            while (uuid.length() < name.length() - 2) {
                uuid += UUID.randomUUID().toString().replace("-", "");
            }
            final String rand = uuid.substring(0, name.length() - 2);
            mappings.put(name, "\"" + rand + "\"");
        });

        String out = json;
        for (final Map.Entry<String, String> entry : mappings.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }

        return out;
    }

    private void addArray(final StringBuilder sb,
                          final String parentId,
                          final int depth,
                          final int maxDepth) {
        sb.append("[");
        for (int i = 0; i < 100; i++) {
            sb.append("\n");
            addObject(sb, parentId + "_" + i, depth, maxDepth);
            sb.append(",");
        }
        // Strip last comma.
        sb.setLength(sb.length() - 1);

        sb.append("\n]");
    }

    private void addObject(final StringBuilder sb,
                          final String id,
                          final int depth,
                          final int maxDepth) {
        sb.append("{");
        sb.append("\"key_");
        sb.append(id);
        sb.append("\": ");

        if (depth < maxDepth) {
            addArray(sb, id, depth + 1, maxDepth);
        } else {
            addValue(sb, id);
        }

        sb.append("}");
    }

    private void addValue(final StringBuilder sb,
                           final String id) {
        sb.append("\"value_");
        sb.append(id);
        sb.append("\"");
    }

    private void writeFull(final Writer writer,
                           final String json,
                           final String xmlVersion) throws Exception {
        writer.write("<?xml version=\"");
        writer.write(xmlVersion);
        writer.write("\" encoding=\"utf-8\"?>");
        writer.write("\n");
        writer.write("<root>");
        writer.write("\n");
        addRows(writer, json);
        writer.write("\n");
        writer.write("</root>");
        writer.write("\n");
    }

    private void addRows(final Writer writer,
                         final String json) throws Exception {
        for (int i = 0; i < 100; i++) {
            writer.write("<row>");
            writer.write("\n");
            writer.write(json);
            writer.write("\n");
            writer.write("</row>");
            writer.write("\n");
        }
    }

    private void rewrite(final Path inputPath,
                         final Path outputPath) throws Exception {
        try (final Reader input = Files.newBufferedReader(inputPath)) {
            try (final Writer output = Files.newBufferedWriter(outputPath)) {
                final TransformerFactory factory = TransformerFactory.newInstance();
                final TransformerHandler th = ((SAXTransformerFactory) factory).newTransformerHandler();
                th.setResult(new StreamResult(output));
                SaxUtil.parse(input, th, new NullEntityResolver());
            }
        }
    }

    private void replace(final Path inputPath,
                         final Path outputPath,
                         final String match,
                         final String replacement) throws Exception {
        try (final Reader input = Files.newBufferedReader(inputPath)) {
            try (final Writer output = Files.newBufferedWriter(outputPath)) {
                final char[] buffer = new char[4096];
                int len = 0;
                while ((len = input.read(buffer)) != -1) {
                    final String string = new String(buffer, 0, len);
                    final String replaced = string.replace(match, replacement);
                    output.write(replaced);
                }
            }
        }
    }

    public static Path getDir() {
        return FileUtil.resolveDir("src").resolve("test").resolve("resources");
    }
}
