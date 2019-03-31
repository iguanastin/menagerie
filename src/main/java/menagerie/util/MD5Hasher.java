package menagerie.util;


import menagerie.gui.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

public abstract class MD5Hasher {

    private static MessageDigest md5;

    public static byte[] hash(File file) throws IOException {
        if (file.exists() && getMD5() != null) return getMD5().digest(Files.readAllBytes(file.toPath()));
        return null;
    }

    private static MessageDigest getMD5() {
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance("md5");
                return md5;
            } catch (NoSuchAlgorithmException e) {
                Main.log.log(Level.SEVERE, "Unable to initialize MD5 digest", e);
                return null;
            }
        } else {
            return md5;
        }
    }

}
