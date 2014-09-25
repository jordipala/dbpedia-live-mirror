package org.dbpedia.extraction.live.mirror.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Description
 *
 * @author Dimitris Kontokostas
 * @since 9/24/14 1:16 PM
 */
public final class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    public static List<String> getTriplesFromFile(String filename) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"))) {

            String line;
            while ((line = in.readLine()) != null) {
                String triple = line.trim();

                // Ends with is a hack for not correctly decompressed changesets
                if (!triple.isEmpty() && !triple.startsWith("#") && triple.endsWith(" .")) {
                    lines.add(triple);
                }

            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File " + filename + " not fount!", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UnsupportedEncodingException: ", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException in file " + filename, e);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Cannot read file " + filename, e);
        }

        return lines;

    }

    public static boolean deleteFile(String filename) {
        try {
            return new File(filename).delete();
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateStringFromList(Collection<String> strList, String sep) {

        StringBuilder finalString = new StringBuilder();


        for (String str : strList) {
            finalString.append(str);
            finalString.append(sep);
        }

        return finalString.toString();
    }

    /**
     * Decompresses the passed GZip file, and returns the filename of the decompressed file
     *
     * @param filename             The filename of compressed file
     * @param deleteCompressedFile Whether to delete the original compressed file upon completion
     * @return The filename of the output file, or empty string if a problem occurs
     */
    public static String decompressGZipFile(String filename, boolean deleteCompressedFile) {

        String outFilename;
        //The output filename is the same as input filename without last .gz
        int lastDotPosition = filename.lastIndexOf(".");
        outFilename = filename.substring(0, lastDotPosition);

        try (
                FileInputStream fis = new FileInputStream(filename);
                //GzipCompressorInputStream(
                GZIPInputStream gis = new GZIPInputStream(fis);
                InputStreamReader isr = new InputStreamReader(gis, "UTF8");
                //BufferedReader in = new BufferedReader(isr);
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outFilename), "UTF8")
        ) {
            int character;
            while ((character = isr.read()) != -1) {
                out.write(character);

            }

            logger.debug("File : " + filename + " decompressed successfully to " + outFilename);
        } catch (EOFException e) {
            // probably Wrong compression, out stream will close and existing contents will remain
            // but might leave incomplete triples
            logger.error("EOFException in compressed file: " + filename + " - Trying to recover");
        } catch (IOException ioe) {
            logger.warn("File " + filename + " cannot be decompressed due to " + ioe.getMessage(), ioe);
            outFilename = "";
        } finally {
            if (deleteCompressedFile)
                (new File(filename)).delete();
        }
        return outFilename;
    }

    /**
     * Downloads the file with passed URL to the passed folder
     * http://stackoverflow.com/a/921400/318221
     *
     * @param fileURL    URL of the file that should be downloaded
     * @param folderPath The path to which this file should be saved
     * @return The local full path of the downloaded file, empty string is returned if a problem occurs
     */
    public static String downloadFile(String fileURL, String folderPath) {

        //Extract filename only without full path
        int lastSlashPos = fileURL.lastIndexOf("/");
        if (lastSlashPos < 0)
            return "";

        String fullFileName = folderPath + fileURL.substring(lastSlashPos + 1);

        //Create parent folder if it does not already exist
        File file = new File(fullFileName);
        file.getParentFile().mkdirs();

        URL url;

        try {
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            return "";
        }

        try (
             ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(file);
        ) {

            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e) {
            return "";
        }

        return fullFileName;
    }
}
