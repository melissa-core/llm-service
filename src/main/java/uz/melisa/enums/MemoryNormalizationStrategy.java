package uz.melisa.enums;

/**
 * How a fact's raw value is canonicalized into normalized_value, and which validation applies.
 */
public enum MemoryNormalizationStrategy {

    /** Value must be one of the definition's allowed structured codes; stored as that canonical code. */
    CODE_SET,

    /** Value must resolve to an authoritative organizationId before it can be stored. */
    ORGANIZATION_REF,

    /** Free text normalized by trimming and lower-casing. */
    LOWERCASE_TEXT
}
