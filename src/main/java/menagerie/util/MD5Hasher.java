package menagerie.util;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class MD5Hasher {

    private static MessageDigest md5;

    public static byte[] hash(File file) throws IOException {
        if (getMD5() != null) return getMD5().digest(Files.readAllBytes(file.toPath()));
        return null;
    }

    private static MessageDigest getMD5() {
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance("md5");
                return md5;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return md5;
        }
    }

}
