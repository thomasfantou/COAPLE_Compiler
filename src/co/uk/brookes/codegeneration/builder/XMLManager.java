package co.uk.brookes.codegeneration.builder;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import co.uk.brookes.symboltable.Obj;
import co.uk.brookes.symboltable.Struct;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Author: Fantou Thomas
 * Date: 7/2/13
 */
public class XMLManager {
    DocumentBuilderFactory docFactory;
    DocumentBuilder docBuilder;
    Document doc;
    Element rootElement;
    Element constantElement;
    Element initElement;
    Element rootingElement;

    final String
        elem_def = "def",
        elem_comment = "comment",
        elem_constant = "constant_section",
            param_index = "index",
            elem_label = "utf8",
            elem_state = "state",
            elem_action = "action",
                elem_parameters = "params",
                param_count = "count",
                elem_param = "p",
            elem_evironment = "env",
                elem_scope = "scope",
                elem_castename = "castename",
                elem_type = "type",
                elem_url = "url",
            elem_caste = "caste",
                elem_ceurl = "ceurl",
                elem_cons = "cons",
                elem_c = "c",
                elem_states = "states",
                elem_s = "s",
                elem_actions = "actions",
                elem_a = "a",
                elem_envs = "envs",
                elem_e = "e",
            elem_name = "name",
            elem_typecode = "typecode",
        elem_init = "init",
        elem_rooting = "rooting";

    //create default file structure
    public XMLManager() {
        try {
            docFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        // root element
        doc = docBuilder.newDocument();
        rootElement = doc.createElement(elem_def);
        doc.appendChild(rootElement);

        // comment element
        Element comment = doc.createElement(elem_comment);
        rootElement.appendChild(comment);

        // constant element
        constantElement = doc.createElement(elem_constant);
        rootElement.appendChild(constantElement);

        // init element
        initElement = doc.createElement(elem_init);
        rootElement.appendChild(initElement);

        //rooting element
        rootingElement = doc.createElement(elem_rooting);
        rootElement.appendChild(rootingElement);
    }

    public void generateFile(ArrayList<Instruction> constants, ArrayList<Instruction> init, ArrayList<Instruction> rooting){
        StreamResult res = new StreamResult(System.out);

        for(Instruction c : constants) {
            switch(c.code) {
                case Instruction.label_: addLabel(c); break;
                case Instruction.state_ : addState(c); break;
                case Instruction.action_ : addAction(c); break;
                case Instruction.env_ : addEnv(c); break;
                case Instruction.caste_ : addCaste(c); break;
            }
        }


        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);

            transformer.transform(source, res);

        }
        catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    void addLabel(Instruction ins){
        Element labelElement = doc.createElement(elem_label);
        constantElement.appendChild(labelElement);

        // set attribute index
        Attr attr = doc.createAttribute(param_index);
        attr.setValue(String.valueOf(ins.index));
        labelElement.setAttributeNode(attr);

        labelElement.setAttributeNode(attr);
        labelElement.appendChild(doc.createTextNode(ins.val));
    }

    void addState(Instruction ins) {
        Element stateElement = doc.createElement(elem_state);
        constantElement.appendChild(stateElement);

        Attr attr = doc.createAttribute(param_index);
        attr.setValue(String.valueOf(ins.index));
        stateElement.setAttributeNode(attr);

        Element nameElement = doc.createElement(elem_name);
        stateElement.appendChild(nameElement);
        nameElement.appendChild(doc.createTextNode(ins.name));

        Element tcElement = doc.createElement(elem_typecode);
        stateElement.appendChild(tcElement);
        tcElement.appendChild(doc.createTextNode(Struct.values[ins.typeCode].toUpperCase()));
    }

    void addAction(Instruction ins) {
        Element actionElement = doc.createElement(elem_action);
        constantElement.appendChild(actionElement);

        Attr attr = doc.createAttribute(param_index);
        attr.setValue(String.valueOf(ins.index));
        actionElement.setAttributeNode(attr);

        Element nameElement = doc.createElement(elem_name);
        actionElement.appendChild(nameElement);
        nameElement.appendChild(doc.createTextNode(ins.name));

        Element paramElement = doc.createElement(elem_parameters);
        actionElement.appendChild(paramElement);
        Attr attrParamCount = doc.createAttribute(param_count);
        attrParamCount.setValue(String.valueOf(ins.nParam));
        paramElement.setAttributeNode(attrParamCount);

        for(Obj p = ins.params; p != null; p = p.next) {
            Element param = doc.createElement(elem_param);
            paramElement.appendChild(param);

            Element paramNameElement = doc.createElement(elem_name);
            param.appendChild(paramNameElement);
            paramNameElement.appendChild(doc.createTextNode(p.name));

            Element paramTcElement = doc.createElement(elem_typecode);
            param.appendChild(paramTcElement);
            paramTcElement.appendChild(doc.createTextNode(Struct.values[p.type.kind].toUpperCase()));
        }

    }

