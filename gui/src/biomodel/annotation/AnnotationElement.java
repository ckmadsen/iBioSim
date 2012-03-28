package biomodel.annotation;

import java.util.LinkedList;

public class AnnotationElement {
	
	private String prefix;
	private String name;
	private LinkedList<AnnotationAttribute> attributes = new LinkedList<AnnotationAttribute>();
	private LinkedList<AnnotationNamespace> namespaces = new LinkedList<AnnotationNamespace>();
	private LinkedList<AnnotationElement> children = new LinkedList<AnnotationElement>();
	
	public AnnotationElement(String name) {
		this.name = name;
	}
	
	public AnnotationElement(String prefix, String name) {
		this.prefix = prefix;
		this.name = name;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public String getName() {
		return name;
	}
	
	public LinkedList<AnnotationAttribute> getAttributes() {
		return attributes;
	}
	
	public LinkedList<AnnotationNamespace> getNamespaces() {
		return namespaces;
	}
	
	public LinkedList<AnnotationElement> getChildren() {
		return children;
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addAttribute(AnnotationAttribute attribute) {
		attributes.add(attribute);
	}
	
	public void setAttributes(LinkedList<AnnotationAttribute> attributes) {
		this.attributes = attributes;
	}
	
	public void addNamespace(AnnotationNamespace namespace) {
		namespaces.add(namespace);
	}
	
	public void setNamespaces(LinkedList<AnnotationNamespace> namespaces) {
		this.namespaces = namespaces;
	}
	
	public void addChild(AnnotationElement child) {
		children.add(child);
	}
	
	public void setChildren(LinkedList<AnnotationElement> children) {
		this.children = children;
	}
	
	public String toXMLString() {
		String xml = "<";
		if (prefix != null)
			xml = xml + prefix + ":";
		xml += name;
		for (AnnotationAttribute attribute : attributes)
			xml = xml + " " + attribute.toXMLString();
		for (AnnotationNamespace namespace : namespaces) {
			xml = xml + " " + namespace.toXMLString();
		}
		if (children.size() == 0)
			xml += "/";
		xml += ">";
		for (AnnotationElement child : children)
			xml += child.toXMLString();
		if (children.size() > 0) {
			xml += "</";
			if (prefix != null)
				xml = xml + prefix + ":";
			xml = xml + name + ">";
		}
		return xml;
	}
	
}