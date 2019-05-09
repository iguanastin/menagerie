/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

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
        if (file != null && file.exists() && getDigest() != null) return getDigest().digest(Files.readAllBytes(file.toPath()));
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
