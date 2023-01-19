import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class TestXmlParser {
    @BeforeAll
    static void setup() throws Exception {
        new DataCreator().create();
    }

    @Test
    void testFullXml10() throws Exception {
        testFull("sample-small-10.xml");
    }

    @Test
    void testFullRewriteXml10() throws Exception {
        testFull("sample-small-10-rewrite.xml");
    }

    @Test
    void testFragmentXml10() throws Exception {
        testFragment("fragment-wrapper_1_0.xml", "sample-fragment-small.xml");
    }

    @Test
    void testFullXml11() throws Exception {
        testFull("sample-small-11.xml");
    }

    @Test
    void testFullRewriteXml11() throws Exception {
        testFull("sample-small-11-rewrite.xml");
    }

    @Test
    void testFragmentXml11() throws Exception {
        testFragment("fragment-wrapper_1_1.xml", "sample-fragment-small.xml");
    }

    private void testFull(final String fileName) throws Exception {
        final Path dir = DataCreator.getDir();
        final Path inputPath = dir.resolve(fileName);

        final JsonValidator jsonValidator = new JsonValidator();
        try (final Reader input = Files.newBufferedReader(inputPath)) {
            final EntityResolver resolver = new NullEntityResolver();
            SaxUtil.parse(input, jsonValidator, resolver);
        }
        assertThat(jsonValidator.getErrorCount()).isZero();
    }

    private void testFragment(final String wrapperFileName,
                              final String fragmentFileName) throws Exception {
        final Path dir = DataCreator.getDir();
        final Path wrapperPath = dir.resolve(wrapperFileName);
        final Path fragmentPath = dir.resolve(fragmentFileName);

        final JsonValidator jsonValidator = new JsonValidator();
        try (final Reader wrapper = Files.newBufferedReader(wrapperPath)) {
            try (final Reader fragment = Files.newBufferedReader(fragmentPath)) {
                final EntityResolver resolver = new FragmentEntity(new InputSource(fragment));
                SaxUtil.parse(wrapper, jsonValidator, resolver);
            }
        }
        assertThat(jsonValidator.getErrorCount()).isZero();
    }
}
