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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void completedAttendantIncomeShouldUsePostCommissionFinalOrderAmount() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("classpath:mapper/OrderMapper.xml");
        String xml;
        try (InputStream inputStream = resource.getInputStream()) {
            xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int start = xml.indexOf("<select id=\"sumCompletedIncome\"");
        int end = xml.indexOf("</select>", start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        String selectSql = xml.substring(start, end);

        assertThat(selectSql).contains("refund_amount");
        assertThat(selectSql).contains("balance_amount");
        assertThat(selectSql).contains("GREATEST(order_amount - refund_amount, 0)");
        assertThat(selectSql).doesNotContain("order_amount + COALESCE(balance_amount, 0)");
    }

    @Test
    void orderDisputeMigrationShouldCoverAllPersistedResolutionColumns() throws Exception {
        String migration = Files.readString(
                Path.of("db/20260426_add_order_admin_remark.sql"),
                StandardCharsets.UTF_8
        );

        assertThat(migration).contains("admin_remark");
        assertThat(migration).contains("dispute_resolved_by");
        assertThat(migration).contains("dispute_resolved_time");
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
