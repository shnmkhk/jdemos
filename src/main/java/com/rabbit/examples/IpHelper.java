/**
 * Copyright (c) 2013, F5 Networks, Inc. All rights reserved.
 * No part of this software may be reproduced or transmitted in any
 * form or by any means, electronic or mechanical, for any purpose,
 * without express written permission of F5 Networks, Inc.
 */

package com.rabbit.examples;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utilities to deal with IP addresses. There is some reflection in here to use some internal Java
 * methods. This avoids the un-supressable warnings generated during compilation. There are also
 * fallbacks in case these aren't available. User: harrison Date: 9/5/13
 */
public class IpHelper {
    private static final Logger LOGGER = Logger.getLogger(IpHelper.class.getSimpleName());

    // CIDR notation.
    public static String PREFIX_LENGTH_SEP = "/";

    private IpHelper() {
    }

    private static Method getIPv6Bytes;
    private static Method getIPv4Bytes;
    static {
        setProprietaryMethods();
    }

    private static Pattern IPV$_ADDRESS_PATTERN = Pattern
            .compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private final static ConcurrentHashMap<String, String> CANONICAL_ADDRESS_MAP =
            new ConcurrentHashMap<>();

    /**
     * Sets up the reflected methods for parsing the ips. Reflection is used here because these are
     * internal Sun/Oracle interfaces and the javac compiler sprays warnings that are not
     * suppressable. In the event that we upgrade or these methods are not available there are
     * fallbacks in this class attempt to use the public API.
     */
    private static void setProprietaryMethods() {
        try {
            Class<?> c = Class.forName("sun.net.util.IPAddressUtil");
            getIPv6Bytes = c.getDeclaredMethod("textToNumericFormatV6",
                    String.class);
        } catch (Exception t) {
            getIPv6Bytes = null;
            LOGGER.severe("Couldn't set ipv6 parser: " + t.getMessage());
        }

        try {
            Class<?> c = Class.forName("sun.net.util.IPAddressUtil");
            getIPv4Bytes = c.getDeclaredMethod("textToNumericFormatV4",
                    String.class);
        } catch (Exception t) {
            getIPv4Bytes = null;
            LOGGER.severe("Couldn't set ipv4 parser: " + t.getMessage());
        }
    }

    /**
     * @param hostname hostname to check.
     * @return true if the hostname is a literal IP (v4 or v6)
     */
    public static boolean checkIsIpAddress(String hostname) {
        return checkIsIpV4Address(hostname) || checkIsIpV6Address(hostname);
    }

    /**
     * @param src a String representing an IPv6 address in textual format
     * @return a boolean indicating whether src is an IPv6 literal address
     */
    public static boolean checkIsIpV6Address(String src) {
        // Insist that the address contains at least a colon before moving on.
        // We are trying to prevent a domain name lookup here by doing some
        // pre-checks.
        if (null == src || 2 > src.length() || !src.contains(":")) {
            return false;
        }

        // Trim white space and brackets.
        src = src.trim();
        if (src.startsWith("[") && src.endsWith("]")) {
            src = src.substring(1, src.length() - 1);
        }

        if (null != getIPv6Bytes) {
            try {
                return null != getIPv6Bytes.invoke(null, src);
            } catch (Exception t) {
                // Should not throw here unless some reflection failed.
                throw new RuntimeException(t);
            }
        }

        return false;
    }

    /**
     * From http://www.mkyong.com/regular-expressions/how-to-validate-ip-address-with-regular-
     * expression/ Checks for decimal dot notation ipv4 addresses. There is no built-in JRE class
     * for this, that does NOT do a DNS resolution. Google Guava library would be the next best
     * thing to use
     *
     * @param src address to check.
     * @return true if literal ipv4 address.
     */
    public static boolean checkIsIpV4Address(String src) {
        if (src == null || src.isEmpty()) {
            return false;
        }

        src = src.trim();
        if ((src.length() < 6) & (src.length() > 15)) {
            return false;
        }

        if (null != getIPv4Bytes) {
            try {
                return null != getIPv4Bytes.invoke(null, src);
            } catch (Exception t) {
                // Should not throw here unless some reflection failed.
                log(t.getMessage(), src);
                return false;
            }
        } else {
            try {
                Matcher matcher = IPV$_ADDRESS_PATTERN.matcher(src);
                return matcher.matches();
            } catch (PatternSyntaxException ex) {
                return false;
            }
        }
    }

    /**
     * Canonicalizes an IPv4 or IPv6 address from a string. Primarily used to allow string
     * comparisons of addresses. IP addresses will be fully expanded.
     *
     * @param address address to canonicalize.
     * @return if literal IP then the canonicalized form of the IP, else the input
     */
    public static String makeCanonicalAddress(final String address) {

        if (null == address) {
            return null;
        }

        if ("localhost".equals(address)) {
            return address;
        }

        if ("127.0.0.1".equals(address)) {
            return address;
        }

        String previousAddress = CANONICAL_ADDRESS_MAP.get(address);
        if (previousAddress != null) {
            return previousAddress;
        }

        // IPv4, no domain resolution!
        if (null != getIPv4Bytes) {
            // IPv4
            try {
                byte[] src = (byte[]) getIPv4Bytes.invoke(null, address);
                if (null != src) {
                    String canonicalAddress = InetAddress.getByAddress(src).getHostAddress();
                    CANONICAL_ADDRESS_MAP.put(address, canonicalAddress);
                    return canonicalAddress;
                }
            } catch (Exception t) {
                // Should not throw here unless some reflection failed.
                log(t.getMessage(), address);
            }
        }

        // IPv6 - Strip leading and trailing brackets if necessary.
        String trimmedAddress = address.trim();
        if (trimmedAddress.startsWith("[") && trimmedAddress.endsWith("]")) {
            trimmedAddress = trimmedAddress.substring(1, trimmedAddress.length() - 1);
        }

        // IPv6, no domain resolution!
        if (null != getIPv6Bytes) {
            try {
                byte[] src = (byte[]) getIPv6Bytes.invoke(null, trimmedAddress);
                if (null != src) {
                    String canonicalAddress = InetAddress.getByAddress(src).getHostAddress();
                    CANONICAL_ADDRESS_MAP.put(address, canonicalAddress);
                    return canonicalAddress;
                }
            } catch (Exception t) {
                // Should not throw here unless some reflection failed.
                log(t.getMessage(), trimmedAddress);
            }
        }

        /*
         * Final catch-all. By now, we failed to resolute either an IPv4 or IPv6 address. We try to
         * avoid a domain name lookup, so just return what we've got.
         */
        return trimmedAddress;
    }

