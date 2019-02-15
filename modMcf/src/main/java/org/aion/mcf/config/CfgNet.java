package org.aion.mcf.config;

import com.google.common.base.Objects;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/** @author chris */
public final class CfgNet {

    private static final boolean SINGLE = false;

    private int id;

    public CfgNet() {
        this.id = 256;
        this.nodes = new String[0];
        this.p2p = new CfgNetP2p();
    }

    protected String[] nodes;

    protected CfgNetP2p p2p;

    public void fromXML(final XMLStreamReader sr) throws XMLStreamException {
        loop:
        while (sr.hasNext()) {
            int eventType = sr.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = sr.getLocalName().toLowerCase();
                    switch (elementName) {
                        case "id":
                            int _id = Integer.parseInt(Cfg.readValue(sr));
                            this.id = _id < 0 ? 0 : _id;
                            break;
                        case "nodes":
                            List<String> nodes = new ArrayList<>();
                            loopNode:
                            while (sr.hasNext()) {
                                int eventType1 = sr.next();
                                switch (eventType1) {
                                    case XMLStreamReader.START_ELEMENT:
                                        nodes.add(Cfg.readValue(sr));
                                        break;
                                    case XMLStreamReader.END_ELEMENT:
                                        this.nodes = nodes.toArray(new String[nodes.size()]);
                                        break loopNode;
                                }
                            }
                            break;
                        case "p2p":
                            this.p2p.fromXML(sr);
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
        final XMLOutputFactory output = XMLOutputFactory.newInstance();
        output.setProperty("escapeCharacters", false);
        XMLStreamWriter xmlWriter;
        String xml;
        try {
            Writer strWriter = new StringWriter();
            xmlWriter = output.createXMLStreamWriter(strWriter);

            // start element net
            xmlWriter.writeCharacters("\r\n\t");
            xmlWriter.writeStartElement("net");

            // sub-element id
            xmlWriter.writeCharacters("\r\n\t\t");
            xmlWriter.writeStartElement("id");
            xmlWriter.writeCharacters(this.id + "");
            xmlWriter.writeEndElement();

            // sub-element nodes
            xmlWriter.writeCharacters("\r\n\t\t");
            xmlWriter.writeStartElement("nodes");
            for (String node : nodes) {
                xmlWriter.writeCharacters("\r\n\t\t\t");
                xmlWriter.writeStartElement("node");
                xmlWriter.writeCharacters(node);
                xmlWriter.writeEndElement();
            }
            xmlWriter.writeCharacters("\r\n\t\t");
            xmlWriter.writeEndElement();

            // sub-element p2p
            xmlWriter.writeCharacters(this.p2p.toXML());

            // close element net
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
    }

    public void setNodes(String[] _nodes) {
        if (SINGLE) this.nodes = new String[0];
        else this.nodes = _nodes;
    }

    public int getId() {
        return this.id;
    }

    public String[] getNodes() {
        if (SINGLE) return new String[0];
        else return this.nodes;
    }

    public CfgNetP2p getP2p() {
        return this.p2p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CfgNet cfgNet = (CfgNet) o;
        return id == cfgNet.id
                && Objects.equal(nodes, cfgNet.nodes)
                && Objects.equal(p2p, cfgNet.p2p);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, nodes, p2p);
    }
}
