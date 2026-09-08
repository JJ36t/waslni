package com.waslni.driver.core.network

/**
 * Base class for all API-related exceptions thrown by the network layer.
 *
 * The presentation layer pattern-matches on the concrete type to show
 * the right message + retry button.
 */
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * No network connection — show "check your internet" message.
     * Operations are saved locally and will sync later.
     */
    class NoConnection(cause: Throwable? = null) :
        ApiException("لا يوجد اتصال بالإنترنت. تم حفظ العملية محليًا وسيتم مزامنتها لاحقًا.", cause)

    /**
     * Connection timed out — show retry button.
     */
    class Timeout(cause: Throwable? = null) :
        ApiException("انتهى وقت الاتصال. حاول مرة أخرى.", cause)

    /**
     * 401 Unauthorized — session expired. Forces logout.
     */
    class Unauthorized(val code: String = "UNAUTHORIZED") :
        ApiException("انتهت الجلسة. الرجاء تسجيل الدخول.")

    /**
     * 403 Forbidden — account disabled or operation not allowed.
     */
    class Forbidden(val code: String = "FORBIDDEN") :
        ApiException("ليس لديك صلاحية لهذه العملية.")

    /**
     * 404 Not Found.
     */
    class NotFound(val code: String = "RESOURCE_NOT_FOUND") :
        ApiException("العنصر غير موجود.")

    /**
     * 409 Conflict — duplicate, illegal state transition, etc.
     */
    class Conflict(val code: String, message: String) :
        ApiException(message)

    /**
     * 422 Validation error — backend rejected the payload.
     */
    class Validation(val code: String, message: String) :
        ApiException(message)

    /**
     * 429 Rate limited.
     */
    class RateLimited(message: String) :
        ApiException(message)

    /**
     * 5xx server error.
     */
    class ServerError(val status: Int, message: String) :
        ApiException(message)

    /**
     * Any other unexpected HTTP status.
     */
    class HttpError(val status: Int, message: String) :
        ApiException(message)

    /**
     * Response body couldn't be parsed.
     */
    class ParseError(cause: Throwable) :
        ApiException("تعذر قراءة الاستجابة من السيرفر.", cause)

    /**
     * Catch-all for anything not covered above.
     */
    class Unknown(message: String = "حدث خطأ غير متوقع.", cause: Throwable? = null) :
        ApiException(message, cause)
}
