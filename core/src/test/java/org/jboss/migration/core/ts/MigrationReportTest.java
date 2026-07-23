/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.migration.core.ts;

import org.jboss.migration.core.report.HtmlReportWriter;
import org.jboss.migration.core.report.SummaryReportWriter;
import org.jboss.migration.core.report.XmlReportWriter;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.StringWriter;

import static org.jboss.migration.core.ts.MigrationTasksTest.migrationData;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MigrationReportTest {
    @Test
    public void success_summary() throws IOException {
        String summary = SummaryReportWriter.INSTANCE.toString(migrationData(false));

        assertTrue(summary.contains("subtask 1"));
        assertTrue(summary.contains("subtask 1.1"));
        assertTrue(summary.contains("subtask 1.2"));
        assertTrue(summary.contains("config=foobar"));
        assertTrue(summary.contains("config=quux"));
        assertFalse(summary.contains("subtask 2"));
        assertFalse(summary.contains("subtask 3"));

        assertTrue(summary.contains(ServerMigrationTaskResult.Status.SUCCESS.toString()));
        assertFalse(summary.contains(ServerMigrationTaskResult.Status.SKIPPED.toString()));
        assertFalse(summary.contains(ServerMigrationTaskResult.Status.FAIL.toString()));

        assertFalse(summary.contains("unused.property"));
        assertFalse(summary.contains("test.property.key"));
        assertFalse(summary.contains("test.property.value"));
    }

    @Test
    public void fail_summary() throws IOException {
        String summary = SummaryReportWriter.INSTANCE.toString(migrationData(true));

        assertTrue(summary.contains("subtask 1"));
        assertTrue(summary.contains("subtask 1.1"));
        assertTrue(summary.contains("subtask 1.2"));
        assertTrue(summary.contains("config=foobar"));
        assertTrue(summary.contains("config=quux"));
        assertFalse(summary.contains("subtask 2"));
        assertTrue(summary.contains("subtask 3"));
        assertTrue(summary.contains("always=fails"));

        assertTrue(summary.contains(ServerMigrationTaskResult.Status.SUCCESS.toString()));
        assertFalse(summary.contains(ServerMigrationTaskResult.Status.SKIPPED.toString()));
        assertTrue(summary.contains(ServerMigrationTaskResult.Status.FAIL.toString()));

        assertFalse(summary.contains("unused.property"));
        assertFalse(summary.contains("test.property.key"));
        assertFalse(summary.contains("test.property.value"));
    }

    @Test
    public void success_html() throws IOException {
        String htmlTemplate = "$SUMMARY \n $ENVIRONMENT \n $TASK_SUMMARY \n $TASK_MAP";
        HtmlReportWriter.ReportTemplate reportTemplate = HtmlReportWriter.ReportTemplate.from(htmlTemplate);
        String html = HtmlReportWriter.INSTANCE.toString(migrationData(false), reportTemplate);

        assertTrue(html.contains("subtask 1"));
        assertTrue(html.contains("subtask 1.1"));
        assertTrue(html.contains("subtask 1.2"));
        assertTrue(html.contains("config=foobar"));
        assertTrue(html.contains("config=quux"));
        assertTrue(html.contains("subtask 2"));
        assertFalse(html.contains("subtask 3"));

        assertTrue(html.contains(ServerMigrationTaskResult.Status.SUCCESS.toString()));
        assertTrue(html.contains(ServerMigrationTaskResult.Status.SKIPPED.toString()));
        assertFalse(html.contains(ServerMigrationTaskResult.Status.FAIL.toString()));

        assertFalse(html.contains("unused.property"));
        assertTrue(html.contains("test.property.key"));
        assertTrue(html.contains("test.property.value"));
    }

    @Test
    public void fail_html() throws IOException {
        String htmlTemplate = "$SUMMARY \n $ENVIRONMENT \n $TASK_SUMMARY \n $TASK_MAP";
        HtmlReportWriter.ReportTemplate reportTemplate = HtmlReportWriter.ReportTemplate.from(htmlTemplate);
        String html = HtmlReportWriter.INSTANCE.toString(migrationData(true), reportTemplate);

        assertTrue(html.contains("subtask 1"));
        assertTrue(html.contains("subtask 1.1"));
        assertTrue(html.contains("subtask 1.2"));
        assertTrue(html.contains("config=foobar"));
        assertTrue(html.contains("config=quux"));
        assertTrue(html.contains("subtask 2"));
        assertTrue(html.contains("subtask 3"));
        assertTrue(html.contains("always=fails"));

        assertTrue(html.contains(ServerMigrationTaskResult.Status.SUCCESS.toString()));
        assertTrue(html.contains(ServerMigrationTaskResult.Status.SKIPPED.toString()));
        assertTrue(html.contains(ServerMigrationTaskResult.Status.FAIL.toString()));

        assertFalse(html.contains("unused.property"));
        assertTrue(html.contains("test.property.key"));
        assertTrue(html.contains("test.property.value"));
    }

    @Test
    public void success_xml() throws XMLStreamException {
        StringWriter xmlData = new StringWriter();
        XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(xmlData);
        XmlReportWriter.INSTANCE.writeContent(xmlStreamWriter, migrationData(false));
        String xml = xmlData.toString();

        assertTrue(xml.contains("subtask 1"));
        assertTrue(xml.contains("subtask 1.1"));
        assertTrue(xml.contains("subtask 1.2"));
        assertTrue(xml.contains("config=foobar"));
        assertTrue(xml.contains("config=quux"));
        assertTrue(xml.contains("subtask 2"));
        assertFalse(xml.contains("subtask 3"));

        assertTrue(xml.contains(ServerMigrationTaskResult.Status.SUCCESS.toString()));
        assertTrue(xml.contains(ServerMigrationTaskResult.Status.SKIPPED.toString()));
        assertFalse(xml.contains(ServerMigrationTaskResult.Status.FAIL.toString()));

        assertFalse(xml.contains("unused.property"));
        assertTrue(xml.contains("test.property.key"));
        assertTrue(xml.contains("test.property.value"));
    }

    @Test
    public void fail_xml() throws XMLStreamException {
        StringWriter xmlData = new StringWriter();
        XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(xmlData);
        XmlReportWriter.INSTANCE.writeContent(xmlStreamWriter, migrationData(true));
        String xml = xmlData.toString();

        assertTrue(xml.contains("subtask 1"));
        assertTrue(xml.contains("subtask 1.1"));
        assertTrue(xml.contains("subtask 1.2"));
        assertTrue(xml.contains("config=foobar"));
        assertTrue(xml.contains("config=quux"));
        assertTrue(xml.contains("subtask 2"));
        assertTrue(xml.contains("subtask 3"));
        assertTrue(xml.contains("always=fails"));

        assertTrue(xml.contains(ServerMigrationTaskResult.Status.SUCCESS.toString()));
        assertTrue(xml.contains(ServerMigrationTaskResult.Status.SKIPPED.toString()));
        assertTrue(xml.contains(ServerMigrationTaskResult.Status.FAIL.toString()));

        assertFalse(xml.contains("unused.property"));
        assertTrue(xml.contains("test.property.key"));
        assertTrue(xml.contains("test.property.value"));
    }
}
