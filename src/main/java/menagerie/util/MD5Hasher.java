package menagerie.util;


import menagerie.gui.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

/**
 * Utility class that contains functions for hashing files with the MD5 algorithm.
 */
public abstract class MD5Hasher {

    private static MessageDigest digest;

    /**
     * Reads a file and calculates an MD5 hash representing it.
     *
     * @param file File to hash.
     * @return The MD5 hash of the file. Null if the file does not exist or the MD5 digest could not be instantiated.
     * @throws IOException If error occurred when reading file.
     */
    public static byte[] hash(File file) throws IOException {
        if (file.exists() && getDigest() != null) return getDigest().digest(Files.readAllBytes(file.toPath()));
        return null;
    }

    private static MessageDigest getDigest() {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("md5");
                return digest;
            } catch (NoSuchAlgorithmException e) {
                Main.log.log(Level.SEVERE, "Unable to initialize MD5 digest", e);
                return null;
            }
        } else {
            return digest;
        }
    }

}