    void addEnv(Instruction ins) {
        Element envElement = doc.createElement(elem_evironment);
        constantElement.appendChild(envElement);

        Attr attr = doc.createAttribute(param_index);
        attr.setValue(String.valueOf(ins.index));
        envElement.setAttributeNode(attr);

        Element nameElement = doc.createElement(elem_name);
        envElement.appendChild(nameElement);
        nameElement.appendChild(doc.createTextNode(ins.name));

        Element casteNameElement = doc.createElement(elem_castename);
        envElement.appendChild(casteNameElement);
        casteNameElement.appendChild(doc.createTextNode(ins.casteName));

        Element typeElement = doc.createElement(elem_type);
        envElement.appendChild(typeElement);
        typeElement.appendChild(doc.createTextNode(ins.type));

        Element urlElement = doc.createElement(elem_url);
        envElement.appendChild(urlElement);
        urlElement.appendChild(doc.createTextNode(ins.url));

        Element scopeElement = doc.createElement(elem_scope);
        envElement.appendChild(scopeElement);
        scopeElement.appendChild(doc.createTextNode(ins.scope));

        Element tcElement = doc.createElement(elem_typecode);
        envElement.appendChild(tcElement);
        tcElement.appendChild(doc.createTextNode(Struct.values[ins.typeCode].toUpperCase()));
    }

    void addCaste(Instruction ins) {
        Element casteElement = doc.createElement(elem_caste);
        constantElement.appendChild(casteElement);

        Attr attr = doc.createAttribute(param_index);
        attr.setValue(String.valueOf(ins.index));
        casteElement.setAttributeNode(attr);

        Element nameElement = doc.createElement(elem_name);
        casteElement.appendChild(nameElement);
        nameElement.appendChild(doc.createTextNode(ins.name));

        Element ceurlElement = doc.createElement(elem_ceurl);
        casteElement.appendChild(ceurlElement);
        ceurlElement.appendChild(doc.createTextNode(ins.url));

        //cons
        Element consElement = doc.createElement(elem_cons);
        casteElement.appendChild(consElement);

        Attr attrCons = doc.createAttribute(param_count);
        attrCons.setValue(String.valueOf(ins.cons.length));
        consElement.setAttributeNode(attrCons);

        for(int i = 0; i < ins.cons.length; i++) {
            Element cons = doc.createElement(elem_c);
            consElement.appendChild(cons);
            cons.appendChild(doc.createTextNode(ins.cons[i]));
        }

        //states
        Element statesElement = doc.createElement(elem_states);
        casteElement.appendChild(statesElement);

        Attr attrStates = doc.createAttribute(param_count);
        attrStates.setValue(String.valueOf(ins.states.length));
        statesElement.setAttributeNode(attrStates);

        for(int i = 0; i < ins.states.length; i++) {
            Element states = doc.createElement(elem_s);
            statesElement.appendChild(states);
            states.appendChild(doc.createTextNode(ins.states[i]));
        }

        //actions
        Element actionsElement = doc.createElement(elem_actions);
        casteElement.appendChild(actionsElement);

        Attr attrActions = doc.createAttribute(param_count);
        attrActions.setValue(String.valueOf(ins.actions.length));
        actionsElement.setAttributeNode(attrActions);

        for(int i = 0; i < ins.actions.length; i++) {
            Element actions = doc.createElement(elem_a);
            actionsElement.appendChild(actions);
            actions.appendChild(doc.createTextNode(ins.actions[i]));
        }

        //envs
        Element envsElement = doc.createElement(elem_envs);
        casteElement.appendChild(envsElement);

        Attr attrEnvs = doc.createAttribute(param_count);
        attrEnvs.setValue(String.valueOf(ins.envs.length));
        envsElement.setAttributeNode(attrEnvs);

        for(int i = 0; i < ins.envs.length; i++) {
            Element envs = doc.createElement(elem_e);
            envsElement.appendChild(envs);
            envs.appendChild(doc.createTextNode(ins.envs[i]));
        }

    }

}
