package org.aion.mcf.config;

import com.google.common.base.Objects;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public final class CfgNetP2p {

    CfgNetP2p() {
        this.ip = "127.0.0.1";
        this.port = 30303;
        this.discover = false;
        this.bootlistSyncOnly = false;
        this.maxTempNodes = 128;
        this.maxActiveNodes = 128;
        this.errorTolerance = 50;
        this.clusterNodeMode = false;
        this.syncOnlyMode = false;
    }

    private String ip;

    private int port;

    private boolean discover;

    private boolean clusterNodeMode;

    private boolean bootlistSyncOnly;

    private boolean syncOnlyMode;

    private int maxTempNodes;

    private int maxActiveNodes;

    private int errorTolerance;

    public void fromXML(final XMLStreamReader sr) throws XMLStreamException {
        loop:
        while (sr.hasNext()) {
            int eventType = sr.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elelmentName = sr.getLocalName().toLowerCase();
                    switch (elelmentName) {
                        case "ip":
                            this.ip = Cfg.readValue(sr);
                            break;
                        case "port":
                            this.port = Integer.parseInt(Cfg.readValue(sr));
                            break;
                        case "discover":
                            this.discover = Boolean.parseBoolean(Cfg.readValue(sr));
                            break;
                        case "cluster-node-mode":
                            this.clusterNodeMode = Boolean.parseBoolean(Cfg.readValue(sr));
                            break;
                        case "bootlist-sync-only":
                            this.bootlistSyncOnly = Boolean.parseBoolean(Cfg.readValue(sr));
                            break;
                        case "sync-only-mode":
                            this.syncOnlyMode = Boolean.parseBoolean(Cfg.readValue(sr));
                            break;
                        case "max-temp-nodes":
                            this.maxTempNodes = Integer.parseInt(Cfg.readValue(sr));
                            break;
                        case "max-active-nodes":
                            this.maxActiveNodes = Integer.parseInt(Cfg.readValue(sr));
                            break;
                        case "err-tolerance":
                            this.errorTolerance = Integer.parseInt(Cfg.readValue(sr));
                            break;
                        default:
                            // Cfg.skipElement(sr);
                            break;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    break loop;
            }
        }
    }

    String toXML() {
        final XMLOutputFactory output = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter;
        String xml;
        try {
            Writer strWriter = new StringWriter();
            xmlWriter = output.createXMLStreamWriter(strWriter);
            xmlWriter.writeCharacters("\r\n\t\t");
            xmlWriter.writeStartElement("p2p");

            xmlWriter.writeCharacters("\r\n\t\t\t");
            xmlWriter.writeStartElement("ip");
            xmlWriter.writeCharacters(this.getIp());
            xmlWriter.writeEndElement();

            xmlWriter.writeCharacters("\r\n\t\t\t");
            xmlWriter.writeStartElement("port");
            xmlWriter.writeCharacters(this.getPort() + "");
            xmlWriter.writeEndElement();

            xmlWriter.writeCharacters("\r\n\t\t\t");
            xmlWriter.writeStartElement("discover");
            xmlWriter.writeCharacters(this.discover + "");
            xmlWriter.writeEndElement();

            xmlWriter.writeCharacters("\r\n\t\t\t");
            xmlWriter.writeStartElement("max-temp-nodes");
            xmlWriter.writeCharacters(this.maxTempNodes + "");
            xmlWriter.writeEndElement();

            xmlWriter.writeCharacters("\r\n\t\t\t");
            xmlWriter.writeStartElement("max-active-nodes");
            xmlWriter.writeCharacters(this.maxActiveNodes + "");
            xmlWriter.writeEndElement();

            xmlWriter.writeCharacters("\r\n\t\t");
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

    public void setIp(final String _ip) {
        this.ip = _ip;
    }

    public void setPort(final int _port) {
        this.port = _port;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public boolean getDiscover() {
        return this.discover;
    }

    public boolean getBootlistSyncOnly() {
        return bootlistSyncOnly;
    }

    public int getMaxTempNodes() {
        return maxTempNodes;
    }

    public int getMaxActiveNodes() {
        return maxActiveNodes;
    }

    public int getErrorTolerance() {
        return errorTolerance;
    }

    public boolean inClusterNodeMode() {
        return clusterNodeMode;
    }

    public boolean inSyncOnlyMode() {
        return syncOnlyMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CfgNetP2p cfgNetP2p = (CfgNetP2p) o;
        return port == cfgNetP2p.port
                && discover == cfgNetP2p.discover
                && clusterNodeMode == cfgNetP2p.clusterNodeMode
                && bootlistSyncOnly == cfgNetP2p.bootlistSyncOnly
                && syncOnlyMode == cfgNetP2p.syncOnlyMode
                && maxTempNodes == cfgNetP2p.maxTempNodes
                && maxActiveNodes == cfgNetP2p.maxActiveNodes
                && errorTolerance == cfgNetP2p.errorTolerance
                && Objects.equal(ip, cfgNetP2p.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                ip,
                port,
                discover,
                clusterNodeMode,
                bootlistSyncOnly,
                syncOnlyMode,
                maxTempNodes,
                maxActiveNodes,
                errorTolerance);
    }
}
