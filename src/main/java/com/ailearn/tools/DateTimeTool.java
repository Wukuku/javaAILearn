package com.ailearn.tools;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

/**
 * 日期时间工具
 *
 * 面试亮点：解决 LLM 不知道当前时间的问题（知识截止日期限制）
 * 模型需要知道当前日期时，会自动调用此工具获取准确时间
 */
@Component
@Description("获取当前日期时间，或计算两个日期之间相差的天数")
public class DateTimeTool implements Function<DateTimeTool.Request, DateTimeTool.Response> {

    public record Request(
            @JsonPropertyDescription("操作类型：now（获取当前时间）或 diff（计算日期差），默认 now")
            String action,

            @JsonPropertyDescription("时区，如 Asia/Shanghai（北京时间）、America/New_York，默认上海时区")
            String timezone,

            @JsonPropertyDescription("开始日期，格式 yyyy-MM-dd，action=diff 时必填")
            String startDate,

            @JsonPropertyDescription("结束日期，格式 yyyy-MM-dd，action=diff 时必填")
            String endDate
    ) {}

    public record Response(String result, String description) {}

    @Override
    public Response apply(Request request) {
        String action = request.action() != null ? request.action() : "now";
        return switch (action) {
            case "now" -> {
                ZoneId zoneId = parseZoneId(request.timezone());
                String time = LocalDateTime.now(zoneId)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                yield new Response(time, "当前时间（时区：" + zoneId + "）");
            }
            case "diff" -> {
                LocalDate start = LocalDate.parse(request.startDate());
                LocalDate end = LocalDate.parse(request.endDate());
                long days = ChronoUnit.DAYS.between(start, end);
                yield new Response(
                        String.valueOf(Math.abs(days)),
                        request.startDate() + " 到 " + request.endDate() + " 相差 " + Math.abs(days) + " 天"
                );
            }
            default -> new Response("error", "不支持的操作: " + action + "，请使用 now 或 diff");
        };
    }

    private ZoneId parseZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) return ZoneId.of("Asia/Shanghai");
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Asia/Shanghai");
        }
    }
}
