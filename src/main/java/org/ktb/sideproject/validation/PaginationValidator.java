package org.ktb.sideproject.validation;

import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;

public final class PaginationValidator {

    public static final int MAX_PAGE_SIZE = 50;

    private PaginationValidator() {
    }

    public static int requireValidSize(int size) {
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
        return size;
    }
}
