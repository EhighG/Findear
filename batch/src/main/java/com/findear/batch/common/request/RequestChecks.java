package com.findear.batch.common.request;

import com.findear.batch.common.exception.BadRequestException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** API 입력 검사. 틀리면 {@link BadRequestException}(400)이다. 서비스가 입력을 쓰기 전에 부른다 */
public final class RequestChecks {

    private RequestChecks() {
    }

    /** page는 1부터, size는 1 이상 */
    public static void pageAndSize(int page, int size) {
        if (page < 1) {
            throw new BadRequestException("page는 1 이상이어야 합니다.");
        }
        if (size < 1) {
            throw new BadRequestException("size는 1 이상이어야 합니다.");
        }
    }

    /** 비어 있지 않은 문자열이어야 한다 */
    public static void required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(name + "은(는) 필수입니다.");
        }
    }

    /** yyyy-MM-dd 날짜여야 한다 */
    public static LocalDate isoDate(String value, String name) {
        required(value, name);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(name + "은(는) yyyy-MM-dd 형식이어야 합니다.");
        }
    }

    /** 정수(Long)여야 한다 */
    public static long longValue(String value, String name) {
        required(value, name);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException(name + "은(는) 숫자여야 합니다.");
        }
    }
}
