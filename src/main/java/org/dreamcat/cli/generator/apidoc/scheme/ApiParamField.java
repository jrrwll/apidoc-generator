package org.dreamcat.cli.generator.apidoc.scheme;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.dreamcat.common.reflect.ObjectType;

import java.util.List;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Data
public class ApiParamField {

    private String name;
    private String comment = "";
    private String typeName;
    private Boolean required;
    private List<ApiParamField> fields;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private transient ObjectType type;

    @JsonIgnore
    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
        this.typeName = type.getSimpleName();
    }
}
