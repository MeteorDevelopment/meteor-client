/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.files;

import meteordevelopment.meteorclient.MeteorClient;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class StreamUtils {
    private StreamUtils() {
    }

    public static void copy(File from, File to) {
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            in.transferTo(out);
        } catch (IOException e) {
            MeteorClient.LOG.error("Error copying from file '%s' to file '%s'.".formatted(from.getName(), to.getName()), e);
        }
    }

    public static void copy(InputStream in, File to) {
        try (OutputStream out = new FileOutputStream(to)) {
            in.transferTo(out);
        } catch (IOException e) {
            MeteorClient.LOG.error("Error writing to file '%s'.".formatted(to.getName()));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
