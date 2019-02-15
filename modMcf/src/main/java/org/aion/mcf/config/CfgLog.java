package org.aion.mcf.config;

import com.google.common.base.Objects;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.aion.log.LogEnum;
import org.aion.log.LogLevel;

/** @author chris */
public class CfgLog {

    private Map<String, String> modules;
    // TODO: rename to enabled; current name leads to confusion
    boolean logFile;
    String logPath;

    public CfgLog() {
        modules = new HashMap<>();
        modules.put(LogEnum.ROOT.name(), LogLevel.WARN.name());
        modules.put(LogEnum.CONS.name(), LogLevel.INFO.name());
        modules.put(LogEnum.GEN.name(), LogLevel.INFO.name());
        modules.put(LogEnum.VM.name(), LogLevel.ERROR.name());
        modules.put(LogEnum.DB.name(), LogLevel.ERROR.name());
        modules.put(LogEnum.SYNC.name(), LogLevel.INFO.name());
        modules.put(LogEnum.API.name(), LogLevel.INFO.name());
        modules.put(LogEnum.P2P.name(), LogLevel.INFO.name());
        modules.put(LogEnum.TX.name(), LogLevel.ERROR.name());
        modules.put(LogEnum.TXPOOL.name(), LogLevel.ERROR.name());
        modules.put(LogEnum.GUI.name(), LogLevel.INFO.name());
        this.logFile = false;
        this.logPath = "log";
    }

    public void fromXML(final XMLStreamReader sr) throws XMLStreamException {
        this.modules = new HashMap<>();
        loop:
        while (sr.hasNext()) {
            int eventType = sr.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    {
                        /* XML - Takes the input in config.xml and parse as T/F */
                        String elementName = sr.getLocalName().toLowerCase();
                        switch (elementName) {
                            case "log-file":
                                this.logFile = Boolean.parseBoolean(Cfg.readValue(sr));
                                break;
                            case "log-path":
                                this.logPath = Cfg.readValue(sr);
                                break;
                            default:
                                if (LogEnum.contains(elementName))
                                    this.modules.put(elementName, Cfg.readValue(sr).toUpperCase());
                                break;
                        }
                        break;
                    }
                case XMLStreamReader.END_ELEMENT:
                    break loop;
                default:
                    break;
            }
        }
    }

    public String toXML() {
        final XMLOutputFactory output = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter;
        String xml;
        try {
            Writer strWriter = new StringWriter();
            xmlWriter = output.createXMLStreamWriter(strWriter);
            xmlWriter.writeCharacters("\r\n\t");
            xmlWriter.writeStartElement("log");
            xmlWriter.writeCharacters("\r\n");

            /*
             * XML - Displays tag/entry in the config.xml
             * Boolean value to allow logger to be toggled ON and OFF
             */
            xmlWriter.writeCharacters("\t\t");
            xmlWriter.writeComment(
                    "Enable/Disable logback service; if disabled, output will not be logged.");
            xmlWriter.writeCharacters("\r\n\t\t");
            xmlWriter.writeStartElement("log-file");
            xmlWriter.writeCharacters(this.logFile + "");
            xmlWriter.writeEndElement();
            xmlWriter.writeCharacters("\r\n");

            /*
             * XML - Displays log-path in the config.xml
             * String value to determine the folder path for log files
             */
            xmlWriter.writeCharacters("\t\t");
            xmlWriter.writeComment(
                    "Sets the physical location on disk where log files will be stored.");
            xmlWriter.writeCharacters("\r\n\t\t");
            xmlWriter.writeStartElement("log-path");
            xmlWriter.writeCharacters(this.logPath + "");
            xmlWriter.writeEndElement();
            xmlWriter.writeCharacters("\r\n");

            for (Map.Entry<String, String> module : this.modules.entrySet()) {
                xmlWriter.writeCharacters("\t\t");
                xmlWriter.writeStartElement(module.getKey().toUpperCase());
                xmlWriter.writeCharacters(module.getValue().toUpperCase());
                xmlWriter.writeEndElement();
                xmlWriter.writeCharacters("\r\n");
            }
            xmlWriter.writeCharacters("\t");
            xmlWriter.writeEndElement();
            xml = strWriter.toString();
            strWriter.flush();
            strWriter.close();
            xmlWriter.flush();
            xmlWriter.close();
            return xml;
        } catch (IOException | XMLStreamException e) {
            return "";
        }
    }

    public Map<String, String> getModules() {
        return this.modules;
    }

    public void setLogPath(String value) {
        logPath = value;
    }

    /** Method checks whether LOGGER is enabled/disabled */
    public boolean getLogFile() {
        return this.logFile;
    }

    /** Used to turn off logging in case of incorrect configuration. */
    public void disableLogging() {
        this.logFile = false;
    }

    /** Method returns user input folder path of logger */
    public String getLogPath() {
        return logPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CfgLog cfgLog = (CfgLog) o;
        return logFile == cfgLog.logFile
                && Objects.equal(modules, cfgLog.modules)
                && Objects.equal(logPath, cfgLog.logPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(modules, logFile, logPath);
    }
}
