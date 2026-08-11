# Context: Book Illustration Studio — Gradion Take-Home Assessment

## Đề bài
Web app biến text 1 cuốn sách thành character portraits + chapter illustrations, dùng Gemini API.
Pipeline 5 bước, chạy tuần tự theo user action: Style → Characters → Portraits → Chapters → Illustrations.
Full spec đầy đủ nằm ở file brief gốc (đính kèm riêng) — bám sát §03 (reference pipeline), §04 (functional requirements), §05 (technical requirements).

## Stack đã chốt
- Backend: Spring Boot 4.1.0, Java 21, Maven
- DB: PostgreSQL (Docker), Flyway migration (KHÔNG dùng ddl-auto=update, dùng validate)
- Frontend: React (chưa bắt đầu)
- Storage ảnh + text sách: filesystem local, serve qua API riêng — KHÔNG dùng S3/CDN

## Trạng thái hiện tại (đã xong)
- Docker Compose chạy Postgres, container tên `book-studio-db`
    - user: bookstudio / pass: bookstudio / db: book_illustration_studio
- Flyway migration đã chạy thành công: `src/main/resources/db/migration/V1__init_schema.sql`
- 4 bảng đã tồn tại: users, projects, characters, chapters (+ flyway_schema_history)
- App Spring Boot start thành công, connect DB OK

## Schema hiện tại (đã áp dụng qua Flyway)
- users: id (UUID), email (unique), name, created_at
- projects: id, user_id (FK), title, book_text_file_path, status (enum: CREATED/STYLE_SET/
  CHARACTERS_GENERATED/PORTRAITS_GENERATED/CHAPTERS_GENERATED/DONE), step_state (enum: IDLE/
  RUNNING/FAILED), step_started_at, step_failure_reason, style, book_file_uri,
  last_text_interaction_id, last_image_interaction_id, created_at, updated_at, version (optimistic lock)
- characters: id, project_id (FK), position, name, prompt, portrait_ready (bool), portrait_path
  (max 2/project — enforce ở SERVICE LAYER, không chỉ DB constraint)
- chapters: id, project_id (FK), position, name, prompt, illustration_ready (bool), illustration_path
  (max 1/project — enforce ở SERVICE LAYER)

## Quyết định kỹ thuật quan trọng đã thống nhất (cần đưa vào DECISIONS.md)
1. Tách `status` (tiến độ tổng) và `step_state` (idle/running/failed của bước hiện tại) thành
   2 field riêng — vì 1 enum không diễn tả được "bước 3 xong, bước 4 đang chạy".
2. Postgres thay vì JSON file — đổi lại cần thêm docker-compose.yml (JSON/SQLite thì không cần).
3. Flyway thay vì Hibernate ddl-auto=update — kiểm soát version schema rõ ràng, dùng
   ddl-auto=validate để Hibernate cảnh báo nếu entity lệch với migration.
4. 2 chuỗi Gemini interaction_id ĐỘC LẬP: last_text_interaction_id (book→style→characters→
   chapters) và last_image_interaction_id (setup portraits→portrait1→portrait2→setup
   chapters→illustration) — vì consistency giữa portrait và illustration đến từ việc chuỗi
   ẢNH nối tiếp liên tục (conversation history của model ảnh), KHÔNG phải upload lại file ảnh
   portrait làm input.
5. Optimistic locking (@Version) trên projects — chống double-call khi refresh/2-tab/double-click
   trong lúc step đang RUNNING.
6. Cap 2 characters / 1 chapter phải enforce ở BACKEND, không chỉ UI — vì Gemini có thể trả về
   nhiều hơn, backend chỉ được lưu/xử lý tối đa đúng số lượng cho phép.

## Gemini API — cách gọi (theo notebook đã chạy thử)
- Model text: gemini-3.6-flash | Model ảnh: gemini-3.1-flash-lite-image
- Dùng API "Interactions" (client.interactions.create trong Python SDK) — MỚI, chỉ SDK Python/JS
  wrap sẵn, REST endpoint cần tra cứu kỹ tại https://ai.google.dev/api (chưa xác nhận path chính
  xác — CẦN VERIFY TRƯỚC KHI CODE GeminiClient).
- Cơ chế chaining: mỗi request gửi `previous_interaction_id` trỏ về interaction trước đó thay vì
  gửi lại toàn bộ text sách — request trả về `id` mới để dùng cho bước kế tiếp.
- Characters/Chapters dùng structured output qua `response_format` (JSON schema, có field ít nhất
  `name` và `prompt`).
- Ảnh trả về nằm trong `interaction.steps[]`, lọc step có `type == "model_output"`, tìm
  `content[]` có `type == "image"`, lấy `mime_type` + `data` (base64) → decode, lưu file lên
  filesystem, lưu path vào DB.

## Việc cần làm tiếp theo (theo thứ tự ưu tiên)
1. Verify chính xác REST endpoint của Gemini Interactions API (path, request/response field)
   — chưa confirm 100%, notebook chỉ cho thấy qua Python SDK.
2. Viết GeminiClient interface + implementation (RestClient), dễ mock cho test JUnit.
3. Viết Service layer: xử lý từng step (validate thứ tự, set RUNNING, gọi Gemini async qua
   @Async, update kết quả), retry, stale/stuck detection (so step_started_at với ngưỡng hợp lý
   ~90-120s, KHÔNG dùng 8s như demo).
4. Viết REST Controller (Auth, Project CRUD, Step execution endpoints).
5. Setup React frontend — cover đủ các màn hình trong app-demo.html + 3 chỗ demo KHÔNG có:
   error state thật, chặn double-call ở SERVER (không chỉ 1 tab), timing thật 10-30s+.
6. Viết test: JUnit (BE, mock Gemini) + Vitest/RTL (FE, các state loading/error/empty).
7. Viết DECISIONS.md, TESTING.md, README.md, .env.example, start.sh, test.sh.

## Nguyên tắc làm việc
- Commit nhỏ, thường xuyên, message rõ ràng — nếu phần lớn do AI viết thì note trong commit message.
- Không over-engineer — brief nhấn mạnh "smallest thing that fully works".
- Ghi lại vào DECISIONS.md bất cứ lúc nào AI đề xuất sai/không tối ưu và mình override — brief
  yêu cầu tối thiểu 3 chỗ override AI, đây là phần chấm điểm nặng nhất.