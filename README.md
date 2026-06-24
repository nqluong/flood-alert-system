# Flood Alert System — Hướng dẫn cài đặt & triển khai

Hệ thống giám sát, phân tích và cảnh báo ngập lụt theo thời gian thực dựa trên dữ liệu cảm biến IoT và báo cáo từ cộng đồng. Backend được đóng gói thành các Docker image dựng sẵn và chạy qua `docker-compose`.

> Tài liệu này hướng dẫn triển khai **toàn bộ backend + hạ tầng** bằng `docker-compose.yml`. Frontend (`flood-alert-frontend`) và Simulator (`sensors-simulator`) có hướng dẫn riêng.

---

## 1. Yêu cầu hệ thống

| Thành phần | Phiên bản tối thiểu | Ghi chú |
|------------|---------------------|---------|
| Docker Engine | 24.x trở lên | Bắt buộc |
| Docker Compose | v2 (`docker compose`) | Tích hợp sẵn trong Docker Desktop |
| RAM | ≥ 12 GB khả dụng | Tổng `memory limit` các container khoảng 12–13 GB |
| CPU | ≥ 4 core | |
| Ổ cứng | ≥ 10 GB trống | Cho image + volume dữ liệu |

> **Lưu ý tài nguyên:** `floodprocessor-service` (3 GB) và `floodcore-service` (2 GB) là 2 service nặng nhất. Nếu máy yếu, hãy giảm `deploy.resources.limits.memory` trong `docker-compose.yml`.

---

## 2. Kiến trúc & danh sách dịch vụ

### Microservices (image dựng sẵn từ Docker Hub `nqluong14/*:v2.0`)

| Service | Port nội bộ | Vai trò |
|---------|-------------|---------|
| `api-gateway` | 8080 | Cổng định tuyến API tập trung (entry point qua Nginx) |
| `auth-service` | 8081 | Xác thực & quản lý người dùng |
| `ingestion-service` | 8082 | Nhận data sensor từ MQTT → validate → đẩy vào Kafka |
| `floodcore-service` | 8083 | API nghiệp vụ: Map, Sensor, Report |
| `floodprocessor-service` | 8084 | Xử lý logic: xác nhận điểm ngập, chấm điểm tin cậy |
| `notification-service` | 8085 | Thông báo real-time (WebSocket + FCM) |

### Hạ tầng (image công khai)

