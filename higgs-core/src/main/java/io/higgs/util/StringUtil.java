package io.higgs.util;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * Related links
 * http://www.joelonsoftware.com/articles/Unicode.html
 * http://www.exampledepot.com/egs/java.nio.charset/ConvertChar.html
 *
 * @author Courtney Robinson <courtney@crlog.rubbish> @ 06/02/12
 */
public class StringUtil {
    private String encoding = "UTF-8";
    private Charset charset = Charset.forName(encoding);
    private CharsetDecoder decoder = charset.newDecoder();
    private CharsetEncoder encoder = charset.newEncoder();

    /**
     * Encode a string to a byte array using
     *
     * @param value The string to encode
     * @return A byte array representation of the string, encoded using the
     *         current charset OR null if an exception is raised;
     */
    public byte[] getBytes(String value) {
        return value.getBytes(charset);
    }

    /**
     * Convert a byte array to a string using the current charset
     *
     * @param data the byte array to convert to a string
     * @return A string representation of the input byte array OR null on error
     */
    public String getString(byte[] data) {
        try {
            return decoder.decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    public void setCharset(String c) {
        charset = Charset.forName(c);
    }

    public void setCharset(Charset c) {
        charset = c;
    }
}