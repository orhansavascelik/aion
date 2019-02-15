package org.aion.mcf.config;

import com.google.common.base.Objects;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Printing reports for debugging purposes.
 *
 * @author Alexandra Roatis
 */
public class CfgReports {

    private boolean print = false;

    private boolean enable;
    private String path;
    private int dump_interval;
    private int block_frequency;
    private boolean enable_heap_dumps;
    private int heap_dump_interval;

    public CfgReports() {
        // default configuration
        this.enable = false;
        this.path = "reports";
        this.dump_interval = 10000;
        this.block_frequency = 500;
        this.enable_heap_dumps = false;
        this.heap_dump_interval = 100000;
    }

    public void fromXML(final XMLStreamReader sr) throws XMLStreamException {
        this.print = true;
        loop:
        while (sr.hasNext()) {
            int eventType = sr.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = sr.getLocalName().toLowerCase();
                    switch (elementName) {
                        case "enable":
                            this.enable = Boolean.parseBoolean(Cfg.readValue(sr));
                            break;
                        case "path":
                            this.path = Cfg.readValue(sr);
                            break;
                        case "dump_interval":
                            this.dump_interval = Integer.parseInt(Cfg.readValue(sr));
                            break;
                        case "block_frequency":
                            this.block_frequency = Integer.parseInt(Cfg.readValue(sr));
                            break;
                        case "enable_heap_dumps":
                            this.enable_heap_dumps = Boolean.parseBoolean(Cfg.readValue(sr));
                            break;
                        case "heap_dump_interval":
                            this.heap_dump_interval = Integer.parseInt(Cfg.readValue(sr));
                            break;
                        default:
                            Cfg.skipElement(sr);
                            break;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    break loop;
            }
        }
    }

    public String toXML() {
        if (print) {
            final XMLOutputFactory output = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlWriter;
            String xml;
            try {
                Writer strWriter = new StringWriter();
                xmlWriter = output.createXMLStreamWriter(strWriter);
                xmlWriter.writeCharacters("\r\n\t");
                xmlWriter.writeStartElement("reports");

                xmlWriter.writeCharacters("\r\n\t\t");
                xmlWriter.writeStartElement("enable");
                xmlWriter.writeCharacters(String.valueOf(this.isEnabled()));
                xmlWriter.writeEndElement();

                xmlWriter.writeCharacters("\r\n\t\t");
                xmlWriter.writeStartElement("path");
                xmlWriter.writeCharacters(this.getPath());
                xmlWriter.writeEndElement();

                xmlWriter.writeCharacters("\r\n\t\t");
                xmlWriter.writeStartElement("dump_interval");
                xmlWriter.writeCharacters(String.valueOf(this.getDumpInterval()));
                xmlWriter.writeEndElement();

                xmlWriter.writeCharacters("\r\n\t\t");
                xmlWriter.writeStartElement("block_frequency");
                xmlWriter.writeCharacters(String.valueOf(this.getBlockFrequency()));
                xmlWriter.writeEndElement();

                xmlWriter.writeCharacters("\r\n\t\t");
                xmlWriter.writeStartElement("enable_heap_dumps");
                xmlWriter.writeCharacters(String.valueOf(this.isHeapDumpEnabled()));
                xmlWriter.writeEndElement();

                xmlWriter.writeCharacters("\r\n\t\t");
                xmlWriter.writeStartElement("heap_dump_interval");
                xmlWriter.writeCharacters(String.valueOf(this.getHeapDumpInterval()));
                xmlWriter.writeEndElement();

                xmlWriter.writeCharacters("\r\n\t");
                xmlWriter.writeEndElement();
                xml = strWriter.toString();
                strWriter.flush();
                strWriter.close();
                xmlWriter.flush();
                xmlWriter.close();
                return xml;
            } catch (IOException | XMLStreamException e) {
                e.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    public boolean isEnabled() {
        return enable;
    }

    public String getPath() {
        return this.path;
    }

    public int getDumpInterval() {
        return this.dump_interval;
    }

    public int getBlockFrequency() {
        return this.block_frequency;
    }

    public boolean isHeapDumpEnabled() {
        return enable_heap_dumps;
    }

    public int getHeapDumpInterval() {
        return this.heap_dump_interval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CfgReports that = (CfgReports) o;
        return print == that.print
                && enable == that.enable
                && dump_interval == that.dump_interval
                && block_frequency == that.block_frequency
                && enable_heap_dumps == that.enable_heap_dumps
                && heap_dump_interval == that.heap_dump_interval
                && Objects.equal(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                print,
                enable,
                path,
                dump_interval,
                block_frequency,
                enable_heap_dumps,
                heap_dump_interval);
    }
}
