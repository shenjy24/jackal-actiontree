package com.jonas.util.xml;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@Data
public class ViewNode implements Serializable {

    private String viewName;

    private HashMap<String, String> attributes;

    private List<ViewNode> children;

    private String value;
}
