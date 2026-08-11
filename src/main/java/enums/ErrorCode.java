package enums;

public enum ErrorCode {
    VALIDATION_ERROR,
    USER_NOT_FOUND,
    PROJECT_NOT_FOUND,
    INVALID_STEP_ORDER,       // chạy step khi step trước chưa xong
    STEP_ALREADY_RUNNING,     // chống double-call
    STEP_NOT_FAILED,          // gọi retry nhưng step không ở trạng thái FAILED
    CHARACTER_CAP_EXCEEDED,   // vượt quá 2 nhân vật
    CHAPTER_CAP_EXCEEDED,     // vượt quá 1 chương
    GEMINI_CALL_FAILED,       // lỗi khi gọi Gemini API
    INTERNAL_ERROR
}