| Service | Image | Port (host) | Vai trò |
|---------|-------|-------------|---------|
| `postgres` | `postgis/postgis:15-3.4-alpine` | 5432 | Database (có PostGIS) |
| `redis` | `redis:7-alpine` | 6379 | Caching |
| `kafka` | `apache/kafka:latest` | 9092 | Message broker nội bộ (KRaft mode) |
| `mqtt-broker` | `eclipse-mosquitto:2.0` | 1883, 9001 | Nhận telemetry IoT |
| `nginx` | `nginx:1.27-alpine` | 80, 443 | Reverse proxy / TLS |
| `certbot` | `certbot/certbot:latest` | — | Tự động gia hạn SSL (Let's Encrypt) |

> Chỉ `nginx`, `postgres`, `redis`, `kafka`, `mqtt-broker` mở port ra host. Các microservice giao tiếp nội bộ qua network `flood-network`.

---

## 3. Chuẩn bị trước khi chạy

### 3.1. Lấy mã nguồn

```bash
git clone <repo-url>
cd flood-alert-system
```

### 3.2. Cấu trúc file/thư mục cần có

`docker-compose.yml` mount sẵn các đường dẫn sau — hãy đảm bảo chúng tồn tại:

```
flood-alert-system/
├── docker-compose.yml
├── secrets/
│   └── firebase-key.json          # (BẮT BUỘC) Service account key của Firebase
├── nginx/
│   ├── nginx.conf                 # Cấu hình reverse proxy
│   └── certbot/
│       ├── conf/                  # Chứng chỉ Let's Encrypt
│       └── www/                   # Webroot cho ACME challenge
├── mosquitto/
│   ├── config/mosquitto.conf      # Cấu hình MQTT broker
│   ├── data/
│   └── log/
├── postgres/
│   └── initdb/                    # Script khởi tạo DB (.sql chạy lần đầu)
│       └── init-db-floodalert.sql
├── api-gateway/.env
├── auth-service/.env
├── ingestion-service/.env
├── floodcore-service/.env
├── floodprocessor-service/.env
└── notification-service/.env
```

Tạo nhanh các thư mục còn thiếu:

```bash
mkdir -p secrets nginx/certbot/conf nginx/certbot/www mosquitto/data mosquitto/log postgres/initdb
```

> Repo đã có sẵn `init-db-floodalert.sql` ở thư mục gốc. Hãy copy nó vào `postgres/initdb/` để Postgres tự khởi tạo schema khi chạy lần đầu:
> ```bash
> cp init-db-floodalert.sql postgres/initdb/
> ```

### 3.3. Firebase key (bắt buộc)

`auth-service`, `floodcore-service`, `notification-service` đều mount `./secrets/firebase-key.json` (read-only).

1. Vào Firebase Console → Project Settings → Service accounts → **Generate new private key**.
2. Lưu file JSON tải về thành `secrets/firebase-key.json`.

---

## 4. Cấu hình biến môi trường (.env)

File `example.env` ở thư mục gốc là **mẫu tổng hợp** chứa biến của tất cả service, được nhóm theo từng phần (`#Auth Service`, `#FloodCore Service`, ...). **Mỗi service đọc file `.env` riêng** (theo `env_file` trong compose), nên cần tách biến tương ứng vào từng file.

### 4.1. Giá trị hạ tầng dùng chung

Các giá trị dưới đây khớp với cấu hình hạ tầng đã khai báo trong `docker-compose.yml` — dùng làm chuẩn khi điền `.env`:

| Biến | Giá trị                  | Nguồn |
|------|--------------------------|-------|
| Postgres host | `postgres`               | container name |
| Postgres user | `floodalert`             | `POSTGRES_USER` |
| Postgres password | `123456`                 | `POSTGRES_PASSWORD` |
| Postgres DB | `flood_alert_db`         | `POSTGRES_DB` |
| `REDIS_HOST` | `redis`                  | container name |
| `REDIS_PORT` | `6379`                   | |
| `REDIS_PASSWORD` | `123456`                 | tham số `--requirepass` của redis |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092`            | listener nội bộ `PLAINTEXT` |
| `MQTT_BROKER_URL` | `tcp://mqtt-broker:1883` | container name |

> **Quan trọng:** trong production hãy đổi toàn bộ mật khẩu mặc định (`postgres`, `redis`) và cập nhật đồng bộ ở cả `docker-compose.yml` lẫn các file `.env`.

### 4.2. Tạo file .env cho từng service

Dựa theo các nhóm trong `example.env`, tạo các file sau (mỗi file chỉ chứa biến của service đó):

**`auth-service/.env`**
```env
DB_URL_AUTH=postgresql://postgres:5432/flood_alert_db?currentSchema=auth,public
DB_USERNAME_AUTH=floodalert
DB_PASSWORD_AUTH=123456
SECRET_KEY_JWT=<chuỗi-bí-mật-jwt-đủ-dài>
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=123456
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
KAFKA_FCM_TOKEN_TOPIC=fcm-token-events
FIREBASE_CONFIG_PATH=file:/app/secrets/firebase-key.json
```

**`floodcore-service/.env`**
```env
DB_URL_FLOODCORE=postgresql://postgres:5432/flood_alert_db?currentSchema=flood_core,public
DB_USERNAME_FLOODCORE=floodalert
DB_PASSWORD_FLOODCORE=123456
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=123456
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
KAFKA_LIFECYCLE_TOPIC=flood-lifecycle-events
KAFKA_SENSOR_HEALTH_SYNC_TOPIC=sensor-health-sync
KAFKA_USER_REPORT_TOPIC=user-report-events
FLOOD_PROCESSOR_URL=http://floodprocessor-service:8084
FIREBASE_STORAGE_BUCKET=<your-bucket>.appspot.com
FIREBASE_SERVICE_ACCOUNT_PATH=file:/app/secrets/firebase-key.json
FIREBASE_SIGNED_URL_DURATION=15
ORS_API_KEY=<OpenRouteService API key>
```

**`ingestion-service/.env`**
```env
SERVER_PORT=8082
SPRING_APPLICATION_NAME=ingestion-service
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=123456
REDIS_DATABASE=0
REDIS_TIMEOUT=5000
MQTT_BROKER_URL=tcp://mqtt-broker:1883
MQTT_CLIENT_ID=ingestion-service
MQTT_TOPIC_PATTERN=floodguard/sensors/+/telemetry
MQTT_QOS=1
MQTT_AUTO_RECONNECT=true
MQTT_USERNAME=
MQTT_PASSWORD=
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
KAFKA_INGEST_TOPIC=iot-raw
KAFKA_PRODUCER_KEY_SERIALIZER=org.apache.kafka.common.serialization.StringSerializer
KAFKA_PRODUCER_VALUE_SERIALIZER=org.springframework.kafka.support.serializer.JsonSerializer
KAFKA_PRODUCER_ACKS=all
KAFKA_PRODUCER_RETRIES=3
LOGGING_LEVEL_KAFKA=WARN
LOGGING_LEVEL_SPRING_KAFKA=INFO
```

**`floodprocessor-service/.env`**
```env
DB_URL_FLOODPROCESSOR=postgresql://postgres:5432/flood_alert_db?currentSchema=flood_processor,public
DB_USERNAME_FLOODPROCESSOR=floodalert
DB_PASSWORD_FLOODPROCESSOR=123456
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=123456
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
AUTH_SERVICE_URL=http://auth-service:8081
GEMINI_API_KEY=<Google Gemini API key>
```

**`notification-service/.env`**
```env
DB_URL_NOTIFICATION=postgresql://postgres:5432/flood_alert_db?currentSchema=notification,public
DB_USERNAME_NOTIFICATION=floodalert
DB_PASSWORD_NOTIFICATION=123456
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=123456
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
FIREBASE_SERVICE_ACCOUNT_PATH=file:/app/secrets/firebase-key.json
```

**`api-gateway/.env`** (đã có sẵn trong repo, kiểm tra lại `CORS_ALLOWED_ORIGINS`)
```env
SERVER_PORT=8080
AUTH_SERVICE_URL=http://auth-service:8081
FLOODCORE_SERVICE_URL=http://floodcore-service:8083
INGESTION_SERVICE_URL=http://ingestion-service:8082
NOTIFICATION_SERVICE_URL=http://notification-service:8085
AUTH_VERIFY_URL=http://auth-service:8081/api/v1/auth/verify
CORS_ALLOWED_ORIGINS=https://admin.yourdomain.com
```

### 4.3. Các API key bên ngoài cần đăng ký

| Biến | Service | Lấy ở đâu |
|------|---------|-----------|
| `SECRET_KEY_JWT` | auth | Tự sinh chuỗi ngẫu nhiên (≥ 256-bit), ví dụ `openssl rand -base64 64` |
| `ORS_API_KEY` | floodcore | https://openrouteservice.org (định tuyến đường đi) |
| `GEMINI_API_KEY` | floodprocessor | https://aistudio.google.com (chấm điểm tin cậy báo cáo) |
| `FIREBASE_STORAGE_BUCKET` | floodcore | Firebase Console → Storage |

---

## 5. Khởi chạy hệ thống

```bash
# Pull toàn bộ image
docker compose pull

# Chạy nền (detached)
docker compose up -d

# Theo dõi log tổng
docker compose logs -f
```

Thứ tự khởi động (theo `depends_on` + `healthcheck`):
1. Hạ tầng: `postgres`, `redis`, `kafka`, `mqtt-broker` khởi động trước.
2. Các microservice nghiệp vụ chờ tới khi **healthy**.
3. `api-gateway` chờ `auth`, `floodcore`, `notification`, `ingestion` healthy rồi mới chạy.
4. `nginx` + `certbot` cuối cùng.

> Lần chạy đầu cần vài phút để các service hoàn tất `start_period` (60s) và Postgres chạy script `initdb`.

---

## 6. Kiểm tra sau khi cài đặt

```bash
# Trạng thái container (cột STATUS nên là healthy/running)
docker compose ps

# Kiểm tra health của từng service (chạy bên trong network)
docker compose exec auth-service wget -qO- http://localhost:8081/actuator/health
docker compose exec floodcore-service wget -qO- http://localhost:8083/actuator/health
docker compose exec notification-service wget -qO- http://localhost:8085/actuator/health

# Kiểm tra Postgres đã sẵn sàng
docker compose exec postgres pg_isready -U floodalert -d flood_alert_db

# Truy cập qua gateway (qua Nginx)
curl http://localhost/actuator/health
```

---

## 7. Lệnh vận hành thường dùng

```bash
docker compose ps                       # Xem trạng thái
docker compose logs -f <service>        # Log 1 service, ví dụ: floodprocessor-service
docker compose restart <service>        # Khởi động lại 1 service
docker compose down                     # Dừng & xóa container (giữ volume dữ liệu)
docker compose down -v                  # ⚠️ Dừng & XÓA luôn volume (mất sạch DB/Redis/Kafka)
docker compose up -d --pull always      # Cập nhật lên image mới nhất
```

Dữ liệu được lưu ở các named volume: `postgres_data`, `redis_data`, `kafka_data` (không mất khi `docker compose down` thông thường).

---

## 8. Xử lý sự cố thường gặp

| Triệu chứng | Nguyên nhân & cách xử lý |
|-------------|--------------------------|
| Service báo `unhealthy` / restart liên tục | Xem `docker compose logs <service>`. Thường do `.env` thiếu biến hoặc sai mật khẩu DB/Redis. |
| `api-gateway` không khởi động | Một trong các service phụ thuộc chưa healthy. Kiểm tra `auth/floodcore/notification/ingestion`. |
| Lỗi kết nối DB | Kiểm tra `DB_USERNAME/PASSWORD` trong `.env` khớp với `POSTGRES_USER/PASSWORD` của compose; host phải là `postgres`. |
| Lỗi Firebase / FCM | Thiếu `secrets/firebase-key.json` hoặc sai `FIREBASE_*`. |
| Không nhận data IoT | Kiểm tra `mosquitto/config/mosquitto.conf` và `MQTT_BROKER_URL=tcp://mqtt-broker:1883`. |
| Hết RAM, container bị OOM-killed | Giảm `deploy.resources.limits.memory` hoặc tắt bớt service không cần. |
| Postgres không tạo schema | File `.sql` phải nằm trong `postgres/initdb/` **trước** lần chạy đầu; nếu đã chạy rồi phải `down -v` để init lại. |

---

## 9. Ghi chú bảo mật

- `docker-compose.yml` và `example.env` chứa **mật khẩu mặc định** chỉ dùng cho môi trường dev/demo. Bắt buộc thay đổi khi lên production.
- Không commit `secrets/firebase-key.json` và các file `.env` thật lên git (kiểm tra `.gitignore`).
- Trong production, cân nhắc đóng các port `5432`, `6379`, `9092`, `1883` khỏi internet (chỉ expose qua Nginx).
