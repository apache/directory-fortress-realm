package org.apache.directory.fortress.realm;

/**
 * Created by smckinn on 3/12/15.
 */
public final class GlobalIds
{
    // disallow construction
    private GlobalIds()
    {
    }

    public static final int CONTEXT_SERIALIZATION_FAILED = 201;
    public static final int CONTEXT_DESERIALIZATION_FAILED_IO = 202;
    public static final int CONTEXT_DESERIALIZATION_FAILED_UNSUPPORTED_ENCODING = 203;
    public static final int CONTEXT_DESERIALIZATION_FAILED_CLASS_NOT_FOUND = 204;
    public static final int SESSION_INITIALIZATION_FAILED = 205;
}
