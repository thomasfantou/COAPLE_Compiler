package co.uk.brookes.symboltable;

/**
 * Author: Fantou Thomas
 * Date: 6/21/13
 */
public class Struct {
    public static final int // structure kinds
            none_       = 0,
            integer_    = 1,
            real_       = 2,
            bool_       = 3,
            char_       = 4,
            string_     = 5,
            caste_      = 6,
            list_       = 7,
            enum_       = 8,
            record_     = 9;

    public static final String[] values = {
            "none", "integer", "real", "bool", "char", "string", "caste", "list", "enum", "record"};

    public int     kind;		    // integer, char, declared type...
    public Struct  listType;        // List: list type
    public int     nFields;         // Caste/List/Record/Enum: number of fields
    public Obj     fields;          // Caste/List/Record/Enum: fields
    public String  name;            // for declared type/caste

    public Struct(int kind) {
        this.kind = kind;
    }
}
