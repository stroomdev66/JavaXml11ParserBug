import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataCreator {
    public void create() throws Exception {
        final Path dir = getDir();
        final Path jsonLocation = dir.resolve("input.json");

        String json = Files.readString(jsonLocation, StandardCharsets.UTF_8);

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
