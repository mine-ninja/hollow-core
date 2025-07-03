package net.warcane.lugin.core.util.compress;

import com.github.luben.zstd.Zstd;

public class ZstdUtil {

    public static byte[] compress(byte[] dataToCompress) {
        return Zstd.compress(dataToCompress);
    }

    public static byte[] decompress(byte[] compressedData, int length) {
        return Zstd.decompress(compressedData, length);
    }
}
