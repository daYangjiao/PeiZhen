package org.example.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThatCode;

class MyBatisMapperXmlTest {

    @Test
    void mapperXmlFilesShouldBeWellFormed() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (Resource resource : resolver.getResources("classpath*:mapper/*.xml")) {
            assertThatCode(() -> parseXml(resource))
                    .as(resource.getFilename())
                    .doesNotThrowAnyException();
        }
    }

    private static Document parseXml(Resource resource) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        try (InputStream inputStream = resource.getInputStream()) {
            return builder.parse(inputStream);
        }
    }
}
