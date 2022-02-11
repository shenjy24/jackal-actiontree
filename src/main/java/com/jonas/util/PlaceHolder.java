package com.jonas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jonas
 * @createTime 2021/9/29 16:13
 */
public class PlaceHolder {

    private static final Logger logger = LoggerFactory.getLogger(PlaceHolder.class);
    private static final PlaceHolder instance = new PlaceHolder();
    private PlaceHolder(){}
    public static PlaceHolder getInstance(){
        return instance;
    }

    private static Pattern pattern = Pattern.compile("\\$\\{[\\u4e00-\\u9fa5\\w:\\-]+}");

    /**
     * 快速编译字符串
     * @param themeName
     * @param sourceStr
     * @return
     */
    public String quickCompile(String themeName, String sourceStr) {
        int lastAppear = 0;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            Matcher matcher = pattern.matcher(sourceStr);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                // 黏贴find之前的内容
                if (start != lastAppear) {
                    stringBuilder.append(sourceStr, lastAppear, start);
                }
                // 替换 find到的内容
                String placeHolder = sourceStr.substring(start + 2, end - 1);
                stringBuilder.append(this.compile(themeName, placeHolder));
                lastAppear = end;
            }

            if (lastAppear != sourceStr.length()) {
                stringBuilder.append(sourceStr.substring(lastAppear));
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            logger.error(String.format("quickCompile failed! %s, %s", themeName, sourceStr), e);
            return sourceStr;
        }
    }

    /**
     * 编译具体的
     * @param themeName
     * @param placeHolder
     * @return
     */
    private String compile(String themeName, String placeHolder) {
        int index = placeHolder.indexOf(":");
        if (index >= 0 && index < placeHolder.length()) {
//            String prefixStr = placeHolder.substring(0, index);
//            String postStr =  placeHolder.substring(index + 1);
//            // 处理视频
//            if (prefixStr.equals("video")) {
//                // 视频的时间
//                int value = VideoService.getInstance().getTimeByKey(themeName, postStr);
//                if (value == 0) {
//                    return "0";
//                } else {
//                    return value + "";
//                }
//            } else if (prefixStr.equals("video_key")) {
//                // 视频的节目单
//                return VideoService.getInstance().getValueByKey(themeName, postStr);
//            } else if (prefixStr.equals("sound")) {
//                // 音频的时间
//                int value = SoundService.getInstance().getTimeByKey(themeName, postStr);
//                if (value == 0) {
//                    return "0";
//                } else {
//                    return value + "";
//                }
//            } else if (prefixStr.equals("sound_key")) {
//                // 音频的节目单
//                return SoundService.getInstance().getValueByKey(themeName, postStr);
//            } else {
                return placeHolder;
//            }
        } else {
            return placeHolder;
        }
    }
}
