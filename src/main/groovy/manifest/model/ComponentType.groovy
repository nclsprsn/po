package manifest.model

class ComponentType {
    String packageName
    String name

    ComponentType() {
    }

    String toString() {
        return "ComponentType{" +
                "packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                '}'
    }
}
