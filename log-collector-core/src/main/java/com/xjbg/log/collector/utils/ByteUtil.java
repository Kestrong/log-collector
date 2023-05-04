package com.xjbg.log.collector.utils;

import java.util.List;

/**
 * @author kesc
 * @since 2023-04-24 10:03
 */
public class ByteUtil {

    public static byte[] mergeBytes(List<byte[]> values) {
        int lengthByte = 0;
        for (byte[] value : values) {
            lengthByte += value.length;
        }
        byte[] allBytes = new byte[lengthByte];
        int countLength = 0;
        for (byte[] b : values) {
            System.arraycopy(b, 0, allBytes, countLength, b.length);
            countLength += b.length;
        }
        return allBytes;
    }

}
