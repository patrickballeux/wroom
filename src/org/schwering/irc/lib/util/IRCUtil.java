/**
 * IRClib - A Java Internet Relay Chat library
 * Copyright (C) 2006-2015 Christoph Schwering <schwering@gmail.com>
 * and/or other contributors as indicated by the @author tags.
 *
 * This library and the accompanying materials are made available under the
 * terms of the
 *  - GNU Lesser General Public License,
 *  - Apache License, Version 2.0 and
 *  - Eclipse Public License v1.0.
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY.
 */
package org.schwering.irc.lib.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.impl.DefaultIRCConnection;

/**
 * Contains some utilities like numeric error and reply numbers.
 * <p>
 * The most description of the numeric errors and numeric replies are copied
 * from RFC1459.
 * @author Christoph Schwering &lt;schwering@gmail.com&gt;
 * @author Normton &lt;normton@latinmail.com&gt;
 * @see DefaultIRCConnection
 * @see IRCEventListener#onError(int, String)
 * @see IRCEventListener#onReply(int, String, String)
 */
public class IRCUtil implements IRCConstants {


    /**
     * This is an empty constructor, it does nothing. Nobody may instantiate this
     * class.
     */
    private IRCUtil() {
        // nothing
    }


    /**
     * According to RFC2812 the channel's name may and must start with one of the
     * following characters.
     * <ul>
     * <li>! == 33 (ASCII)</li>
     * <li># == 35</li>
     * <li>&amp; == 38</li>
     * <li>+ == 43</li>
     * </ul>.
     * @param str The name to check if it's a channel.
     * @return <code>true</code> if the argument starts with one of the characters
     *         mentioned above.
     */
    public static boolean isChan(String str) {
        int c;
        return (str.length() >= 2)
        && ((c = str.charAt(0)) == '#' || c == '&' || c == '!' || c == '+');
    }


