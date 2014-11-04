package org.bubblecloud.vr4java.rpc;

/**
 * Created by tlaukkan on 9/20/14.
 */
public interface RpcConstants {

    public static final int VERSION = 1;

    public static final int VALIDITY_TIME_MILLIS = 60000;

    public static final int VERSION_INDEX = 0;
    public static final int VERSION_LENGTH = 1;
    public static final int TYPE_INDEX = VERSION_INDEX + VERSION_LENGTH;
    public static final int TYPE_LENGTH = 1;
    public static final int ID_INDEX = TYPE_INDEX + TYPE_LENGTH;
    public static final int ID_LENGTH = 4;
    public static final int TIMESTAMP_INDEX = ID_INDEX + ID_LENGTH;
    public static final int TIMESTAMP_LENGTH = 8;
    public static final int SOURCE_INDEX = TIMESTAMP_INDEX + TIMESTAMP_LENGTH;
    public static final int SOURCE_LENGTH = 32;
    public static final int TARGET_INDEX = SOURCE_INDEX + SOURCE_LENGTH;
    public static final int TARGET_LENGTH = 32;
    public static final int SIGNATURE_INDEX = TARGET_INDEX + TARGET_LENGTH;
    public static final int SIGNATURE_LENGTH = 128;

    public static final int SEAL_LENGTH = TIMESTAMP_LENGTH + SOURCE_LENGTH + TARGET_LENGTH + SIGNATURE_LENGTH;

    public static final int HEADER_LENGTH = VERSION_LENGTH + TYPE_LENGTH + ID_LENGTH + SEAL_LENGTH;

    public static final int TYPE_HANDSHAKE_REQUEST = 0;
    public static final int TYPE_HANDSHAKE_RESPONSE = 1;
    public static final int TYPE_REQUEST = 2;
    public static final int TYPE_RESPONSE = 3;
    public static final int TYPE_EVENT = 4;

}
