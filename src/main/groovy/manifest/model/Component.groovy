package manifest.model

class Component {
    ComponentType component_type
    String name
    Boolean include

    String toString() {
        return "Component{" +
                "component_type=" + component_type +
                ", name='" + name + '\'' +
                ", include=" + include +
                '}'
    }
}