    /**
     * Compares two hosts/addresses. If they are literal IPs then they are canonicalized first.
     *
     * @return true if the addresses are the same.
     */
    public static boolean checkIsSameAddress(String hostOne, String hostTwo) {
        if (null == hostOne || null == hostTwo) {
            return null == hostOne && null == hostTwo;
        }

        if (hostOne.equals(hostTwo)) {
            return true;
        }

        String addressOne = makeCanonicalAddress(hostOne);
        if (addressOne.equals(hostTwo)) {
            return true;
        }

        String addressTwo = makeCanonicalAddress(hostTwo);
        return addressOne.equals(addressTwo);
    }

    private static void log(String message, String input) {
        LOGGER.severe(message + " for address " + input);
    }

    /**
     * Truncates an IP Address if there is a routing prefix using CIDR notation ('/' at the end). No
     * checking of address is done, simply string search and truncate.
     *
     * @param address IP address,
     * @return truncated IP address.
     */
    public static String trimCIDR(String address) {
        // Trim the slash if necessary, otherwise mask breaks comparison.
        int slashIndex = address.indexOf(PREFIX_LENGTH_SEP);
        if (-1 != slashIndex) {
            return address.substring(0, slashIndex);
        }
        return address;
    }

    /**
     * Finds the routing prefix of an IP Address given in CIDR notation (value after the '/'). No
     * checking of address is done, simply string search.
     *
     * @param address IP address in CIDR notation,
     * @return value of routing prefix.
     */
    public static String trimPrefix(String address) throws IllegalArgumentException {
        // Trim the slash if necessary, otherwise mask breaks comparison.
        int slashIndex = address.indexOf(PREFIX_LENGTH_SEP);
        if (-1 == slashIndex) {
            throw new IllegalArgumentException("Invalid IP Address CIDR notation: '/' not found.");
        }

        return address.substring(slashIndex + 1);
    }

    public static String getNetmaskAddress(String cidrAddress) throws IllegalArgumentException,
            UnknownHostException {
        String address = trimCIDR(cidrAddress);
        int prefixLength = Integer.parseInt(trimPrefix(cidrAddress));

        ByteBuffer bb;
        if (checkIsIpV4Address(address)) {
            bb = ByteBuffer.allocate(4);
        } else if (checkIsIpV6Address(address)) {
            bb = ByteBuffer.allocate(16);

        } else {
            throw new IllegalArgumentException("Invalid IP Address, not ipv4 or ipv6.");
        }

        for (int i = 0; i < prefixLength;) {
            byte b = 0x00;
            for (int j = 0; j < 8 && i < prefixLength; j++, i++) {
                b = (byte) ((b >> 1) | 0x80);
            }
            bb.put(b);
        }

        return InetAddress.getByAddress(bb.array()).getHostAddress();

    }

    public static byte[] getAddressBytes(String ipAddress) throws Exception {
        byte[] bytes = null;

        IpVersion ipVersion = getIpVersion(ipAddress);

        switch (ipVersion) {
        case ipV4:
            bytes = (byte[]) getIPv4Bytes.invoke(null, ipAddress);
            break;
        case ipV6:
            bytes = (byte[]) getIPv6Bytes.invoke(null, ipAddress);
            break;
        }

        return bytes;
    }

    public static IpVersion getIpVersion(String ipAddress) throws Exception {

        if (null == getIPv4Bytes.invoke(null, ipAddress)) {
            if (null == getIPv6Bytes.invoke(null, ipAddress)) {
                throw new IllegalArgumentException(ipAddress);
            } else {
                return IpVersion.ipV6;
            }
        } else {
            return IpVersion.ipV4;
        }
    }

    public static String formatCIDR(String ipAddress, String subnetMask)
            throws UnknownHostException {
        int subnetPrefix = 0;
        InetAddress subnetAddr;
        subnetAddr = InetAddress.getByName(subnetMask);
        subnetPrefix = BitSet.valueOf(subnetAddr.getAddress()).cardinality();

        return ipAddress + IpHelper.PREFIX_LENGTH_SEP + subnetPrefix;
    }

    public enum IpVersion {
        ipV4,
        ipV6
    }
    
    public static void main(String[] args) {
    	final String[] inputStrs = new String[] {"11111111", "10.218.25.25", "Hello, World"};
    	for (int ix = 0; ix < inputStrs.length; ix++) { 
    		System.out.println(String.format("Approach.1: Input: [%s], Result: [%s]", inputStrs[ix], checkIsIpV4Address(inputStrs[ix])));
    		System.out.println(String.format("Approach.2: Input: [%s], Result: [%s]", inputStrs[ix], IPV$_ADDRESS_PATTERN.matcher(inputStrs[ix]).matches()));
    		System.out.println();
    	}
	}
}
