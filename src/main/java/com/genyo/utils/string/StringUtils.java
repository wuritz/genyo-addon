package com.genyo.utils.string;

import meteordevelopment.meteorclient.utils.Utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringUtils {

    public static String titleToName(String title) {
        title = Utils.titleToName(title);
        return Arrays.stream(title.split(" ")).map(org.apache.commons.lang3.StringUtils::uncapitalize).collect(Collectors.joining("-"));
    }

}