    /**
     * Parses a <code>String</code> to an <code>int</code> via
     * <code>Integer.parseInt</code> but avoids the
     * <code>NumberFormatException</code>.
     * @param str The <code>String</code> to parse.
     * @return The parsed new <code>int</code>. <code>-1</code> if
     *         <code>NumberFormatException</code> was thrown.
     */
    public static int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException exc) {
            return -1;
        }
    }


    /**
     * Erases the mIRC colorcodes from a String.
     * The documentation of the evil color codes is available on
     * <a href="http://www.mirc.co.uk/help/color.txt"
     * target="_blank">http://www.mirc.co.uk/help/color.txt</a>.
     * <p>
     * Calling this method is equivalent to
     * {@code IRCUtil.stripColors(new StringBuilder(str), false).toString()}.
     *
     * @param str The line which should be parsed.
     * @return A line cleaned from any mIRC colorcodes.
     * @see #stripColorsAndCTCPDelimiters(StringBuilder)
     */
    public static String stripColors(String str) {
        return stripColors(new StringBuilder(str), false).toString();
    }



    /**
     * Erases the mIRC colorcodes from a StringBuilder.
     * The documentation of the evil color codes is available on
     * <a href="http://www.mirc.co.uk/help/color.txt"
     * target="_blank">http://www.mirc.co.uk/help/color.txt</a>.
     * <p>
     * Calling this method is equivalent to
     * {@code IRCUtil.stripColors(buf, false)}.
     *
     * @param buf The line which should be parsed.
     * @return A line cleaned from any mIRC colorcodes.
     * @see #stripColorsAndCTCPDelimiters(StringBuilder)
     */
    public static StringBuilder stripColors(StringBuilder buf) {
        return stripColors(buf, false);
    }

    /**
     * Erases the mIRC colorcodes and CTCP delimiters from a StringBuilder.
     * The documentation of the evil color codes is available on
     * <a href="http://www.mirc.co.uk/help/color.txt"
     * target="_blank">http://www.mirc.co.uk/help/color.txt</a>.
     * <p>
     * Calling this method is equivalent to
     * {@code IRCUtil.stripColors(buf, true)}.
     *
     * @param buf The line which should be parsed.
     * @return A line as <code>StringBuilder</code> object which is cleaned from
     *         any mIRC colorcodes.
     */
    public static StringBuilder stripColorsAndCTCPDelimiters(StringBuilder buf) {
        return stripColors(buf, true);
    }


    /**
     * Erases the mIRC colorcodes and optionally also CTCP delimiters from a String.
     * The documentation of the evil color codes is available on
     * <a href="http://www.mirc.co.uk/help/color.txt"
     * target="_blank">http://www.mirc.co.uk/help/color.txt</a>.
     *
     * @param buf The line which should be parsed.
     * @param removeCTCP If <code>false</code>, <code>CTCP_DELIMITER</code>s
     *                   are left untouched in the string.
     * @return A line as <code>StringBuilder</code> object which is cleaned from
     *         any mIRC colorcodes.
     */
    private static StringBuilder stripColors(StringBuilder buf,
            boolean removeCTCP) {
        int len = buf.length();
        char c;
        for (int i = 0, j = 0; i < len; i++, j = i) {
            c = buf.charAt(i);
            try {
                // COLORS Beginning
                // (format: <colorIndicator><int>[<int>][[,<int>[<int>]]
                if (c == COLOR_INDICATOR) {
                    c = buf.charAt(++j);
                    if ('0' <= c && c <= '9') { // first int
                        c = buf.charAt(++j);
                        if ('0' <= c && c <= '9')
                            c = buf.charAt(++j); // second int
                    }
                    if (c == ',')
                        c = buf.charAt(++j); // comma
                    if ('0' <= c && c <= '9') { // first int
                        c = buf.charAt(++j);
                        if ('0' <= c && c <= '9')
                            c = buf.charAt(++j); // second int
                    }
                    // CTCP / BOLD / UNDERLINE / COLOR END
                    // (format: <ctcpDelimiter> / <boldIndicator> etc.)
                } else if ((removeCTCP && c == CTCPCommand.QUOTE_CHAR)
                        || c == BOLD_INDICATOR
                        || c == UNDERLINE_INDICATOR
                        || c == COLOR_END_INDICATOR
                        || c == COLOR_REVERSE_INDICATOR) {
                    j++;
                }
            } catch(StringIndexOutOfBoundsException exc) {
                // we got the end of the string with a call to charAt(++iIndexEnd)
                // nothing
            }

            if (j > i) {
                buf = buf.delete(i, j); // remove the cars
                len -= (j - i);
                i--;
            }
        }
        return buf;
    }


    /**
     * Splits a string into substrings.
     * @param str The string which is to split.
     * @param delim The delimiter character, for example a space <code>' '</code>.
     * @param trailing The ending which is added as a substring though it wasn't
     *                 in the <code>str</code>. This parameter is just for the
     *                 <code>IRCParser</code> class which uses this method to
     *                 split the <code>middle</code> part into the parameters.
     *                 But as last parameter always the <code>trailing</code> is
     *                 added. This is done here because it's the fastest way to
     *                 do it here.
     *                 If the <code>end</code> is <code>null</code> or
     *                 <code>""</code>, nothing is appended.
     * @return An array with all substrings.
     * @see #split(String, int)
     */
    public static String[] split(String str, int delim, String trailing) {
        List<String> items = new ArrayList<String>(15);
        int last = 0;
        int index = 0;
        int len = str.length();
        while (index < len) {
            if (str.charAt(index) == delim) {
                items.add(str.substring(last, index));
                last = index + 1;
            }
            index++;
        }
        if (last != len)
            items.add(str.substring(last));
        if (trailing != null && trailing.length() != 0)
            items.add(trailing);
        String[] result = items.toArray(new String[0]);
        return result;
    }


    /**
     * Splits a string into substrings. This method is totally equal to
     * <code>split(str, delim, null)</code>.
     * @param str The string which is to split.
     * @param delim The delimiter character, for example a space <code>' '</code>.
     * @return An array with all substrings.
     * @see #split(String, int, String)
     */
    public static String[] split(String str, int delim) {
        return split(str, delim, null);
    }

    public static int[] toArray(Collection<Integer> list) {
        if (list == null || list.isEmpty()) {
            return new int[0];
        }
        int[] result = new int[list.size()];
        int i = 0;
        for (int value : list) {
            result[i++] = value;
        }
        return result;
    }
}
