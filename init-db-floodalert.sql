--
-- PostgreSQL database dump
--

-- Dumped from database version 15.8
-- Dumped by pg_dump version 17.0

-- Started on 2026-06-24 11:58:07

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 11 (class 2615 OID 16385)
-- Name: auth; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA auth;


--
-- TOC entry 12 (class 2615 OID 16386)
-- Name: flood_core; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA flood_core;


--
-- TOC entry 13 (class 2615 OID 16387)
-- Name: flood_processor; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA flood_processor;


--
-- TOC entry 14 (class 2615 OID 16388)
-- Name: notification; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA notification;


--
-- TOC entry 1109 (class 1255 OID 17618)
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: auth; Owner: -
--

CREATE FUNCTION auth.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- TOC entry 4750 (class 0 OID 0)
-- Dependencies: 1109
-- Name: FUNCTION update_updated_at_column(); Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON FUNCTION auth.update_updated_at_column() IS 'Function tự động cập nhật trường updated_at khi có thay đổi dữ liệu';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 228 (class 1259 OID 17619)
-- Name: audit_logs; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.audit_logs (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid,
    action character varying(100) NOT NULL,
    resource_type character varying(50),
    resource_id uuid,
    ip_address inet,
    user_agent text,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 4751 (class 0 OID 0)
-- Dependencies: 228
-- Name: TABLE audit_logs; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON TABLE auth.audit_logs IS 'Bảng ghi lại lịch sử hành động của người dùng (audit trail)';


--
-- TOC entry 4752 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.id IS 'UUID duy nhất của bản ghi log';


--
-- TOC entry 4753 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.user_id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.user_id IS 'ID người dùng thực hiện hành động (NULL nếu là hành động hệ thống)';


--
-- TOC entry 4754 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.action; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.action IS 'Loại hành động (VD: LOGIN, LOGOUT, PASSWORD_CHANGE, REPORT_CREATE, VOTE)';


--
-- TOC entry 4755 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.resource_type; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.resource_type IS 'Loại tài nguyên bị tác động (VD: "user", "flood_event", "report")';


--
-- TOC entry 4756 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.resource_id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.resource_id IS 'ID của tài nguyên bị tác động';


--
-- TOC entry 4757 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.ip_address; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.ip_address IS 'Địa chỉ IP của người dùng khi thực hiện hành động';


--
-- TOC entry 4758 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.user_agent; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.user_agent IS 'Thông tin trình duyệt/ứng dụng (User Agent string)';


--
-- TOC entry 4759 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.metadata; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.metadata IS 'Dữ liệu bổ sung dạng JSON (VD: {"old_email": "...", "new_email": "..."})';


--
-- TOC entry 4760 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN audit_logs.created_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.audit_logs.created_at IS 'Thời điểm ghi log hành động';


--
-- TOC entry 229 (class 1259 OID 17626)
-- Name: fcm_tokens; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.fcm_tokens (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    token text NOT NULL,
    device_type character varying(20),
    device_id character varying(255),
    device_name character varying(255),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_used_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 4761 (class 0 OID 0)
-- Dependencies: 229
-- Name: TABLE fcm_tokens; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON TABLE auth.fcm_tokens IS 'Bảng lưu trữ token FCM để gửi push notification';


--
-- TOC entry 4762 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.id IS 'UUID duy nhất của token';


--
-- TOC entry 4763 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.user_id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.user_id IS 'ID người dùng sở hữu thiết bị này';


--
-- TOC entry 4764 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.token; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.token IS 'FCM token để gửi thông báo đẩy (duy nhất)';


--
-- TOC entry 4765 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.device_type; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.device_type IS 'Loại thiết bị: ANDROID, IOS, WEB';


--
-- TOC entry 4766 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.device_id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.device_id IS 'ID định danh thiết bị (UUID hoặc Device ID)';


--
-- TOC entry 4767 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.device_name; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.device_name IS 'Tên thiết bị (VD: "iPhone 13", "Samsung Galaxy S21")';


--
-- TOC entry 4768 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.is_active; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.is_active IS 'Token có còn hoạt động không (false nếu người dùng đăng xuất hoặc gỡ app)';


--
-- TOC entry 4769 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.created_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.created_at IS 'Thời điểm đăng ký token lần đầu';


--
-- TOC entry 4770 (class 0 OID 0)
-- Dependencies: 229
-- Name: COLUMN fcm_tokens.last_used_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.fcm_tokens.last_used_at IS 'Thời điểm gửi thông báo đến token này lần cuối';


--
-- TOC entry 230 (class 1259 OID 17635)
-- Name: invalidated_tokens; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.invalidated_tokens (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    token_jti character varying(255) NOT NULL,
    token_type character varying(20) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    invalidated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    invalidated_reason character varying(50),
    ip_address character varying(45),
    user_agent text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 231 (class 1259 OID 17643)
-- Name: roles; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.roles (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(50) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 4771 (class 0 OID 0)
-- Dependencies: 231
-- Name: TABLE roles; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON TABLE auth.roles IS 'Bảng định nghĩa các vai trò trong hệ thống';


--
-- TOC entry 4772 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN roles.id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.roles.id IS 'UUID duy nhất của vai trò';


--
-- TOC entry 4773 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN roles.name; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.roles.name IS 'Tên vai trò (VD: USER, ADMIN, MODERATOR) - không trùng lặp';


--
-- TOC entry 4774 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN roles.description; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.roles.description IS 'Mô tả chi tiết về vai trò và quyền hạn';


--
-- TOC entry 4775 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN roles.created_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.roles.created_at IS 'Thời điểm tạo vai trò';


--
-- TOC entry 232 (class 1259 OID 17650)
-- Name: user_addresses; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.user_addresses (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    address_text text NOT NULL,
    lat numeric(10,8) NOT NULL,
    lon numeric(11,8) NOT NULL,
    is_primary boolean DEFAULT false,
    address_type character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 4776 (class 0 OID 0)
-- Dependencies: 232
-- Name: TABLE user_addresses; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON TABLE auth.user_addresses IS 'Bảng lưu trữ các địa chỉ quan trọng của người dùng';


--
-- TOC entry 4777 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.id IS 'UUID duy nhất của địa chỉ';


--
-- TOC entry 4778 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.user_id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.user_id IS 'ID người dùng sở hữu địa chỉ này';


--
-- TOC entry 4779 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.address_text; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.address_text IS 'Địa chỉ dạng văn bản (VD: "123 Nguyễn Huệ, Q1, TP.HCM")';


--
-- TOC entry 4780 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.lat; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.lat IS 'Vĩ độ của địa chỉ';


--
-- TOC entry 4781 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.lon; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.lon IS 'Kinh độ của địa chỉ';


--
-- TOC entry 4782 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.is_primary; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.is_primary IS 'Có phải địa chỉ chính/mặc định không (dùng cho cảnh báo ưu tiên)';


--
-- TOC entry 4783 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.address_type; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.address_type IS 'Loại địa chỉ: HOME (nhà), OFFICE (văn phòng), SCHOOL (trường học)';


--
-- TOC entry 4784 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.created_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.created_at IS 'Thời điểm thêm địa chỉ';


--
-- TOC entry 4785 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN user_addresses.updated_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_addresses.updated_at IS 'Thời điểm cập nhật địa chỉ lần cuối';


--
-- TOC entry 233 (class 1259 OID 17659)
-- Name: user_profiles; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.user_profiles (
    user_id uuid NOT NULL,
    full_name character varying(255) NOT NULL,
    avatar_url text,
    reputation_score integer DEFAULT 50 NOT NULL,
    total_reports_submitted integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    CONSTRAINT check_reputation_score CHECK (((reputation_score >= 0) AND (reputation_score <= 100)))
);


--
-- TOC entry 234 (class 1259 OID 17670)
-- Name: user_roles; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.user_roles (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 4786 (class 0 OID 0)
-- Dependencies: 234
-- Name: TABLE user_roles; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON TABLE auth.user_roles IS 'Bảng liên kết người dùng với vai trò (many-to-many)';


--
-- TOC entry 4787 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN user_roles.id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_roles.id IS 'UUID duy nhất của liên kết';


--
-- TOC entry 4788 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN user_roles.user_id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_roles.user_id IS 'ID người dùng';


--
-- TOC entry 4789 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN user_roles.role_id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_roles.role_id IS 'ID vai trò được gán';


--
-- TOC entry 4790 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN user_roles.assigned_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.user_roles.assigned_at IS 'Thời điểm gán vai trò cho người dùng';


--
-- TOC entry 235 (class 1259 OID 17675)
-- Name: users; Type: TABLE; Schema: auth; Owner: -
--

CREATE TABLE auth.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255),
    phone character varying(20),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_login_at timestamp without time zone,
    firebase_uid character varying(128),
    auth_provider character varying(20) DEFAULT 'LOCAL'::character varying,
    email_verified boolean,
    CONSTRAINT check_login_credential CHECK (((email IS NOT NULL) OR (phone IS NOT NULL)))
);


--
-- TOC entry 4791 (class 0 OID 0)
-- Dependencies: 235
-- Name: TABLE users; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON TABLE auth.users IS 'Bảng quản lý thông tin người dùng hệ thống';


--
-- TOC entry 4792 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.id; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.id IS 'UUID duy nhất của người dùng';


--
-- TOC entry 4793 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.email; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.email IS 'Email đăng nhập (duy nhất, không trùng lặp)';


--
-- TOC entry 4794 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.password_hash; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.password_hash IS 'Mật khẩu đã mã hóa (bcrypt/argon2)';


--
-- TOC entry 4795 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.phone; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.phone IS 'Số điện thoại liên hệ';


--
-- TOC entry 4796 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.status; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.status IS 'Trạng thái tài khoản: ACTIVE (hoạt động), DISABLED (vô hiệu hóa), BANNED (bị cấm)';


--
-- TOC entry 4797 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.created_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.created_at IS 'Thời điểm tạo tài khoản';


--
-- TOC entry 4798 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.updated_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.updated_at IS 'Thời điểm cập nhật thông tin lần cuối (tự động cập nhật)';


--
-- TOC entry 4799 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN users.last_login_at; Type: COMMENT; Schema: auth; Owner: -
--

COMMENT ON COLUMN auth.users.last_login_at IS 'Thời điểm đăng nhập gần nhất';


--
-- TOC entry 236 (class 1259 OID 17686)
-- Name: flood_zones; Type: TABLE; Schema: flood_core; Owner: -
--

CREATE TABLE flood_core.flood_zones (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    center_lat numeric(10,8) NOT NULL,
    center_lon numeric(11,8) NOT NULL,
    radius_meters integer DEFAULT 100,
    boundary jsonb,
    level character varying(20),
    is_active boolean DEFAULT true,
    is_pinned boolean DEFAULT false,
    created_by uuid,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp without time zone
);


--
-- TOC entry 4800 (class 0 OID 0)
-- Dependencies: 236
-- Name: TABLE flood_zones; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON TABLE flood_core.flood_zones IS 'Bảng quản lý các vùng ngập lụt được định nghĩa trước';


--
-- TOC entry 4801 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.id IS 'UUID duy nhất của vùng ngập';


--
-- TOC entry 4802 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.name; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.name IS 'Tên vùng ngập (VD: "Khu vực Nguyễn Hữu Cảnh")';


--
-- TOC entry 4803 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.description; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.description IS 'Mô tả chi tiết về vùng ngập này';


--
-- TOC entry 4804 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.center_lat; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.center_lat IS 'Vĩ độ tâm của vùng ngập';


--
-- TOC entry 4805 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.center_lon; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.center_lon IS 'Kinh độ tâm của vùng ngập';


--
-- TOC entry 4806 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.radius_meters; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.radius_meters IS 'Bán kính vùng ngập tính từ tâm (đơn vị: mét)';


--
-- TOC entry 4807 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.boundary; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.boundary IS 'Ranh giới vùng ngập dạng polygon (sử dụng PostGIS hoặc GeoJSON)';


--
-- TOC entry 4808 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.level; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.level IS 'Mức độ ngập: LOW (thấp), MEDIUM (trung bình), HIGH (cao), CRITICAL (nghiêm trọng)';


--
-- TOC entry 4809 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.is_active; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.is_active IS 'Vùng ngập có đang hoạt động/hiệu lực không (true/false)';


--
-- TOC entry 4810 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.is_pinned; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.is_pinned IS 'Admin có ghim cảnh báo khẩn cấp cho vùng này không (hiển thị ưu tiên)';


--
-- TOC entry 4811 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.created_by; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.created_by IS 'ID người tạo vùng ngập (admin)';


--
-- TOC entry 4812 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.created_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.created_at IS 'Thời điểm tạo vùng ngập';


--
-- TOC entry 4813 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.updated_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.updated_at IS 'Thời điểm cập nhật thông tin vùng ngập lần cuối';


--
-- TOC entry 4814 (class 0 OID 0)
-- Dependencies: 236
-- Name: COLUMN flood_zones.expires_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.flood_zones.expires_at IS 'Thời điểm vùng ngập này hết hiệu lực (tự động vô hiệu hóa)';


--
-- TOC entry 237 (class 1259 OID 17697)
-- Name: active_flood_zones; Type: VIEW; Schema: flood_core; Owner: -
--

CREATE VIEW flood_core.active_flood_zones AS
 SELECT flood_zones.id,
    flood_zones.name,
    flood_zones.description,
    flood_zones.center_lat,
    flood_zones.center_lon,
    flood_zones.radius_meters,
    flood_zones.boundary,
    flood_zones.level,
    flood_zones.is_active,
    flood_zones.is_pinned,
    flood_zones.created_by,
    flood_zones.created_at,
    flood_zones.updated_at,
    flood_zones.expires_at
   FROM flood_core.flood_zones
  WHERE ((flood_zones.is_active = true) AND ((flood_zones.expires_at IS NULL) OR (flood_zones.expires_at > now())));


--
-- TOC entry 4815 (class 0 OID 0)
-- Dependencies: 237
-- Name: VIEW active_flood_zones; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON VIEW flood_core.active_flood_zones IS 'View hiển thị các vùng ngập đang hoạt động và chưa hết hạn';


--
-- TOC entry 238 (class 1259 OID 17701)
-- Name: core_active_floods; Type: TABLE; Schema: flood_core; Owner: -
--

CREATE TABLE flood_core.core_active_floods (
    event_id character varying(50) NOT NULL,
    lat numeric(10,8) NOT NULL,
    lon numeric(11,8) NOT NULL,
    location_description text,
    water_level numeric(5,2),
    severity_level character varying(20),
    status character varying(20) NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    source character varying(20)
);


--
-- TOC entry 239 (class 1259 OID 17707)
-- Name: sensor_logs; Type: TABLE; Schema: flood_core; Owner: -
--

CREATE TABLE flood_core.sensor_logs (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    sensor_id uuid NOT NULL,
    action character varying(50) NOT NULL,
    performed_by uuid,
    old_value jsonb,
    new_value jsonb,
    comment text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 4816 (class 0 OID 0)
-- Dependencies: 239
-- Name: TABLE sensor_logs; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON TABLE flood_core.sensor_logs IS 'Bảng ghi lại lịch sử thay đổi của cảm biến (audit trail)';


--
-- TOC entry 4817 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.id IS 'UUID duy nhất của bản ghi log';


--
-- TOC entry 4818 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.sensor_id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.sensor_id IS 'Liên kết đến cảm biến bị thay đổi';


--
-- TOC entry 4819 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.action; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.action IS 'Loại hành động: CREATED (tạo mới), DISABLED (tắt), ENABLED (bật), LOCATION_UPDATED (cập nhật vị trí)';


--
-- TOC entry 4820 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.performed_by; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.performed_by IS 'ID người thực hiện hành động (admin/kỹ thuật viên)';


--
-- TOC entry 4821 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.old_value; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.old_value IS 'Giá trị cũ trước khi thay đổi (dạng JSON)';


--
-- TOC entry 4822 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.new_value; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.new_value IS 'Giá trị mới sau khi thay đổi (dạng JSON)';


--
-- TOC entry 4823 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.comment; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.comment IS 'Ghi chú/lý do thực hiện thay đổi';


--
-- TOC entry 4824 (class 0 OID 0)
-- Dependencies: 239
-- Name: COLUMN sensor_logs.created_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensor_logs.created_at IS 'Thời điểm ghi log hành động';


--
-- TOC entry 240 (class 1259 OID 17714)
-- Name: sensors; Type: TABLE; Schema: flood_core; Owner: -
--

CREATE TABLE flood_core.sensors (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    sensor_id character varying(50) NOT NULL,
    name character varying(255),
    location_name text,
    lat numeric(10,8) NOT NULL,
    lon numeric(11,8) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    api_key character varying(255) NOT NULL,
    hardware_model character varying(100),
    firmware_version character varying(50),
    battery_level integer,
    signal_strength integer,
    installed_at timestamp without time zone,
    last_heartbeat timestamp without time zone,
    last_reading_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by uuid,
    warning_threshold numeric(5,2),
    danger_threshold numeric(5,2),
    is_virtual boolean DEFAULT false,
    CONSTRAINT sensors_battery_level_check CHECK (((battery_level >= 0) AND (battery_level <= 100)))
);


--
-- TOC entry 4825 (class 0 OID 0)
-- Dependencies: 240
-- Name: TABLE sensors; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON TABLE flood_core.sensors IS 'Bảng quản lý thông tin các cảm biến IoT đo mực nước';


--
-- TOC entry 4826 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.id IS 'UUID duy nhất của cảm biến';


--
-- TOC entry 4827 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.sensor_id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.sensor_id IS 'Mã cảm biến dễ đọc (VD: SENSOR_001), không trùng lặp';


--
-- TOC entry 4828 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.name; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.name IS 'Tên gọi của cảm biến';


--
-- TOC entry 4829 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.location_name; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.location_name IS 'Tên địa điểm lắp đặt cảm biến (VD: "Cầu Nguyễn Văn Cừ, Quận 1")';


--
-- TOC entry 4830 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.lat; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.lat IS 'Vĩ độ vị trí lắp đặt cảm biến';


--
-- TOC entry 4831 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.lon; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.lon IS 'Kinh độ vị trí lắp đặt cảm biến';


--
-- TOC entry 4832 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.status; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.status IS 'Trạng thái hoạt động: ACTIVE (hoạt động), DISABLED (tắt), MAINTENANCE (bảo trì), OFFLINE (mất kết nối)';


--
-- TOC entry 4833 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.api_key; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.api_key IS 'Khóa API duy nhất để xác thực cảm biến khi gửi dữ liệu';


--
-- TOC entry 4834 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.hardware_model; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.hardware_model IS 'Model/loại phần cứng của cảm biến (VD: "HC-SR04", "WaterLevel-v2")';


--
-- TOC entry 4835 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.firmware_version; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.firmware_version IS 'Phiên bản firmware đang chạy trên cảm biến (VD: "1.2.3")';


--
-- TOC entry 4836 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.battery_level; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.battery_level IS 'Mức pin hiện tại của cảm biến (0-100%)';


--
-- TOC entry 4837 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.signal_strength; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.signal_strength IS 'Cường độ tín hiệu mạng hiện tại (đơn vị: dBm)';


--
-- TOC entry 4838 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.installed_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.installed_at IS 'Thời điểm lắp đặt cảm biến';


--
-- TOC entry 4839 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.last_heartbeat; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.last_heartbeat IS 'Thời điểm cảm biến gửi tín hiệu "còn sống" lần cuối';


--
-- TOC entry 4840 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.last_reading_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.last_reading_at IS 'Thời điểm cảm biến gửi dữ liệu đo lần cuối';


--
-- TOC entry 4841 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.created_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.created_at IS 'Thời điểm tạo bản ghi trong database';


--
-- TOC entry 4842 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.updated_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.updated_at IS 'Thời điểm cập nhật thông tin cảm biến lần cuối';


--
-- TOC entry 4843 (class 0 OID 0)
-- Dependencies: 240
-- Name: COLUMN sensors.created_by; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.sensors.created_by IS 'ID người tạo/đăng ký cảm biến (admin/kỹ thuật viên)';


--
-- TOC entry 241 (class 1259 OID 17724)
-- Name: user_reports; Type: TABLE; Schema: flood_core; Owner: -
--

CREATE TABLE flood_core.user_reports (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    report_id character varying(50) NOT NULL,
    user_id uuid NOT NULL,
    flood_event_id character varying,
    description text,
    image_urls text,
    severity_level character varying(20),
    lat numeric(10,8) NOT NULL,
    lon numeric(11,8) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    reviewed_by uuid,
    reviewed_at timestamp without time zone,
    reject_reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    score double precision,
    ai_score double precision,
    spatial_score double precision,
    reputation_score double precision
);


--
-- TOC entry 4844 (class 0 OID 0)
-- Dependencies: 241
-- Name: TABLE user_reports; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON TABLE flood_core.user_reports IS 'Bảng lưu trữ các báo cáo ngập lụt từ người dùng';


--
-- TOC entry 4845 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.id IS 'UUID duy nhất của báo cáo';


--
-- TOC entry 4846 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.report_id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.report_id IS 'Mã báo cáo dễ đọc (VD: UR_20240320_001)';


--
-- TOC entry 4847 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.user_id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.user_id IS 'ID người dùng gửi báo cáo (tham chiếu đến bảng auth.users)';


--
-- TOC entry 4848 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.flood_event_id; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.flood_event_id IS 'Liên kết đến sự kiện ngập lụt tương ứng';


--
-- TOC entry 4849 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.description; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.description IS 'Mô tả chi tiết tình hình ngập do người dùng viết';


--
-- TOC entry 4850 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.image_urls; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.image_urls IS 'Danh sách link ảnh chụp hiện trường';


--
-- TOC entry 4851 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.severity_level; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.severity_level IS 'Đánh giá mức độ nghiêm trọng của người dùng';


--
-- TOC entry 4852 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.lat; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.lat IS 'Vĩ độ nơi người dùng báo cáo ngập';


--
-- TOC entry 4853 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.lon; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.lon IS 'Kinh độ nơi người dùng báo cáo ngập';


--
-- TOC entry 4854 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.status; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.status IS 'Trạng thái duyệt: PENDING (chờ duyệt), APPROVED (chấp nhận), REJECTED (từ chối)';


--
-- TOC entry 4855 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.reviewed_by; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.reviewed_by IS 'ID người duyệt báo cáo (admin/moderator)';


--
-- TOC entry 4856 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.reviewed_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.reviewed_at IS 'Thời điểm báo cáo được xét duyệt';


--
-- TOC entry 4857 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.reject_reason; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.reject_reason IS 'Lý do từ chối báo cáo (nếu bị từ chối)';


--
-- TOC entry 4858 (class 0 OID 0)
-- Dependencies: 241
-- Name: COLUMN user_reports.created_at; Type: COMMENT; Schema: flood_core; Owner: -
--

COMMENT ON COLUMN flood_core.user_reports.created_at IS 'Thời điểm người dùng tạo báo cáo';


--
-- TOC entry 242 (class 1259 OID 17732)
-- Name: alert_triggers; Type: TABLE; Schema: flood_processor; Owner: -
--

CREATE TABLE flood_processor.alert_triggers (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    trigger_id character varying(50) NOT NULL,
    flood_event_id uuid,
    trigger_type character varying(50) NOT NULL,
    trigger_condition jsonb,
    affected_users_count integer DEFAULT 0,
    search_radius_meters integer,
    search_center_lat numeric(10,8),
    search_center_lon numeric(11,8),
    status character varying(20) DEFAULT 'TRIGGERED'::character varying,
    triggered_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamp without time zone
);


--
-- TOC entry 4859 (class 0 OID 0)
-- Dependencies: 242
-- Name: TABLE alert_triggers; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON TABLE flood_processor.alert_triggers IS 'Bảng lưu trữ các lần kích hoạt cảnh báo gửi cho người dùng';


--
-- TOC entry 4860 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.id IS 'UUID duy nhất của lần kích hoạt cảnh báo';


--
-- TOC entry 4861 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.trigger_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.trigger_id IS 'Mã kích hoạt dễ đọc (VD: AT_20240320_001)';


--
-- TOC entry 4862 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.flood_event_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.flood_event_id IS 'Liên kết đến sự kiện ngập lụt gây ra cảnh báo';


--
-- TOC entry 4863 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.trigger_type; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.trigger_type IS 'Loại cảnh báo: NEW_FLOOD (ngập mới), LEVEL_INCREASE (mực nước tăng), PROXIMITY (gần vị trí người dùng), MANUAL (kích hoạt thủ công)';


--
-- TOC entry 4864 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.trigger_condition; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.trigger_condition IS 'Điều kiện kích hoạt cảnh báo dạng JSON (VD: {"min_level": 50, "radius": 1000})';


--
-- TOC entry 4865 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.affected_users_count; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.affected_users_count IS 'Số lượng người dùng bị ảnh hưởng và nhận cảnh báo';


--
-- TOC entry 4866 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.search_radius_meters; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.search_radius_meters IS 'Bán kính tìm kiếm người dùng xung quanh điểm ngập (đơn vị: mét)';


--
-- TOC entry 4867 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.search_center_lat; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.search_center_lat IS 'Vĩ độ của tâm vùng tìm kiếm người dùng';


--
-- TOC entry 4868 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.search_center_lon; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.search_center_lon IS 'Kinh độ của tâm vùng tìm kiếm người dùng';


--
-- TOC entry 4869 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.status; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.status IS 'Trạng thái xử lý: TRIGGERED (đã kích hoạt), PROCESSING (đang xử lý), COMPLETED (hoàn tất), FAILED (thất bại)';


--
-- TOC entry 4870 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.triggered_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.triggered_at IS 'Thời điểm kích hoạt cảnh báo';


--
-- TOC entry 4871 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN alert_triggers.completed_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.alert_triggers.completed_at IS 'Thời điểm hoàn tất việc gửi cảnh báo cho tất cả người dùng';


--
-- TOC entry 243 (class 1259 OID 17741)
-- Name: event_contributors; Type: TABLE; Schema: flood_processor; Owner: -
--

CREATE TABLE flood_processor.event_contributors (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    flood_event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    report_id character varying(50)
);


--
-- TOC entry 244 (class 1259 OID 17746)
-- Name: flood_events; Type: TABLE; Schema: flood_processor; Owner: -
--

CREATE TABLE flood_processor.flood_events (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    event_id character varying(50) NOT NULL,
    source character varying(20) NOT NULL,
    source_id character varying(100),
    lat numeric(10,8) NOT NULL,
    lon numeric(11,8) NOT NULL,
    location_description text,
    geo_hash character varying(20),
    water_level numeric(5,2),
    severity_level character varying(20),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    confidence_score numeric(3,2) DEFAULT 0.0,
    vote_count integer DEFAULT 1,
    raw_data jsonb,
    tags text[],
    processed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    confirmed_at timestamp without time zone,
    expires_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT flood_events_confidence_score_check CHECK (((confidence_score >= (0)::numeric) AND (confidence_score <= (1)::numeric)))
);


--
-- TOC entry 4872 (class 0 OID 0)
-- Dependencies: 244
-- Name: TABLE flood_events; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON TABLE flood_processor.flood_events IS 'Bảng lưu trữ các sự kiện ngập lụt từ nhiều nguồn';


--
-- TOC entry 4873 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.id IS 'UUID duy nhất của sự kiện (tự động sinh)';


--
-- TOC entry 4874 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.event_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.event_id IS 'Mã sự kiện dễ đọc (VD: FE_20240320_001), không trùng lặp';


--
-- TOC entry 4875 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.source; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.source IS 'Nguồn báo cáo: IOT (cảm biến), USER (người dùng), MANUAL (nhập tay)';


--
-- TOC entry 4876 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.source_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.source_id IS 'Mã định danh nguồn - ID cảm biến hoặc ID người dùng';


--
-- TOC entry 4877 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.lat; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.lat IS 'Vĩ độ (latitude) - tọa độ địa lý, chính xác đến 8 chữ số thập phân';


--
-- TOC entry 4878 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.lon; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.lon IS 'Kinh độ (longitude) - tọa độ địa lý, chính xác đến 8 chữ số thập phân';


--
-- TOC entry 4879 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.location_description; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.location_description IS 'Mô tả địa điểm bằng văn bản (VD: "Đường Nguyễn Huệ, Quận 1")';


--
-- TOC entry 4880 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.geo_hash; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.geo_hash IS 'Mã hash địa lý để tìm kiếm nhanh các vị trí gần nhau';


--
-- TOC entry 4881 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.water_level; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.water_level IS 'Mực nước đo được từ cảm biến IoT (đơn vị: cm)';


--
-- TOC entry 4882 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.severity_level; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.severity_level IS 'Mức độ nghiêm trọng: LOW (thấp), MEDIUM (trung bình), HIGH (cao), CRITICAL (nghiêm trọng)';


--
-- TOC entry 4883 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.status; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.status IS 'Trạng thái xử lý: PENDING (chờ xử lý), CONFIRMED (đã xác nhận), REJECTED (từ chối), EXPIRED (hết hạn)';


--
-- TOC entry 4884 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.confidence_score; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.confidence_score IS 'Điểm tin cậy từ 0.0 đến 1.0 - càng cao càng đáng tin cậy';


--
-- TOC entry 4885 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.vote_count; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.vote_count IS 'Số lượt bình chọn/xác nhận từ người dùng';


--
-- TOC entry 4886 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.raw_data; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.raw_data IS 'Dữ liệu thô dạng JSON - lưu toàn bộ thông tin gốc chưa xử lý';


--
-- TOC entry 4887 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.tags; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.tags IS 'Các thẻ gắn nhãn (VD: ["mưa_lớn", "tắc_đường", "ngập_sâu"])';


--
-- TOC entry 4888 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.processed_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.processed_at IS 'Thời điểm hệ thống xử lý sự kiện này';


--
-- TOC entry 4889 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.confirmed_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.confirmed_at IS 'Thời điểm sự kiện được xác nhận là có thật';


--
-- TOC entry 4890 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.expires_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.expires_at IS 'Thời điểm hết hiệu lực - sự kiện không còn xảy ra';


--
-- TOC entry 4891 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.created_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.created_at IS 'Thời điểm tạo bản ghi trong database';


--
-- TOC entry 4892 (class 0 OID 0)
-- Dependencies: 244
-- Name: COLUMN flood_events.updated_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.flood_events.updated_at IS 'Thời điểm cập nhật bản ghi lần cuối';


--
-- TOC entry 245 (class 1259 OID 17759)
-- Name: iot_readings; Type: TABLE; Schema: flood_processor; Owner: -
--

CREATE TABLE flood_processor.iot_readings (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    reading_id character varying(50) NOT NULL,
    sensor_id character varying(50) NOT NULL,
    flood_event_id uuid,
    water_level numeric(5,2),
    battery_level integer,
    signal_strength integer,
    temperature numeric(4,1),
    humidity numeric(4,1),
    measured_at timestamp without time zone NOT NULL,
    received_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    raw_payload jsonb,
    status character varying(20),
    CONSTRAINT chk_status_valid CHECK (((status IS NULL) OR ((status)::text = ANY (ARRAY[('SAFE'::character varying)::text, ('WARNING'::character varying)::text, ('DANGER'::character varying)::text, ('UNKNOWN'::character varying)::text])))),
    CONSTRAINT iot_readings_battery_level_check CHECK (((battery_level >= 0) AND (battery_level <= 100)))
);


--
-- TOC entry 4893 (class 0 OID 0)
-- Dependencies: 245
-- Name: TABLE iot_readings; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON TABLE flood_processor.iot_readings IS 'Bảng lưu trữ dữ liệu thô từ các cảm biến IoT';


--
-- TOC entry 4894 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.id IS 'UUID duy nhất của bản đọc cảm biến';


--
-- TOC entry 4895 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.reading_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.reading_id IS 'Mã bản đọc dễ nhớ (VD: IR_20240320_001)';


--
-- TOC entry 4896 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.sensor_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.sensor_id IS 'Mã cảm biến (tham chiếu đến bảng flood_core.sensors)';


--
-- TOC entry 4897 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.flood_event_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.flood_event_id IS 'Liên kết đến sự kiện ngập lụt được tạo từ bản đọc này';


--
-- TOC entry 4898 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.water_level; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.water_level IS 'Mực nước đo được từ cảm biến (đơn vị: cm)';


--
-- TOC entry 4899 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.battery_level; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.battery_level IS 'Mức pin còn lại của cảm biến (0-100%)';


--
-- TOC entry 4900 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.signal_strength; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.signal_strength IS 'Cường độ tín hiệu mạng của cảm biến (đơn vị: dBm)';


--
-- TOC entry 4901 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.temperature; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.temperature IS 'Nhiệt độ môi trường tại vị trí cảm biến (đơn vị: °C)';


--
-- TOC entry 4902 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.humidity; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.humidity IS 'Độ ẩm không khí (đơn vị: %)';


--
-- TOC entry 4903 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.measured_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.measured_at IS 'Thời điểm cảm biến thực hiện phép đo';


--
-- TOC entry 4904 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.received_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.received_at IS 'Thời điểm server nhận được dữ liệu từ cảm biến';


--
-- TOC entry 4905 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN iot_readings.raw_payload; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.iot_readings.raw_payload IS 'Toàn bộ dữ liệu gốc dạng JSON từ cảm biến (chưa xử lý)';


--
-- TOC entry 246 (class 1259 OID 17768)
-- Name: trust_scores; Type: TABLE; Schema: flood_processor; Owner: -
--

CREATE TABLE flood_processor.trust_scores (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    flood_event_id uuid NOT NULL,
    iot_score numeric(3,2),
    user_vote_score numeric(3,2),
    historical_score numeric(3,2),
    spatial_correlation_score numeric(3,2),
    final_score numeric(3,2) NOT NULL,
    calculation_method character varying(50),
    algorithm_version character varying(20),
    factors jsonb,
    calculated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT trust_scores_final_score_check CHECK (((final_score >= (0)::numeric) AND (final_score <= (1)::numeric))),
    CONSTRAINT trust_scores_historical_score_check CHECK (((historical_score >= (0)::numeric) AND (historical_score <= (1)::numeric))),
    CONSTRAINT trust_scores_iot_score_check CHECK (((iot_score >= (0)::numeric) AND (iot_score <= (1)::numeric))),
    CONSTRAINT trust_scores_user_vote_score_check CHECK (((user_vote_score >= (0)::numeric) AND (user_vote_score <= (1)::numeric)))
);


--
-- TOC entry 4906 (class 0 OID 0)
-- Dependencies: 246
-- Name: TABLE trust_scores; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON TABLE flood_processor.trust_scores IS 'Bảng lưu trữ điểm tin cậy của các sự kiện ngập lụt';


--
-- TOC entry 4907 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.id IS 'UUID duy nhất của bản ghi điểm tin cậy';


--
-- TOC entry 4908 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.flood_event_id; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.flood_event_id IS 'Liên kết đến sự kiện ngập lụt được tính điểm';


--
-- TOC entry 4909 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.iot_score; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.iot_score IS 'Điểm tin cậy từ dữ liệu cảm biến IoT (0.0-1.0)';


--
-- TOC entry 4910 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.user_vote_score; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.user_vote_score IS 'Điểm tin cậy từ bình chọn của người dùng (0.0-1.0)';


--
-- TOC entry 4911 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.historical_score; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.historical_score IS 'Điểm tin cậy dựa trên lịch sử ngập của khu vực (0.0-1.0)';


--
-- TOC entry 4912 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.spatial_correlation_score; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.spatial_correlation_score IS 'Điểm tương quan không gian - so sánh với các vùng lân cận (0.0-1.0)';


--
-- TOC entry 4913 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.final_score; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.final_score IS 'Điểm tin cậy tổng hợp cuối cùng (0.0-1.0)';


--
-- TOC entry 4914 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.calculation_method; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.calculation_method IS 'Phương pháp tính: RULE_BASED (theo quy tắc), ML_MODEL (mô hình AI), WEIGHTED_AVG (trung bình có trọng số)';


--
-- TOC entry 4915 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.algorithm_version; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.algorithm_version IS 'Phiên bản thuật toán tính điểm (VD: v1.2.0)';


--
-- TOC entry 4916 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.factors; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.factors IS 'Các yếu tố ảnh hưởng đến điểm số dạng JSON';


--
-- TOC entry 4917 (class 0 OID 0)
-- Dependencies: 246
-- Name: COLUMN trust_scores.calculated_at; Type: COMMENT; Schema: flood_processor; Owner: -
--

COMMENT ON COLUMN flood_processor.trust_scores.calculated_at IS 'Thời điểm hệ thống tính toán điểm tin cậy';


--
-- TOC entry 247 (class 1259 OID 17779)
-- Name: notification_preferences; Type: TABLE; Schema: notification; Owner: -
--

CREATE TABLE notification.notification_preferences (
    user_id uuid NOT NULL,
    enabled boolean DEFAULT true,
    flood_alerts boolean DEFAULT true,
    flood_updates boolean DEFAULT true,
    system_updates boolean DEFAULT true,
    quiet_hours_enabled boolean DEFAULT false,
    quiet_hours_start time without time zone,
    quiet_hours_end time without time zone,
    alert_radius_meters integer DEFAULT 500,
    prefer_push boolean DEFAULT true,
    prefer_email boolean DEFAULT false,
    prefer_sms boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fcm_token text
);


--
-- TOC entry 248 (class 1259 OID 17795)
-- Name: notifications; Type: TABLE; Schema: notification; Owner: -
--

CREATE TABLE notification.notifications (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    body text NOT NULL,
    notification_type character varying(50),
    priority character varying(20) DEFAULT 'NORMAL'::character varying,
    data jsonb,
    channel character varying(20) DEFAULT 'PUSH'::character varying,
    fcm_token text,
    fcm_message_id character varying(255),
    email_to character varying(255),
    email_subject character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    error_message text,
    retry_count integer DEFAULT 0,
    max_retries integer DEFAULT 3,
    next_retry_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    sent_at timestamp without time zone,
    delivered_at timestamp without time zone,
    clicked_at timestamp without time zone
);


--
-- TOC entry 4465 (class 2606 OID 17808)
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 4470 (class 2606 OID 17810)
-- Name: fcm_tokens fcm_tokens_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.fcm_tokens
    ADD CONSTRAINT fcm_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 4472 (class 2606 OID 17812)
-- Name: fcm_tokens fcm_tokens_token_key; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.fcm_tokens
    ADD CONSTRAINT fcm_tokens_token_key UNIQUE (token);


--
-- TOC entry 4480 (class 2606 OID 17814)
-- Name: invalidated_tokens invalidated_tokens_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.invalidated_tokens
    ADD CONSTRAINT invalidated_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 4482 (class 2606 OID 17816)
-- Name: invalidated_tokens invalidated_tokens_token_jti_key; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.invalidated_tokens
    ADD CONSTRAINT invalidated_tokens_token_jti_key UNIQUE (token_jti);


--
-- TOC entry 4484 (class 2606 OID 17818)
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- TOC entry 4486 (class 2606 OID 17820)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- TOC entry 4490 (class 2606 OID 17822)
-- Name: user_addresses user_addresses_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_addresses
    ADD CONSTRAINT user_addresses_pkey PRIMARY KEY (id);


--
-- TOC entry 4492 (class 2606 OID 17824)
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 4494 (class 2606 OID 17826)
-- Name: user_profiles user_profiles_user_id_unique; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_profiles
    ADD CONSTRAINT user_profiles_user_id_unique UNIQUE (user_id);


--
-- TOC entry 4498 (class 2606 OID 17828)
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (id);


--
-- TOC entry 4500 (class 2606 OID 17830)
-- Name: user_roles user_roles_user_id_role_id_key; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_roles
    ADD CONSTRAINT user_roles_user_id_role_id_key UNIQUE (user_id, role_id);


--
-- TOC entry 4505 (class 2606 OID 17832)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 4507 (class 2606 OID 17834)
-- Name: users users_firebase_uid_key; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.users
    ADD CONSTRAINT users_firebase_uid_key UNIQUE (firebase_uid);


--
-- TOC entry 4509 (class 2606 OID 17836)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 4515 (class 2606 OID 17838)
-- Name: core_active_floods core_active_floods_pkey; Type: CONSTRAINT; Schema: flood_core; Owner: -
--

ALTER TABLE ONLY flood_core.core_active_floods
    ADD CONSTRAINT core_active_floods_pkey PRIMARY KEY (event_id);


--
-- TOC entry 4511 (class 2606 OID 17840)
-- Name: flood_zones flood_zones_pkey; Type: CONSTRAINT; Schema: flood_core; Owner: -
--

ALTER TABLE ONLY flood_core.flood_zones
    ADD CONSTRAINT flood_zones_pkey PRIMARY KEY (id);


--
-- TOC entry 4522 (class 2606 OID 17842)
-- Name: sensor_logs sensor_logs_pkey; Type: CONSTRAINT; Schema: flood_core; Owner: -
--

ALTER TABLE ONLY flood_core.sensor_logs
    ADD CONSTRAINT sensor_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 4528 (class 2606 OID 17844)
-- Name: sensors sensors_api_key_key; Type: CONSTRAINT; Schema: flood_core; Owner: -
--

ALTER TABLE ONLY flood_core.sensors
    ADD CONSTRAINT sensors_api_key_key UNIQUE (api_key);


--
-- TOC entry 4530 (class 2606 OID 17846)
-- Name: sensors sensors_pkey; Type: CONSTRAINT; Schema: flood_core; Owner: -
--

ALTER TABLE ONLY flood_core.sensors
    ADD CONSTRAINT sensors_pkey PRIMARY KEY (id);


--
-- TOC entry 4532 (class 2606 OID 17848)
-- Name: sensors sensors_sensor_id_key; Type: CONSTRAINT; Schema: flood_core; Owner: -
--

ALTER TABLE ONLY flood_core.sensors
    ADD CONSTRAINT sensors_sensor_id_key UNIQUE (sensor_id);


--
-- TOC entry 4539 (class 2606 OID 17850)
-- Name: user_reports user_reports_report_id_key; Type: CONSTRAINT; Schema: flood_core; Owner: -
--

ALTER TABLE ONLY flood_core.user_reports
    ADD CONSTRAINT user_reports_report_id_key UNIQUE (report_id);


--
-- TOC entry 4541 (class 2606 OID 17852)
-- Name: alert_triggers alert_triggers_pkey; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.alert_triggers
    ADD CONSTRAINT alert_triggers_pkey PRIMARY KEY (id);


--
-- TOC entry 4543 (class 2606 OID 17854)
-- Name: alert_triggers alert_triggers_trigger_id_key; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.alert_triggers
    ADD CONSTRAINT alert_triggers_trigger_id_key UNIQUE (trigger_id);


--
-- TOC entry 4549 (class 2606 OID 17856)
-- Name: event_contributors event_contributors_pkey; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.event_contributors
    ADD CONSTRAINT event_contributors_pkey PRIMARY KEY (id);


--
-- TOC entry 4553 (class 2606 OID 17858)
-- Name: flood_events flood_events_event_id_key; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.flood_events
    ADD CONSTRAINT flood_events_event_id_key UNIQUE (event_id);


--
-- TOC entry 4555 (class 2606 OID 17860)
-- Name: flood_events flood_events_pkey; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.flood_events
    ADD CONSTRAINT flood_events_pkey PRIMARY KEY (id);


--
-- TOC entry 4568 (class 2606 OID 17862)
-- Name: iot_readings iot_readings_pkey; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.iot_readings
    ADD CONSTRAINT iot_readings_pkey PRIMARY KEY (id);


--
-- TOC entry 4570 (class 2606 OID 17864)
-- Name: iot_readings iot_readings_reading_id_key; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.iot_readings
    ADD CONSTRAINT iot_readings_reading_id_key UNIQUE (reading_id);


--
-- TOC entry 4574 (class 2606 OID 17866)
-- Name: trust_scores trust_scores_pkey; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.trust_scores
    ADD CONSTRAINT trust_scores_pkey PRIMARY KEY (id);


--
-- TOC entry 4551 (class 2606 OID 17868)
-- Name: event_contributors unique_user_vote; Type: CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.event_contributors
    ADD CONSTRAINT unique_user_vote UNIQUE (flood_event_id, user_id);


--
-- TOC entry 4577 (class 2606 OID 17870)
-- Name: notification_preferences notification_preferences_pkey; Type: CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.notification_preferences
    ADD CONSTRAINT notification_preferences_pkey PRIMARY KEY (user_id);


--
-- TOC entry 4585 (class 2606 OID 17872)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 4466 (class 1259 OID 17873)
-- Name: idx_auth_audit_logs_action; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_audit_logs_action ON auth.audit_logs USING btree (action);


--
-- TOC entry 4467 (class 1259 OID 17874)
-- Name: idx_auth_audit_logs_created; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_audit_logs_created ON auth.audit_logs USING btree (created_at DESC);


--
-- TOC entry 4468 (class 1259 OID 17875)
-- Name: idx_auth_audit_logs_user; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_audit_logs_user ON auth.audit_logs USING btree (user_id);


--
-- TOC entry 4473 (class 1259 OID 17876)
-- Name: idx_auth_fcm_tokens_active; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_fcm_tokens_active ON auth.fcm_tokens USING btree (user_id, is_active) WHERE (is_active = true);


--
-- TOC entry 4474 (class 1259 OID 17877)
-- Name: idx_auth_fcm_tokens_token; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_fcm_tokens_token ON auth.fcm_tokens USING btree (token);


--
-- TOC entry 4475 (class 1259 OID 17878)
-- Name: idx_auth_fcm_tokens_user; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_fcm_tokens_user ON auth.fcm_tokens USING btree (user_id);


--
-- TOC entry 4476 (class 1259 OID 17879)
-- Name: idx_auth_invalidated_tokens_expires; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_invalidated_tokens_expires ON auth.invalidated_tokens USING btree (expires_at);


--
-- TOC entry 4477 (class 1259 OID 17880)
-- Name: idx_auth_invalidated_tokens_jti; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_invalidated_tokens_jti ON auth.invalidated_tokens USING btree (token_jti);


--
-- TOC entry 4478 (class 1259 OID 17881)
-- Name: idx_auth_invalidated_tokens_user; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_invalidated_tokens_user ON auth.invalidated_tokens USING btree (user_id);


--
-- TOC entry 4487 (class 1259 OID 17882)
-- Name: idx_auth_user_addresses_primary; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_user_addresses_primary ON auth.user_addresses USING btree (user_id, is_primary) WHERE (is_primary = true);


--
-- TOC entry 4488 (class 1259 OID 17883)
-- Name: idx_auth_user_addresses_user; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_user_addresses_user ON auth.user_addresses USING btree (user_id);


--
-- TOC entry 4495 (class 1259 OID 17884)
-- Name: idx_auth_user_roles_role; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_user_roles_role ON auth.user_roles USING btree (role_id);


--
-- TOC entry 4496 (class 1259 OID 17885)
-- Name: idx_auth_user_roles_user; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_user_roles_user ON auth.user_roles USING btree (user_id);


--
-- TOC entry 4501 (class 1259 OID 17886)
-- Name: idx_auth_users_created; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_users_created ON auth.users USING btree (created_at DESC);


--
-- TOC entry 4502 (class 1259 OID 17887)
-- Name: idx_auth_users_email; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_users_email ON auth.users USING btree (email);


--
-- TOC entry 4503 (class 1259 OID 17888)
-- Name: idx_auth_users_status; Type: INDEX; Schema: auth; Owner: -
--

CREATE INDEX idx_auth_users_status ON auth.users USING btree (status);


--
-- TOC entry 4516 (class 1259 OID 17889)
-- Name: idx_core_floods_location; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_core_floods_location ON flood_core.core_active_floods USING gist (public.ll_to_earth((lat)::double precision, (lon)::double precision));


--
-- TOC entry 4517 (class 1259 OID 17890)
-- Name: idx_core_floods_status; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_core_floods_status ON flood_core.core_active_floods USING btree (status);


--
-- TOC entry 4518 (class 1259 OID 17891)
-- Name: idx_flood_core_sensor_logs_action; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_sensor_logs_action ON flood_core.sensor_logs USING btree (action);


--
-- TOC entry 4519 (class 1259 OID 17892)
-- Name: idx_flood_core_sensor_logs_created; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_sensor_logs_created ON flood_core.sensor_logs USING btree (created_at DESC);


--
-- TOC entry 4520 (class 1259 OID 17893)
-- Name: idx_flood_core_sensor_logs_sensor; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_sensor_logs_sensor ON flood_core.sensor_logs USING btree (sensor_id);


--
-- TOC entry 4523 (class 1259 OID 17894)
-- Name: idx_flood_core_sensors_api_key; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_sensors_api_key ON flood_core.sensors USING btree (api_key);


--
-- TOC entry 4524 (class 1259 OID 17895)
-- Name: idx_flood_core_sensors_heartbeat; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_sensors_heartbeat ON flood_core.sensors USING btree (last_heartbeat DESC);


--
-- TOC entry 4525 (class 1259 OID 17896)
-- Name: idx_flood_core_sensors_sensor_id; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_sensors_sensor_id ON flood_core.sensors USING btree (sensor_id);


--
-- TOC entry 4526 (class 1259 OID 17897)
-- Name: idx_flood_core_sensors_status; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_sensors_status ON flood_core.sensors USING btree (status);


--
-- TOC entry 4512 (class 1259 OID 17898)
-- Name: idx_flood_core_zones_active; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_zones_active ON flood_core.flood_zones USING btree (is_active);


--
-- TOC entry 4513 (class 1259 OID 17899)
-- Name: idx_flood_core_zones_pinned; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_core_zones_pinned ON flood_core.flood_zones USING btree (is_pinned) WHERE (is_pinned = true);


--
-- TOC entry 4533 (class 1259 OID 17900)
-- Name: idx_flood_proc_reports_created; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_proc_reports_created ON flood_core.user_reports USING btree (created_at DESC);


--
-- TOC entry 4534 (class 1259 OID 18009)
-- Name: idx_flood_proc_reports_event; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_proc_reports_event ON flood_core.user_reports USING btree (flood_event_id);


--
-- TOC entry 4535 (class 1259 OID 17902)
-- Name: idx_flood_proc_reports_report_id; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_proc_reports_report_id ON flood_core.user_reports USING btree (report_id);


--
-- TOC entry 4536 (class 1259 OID 17903)
-- Name: idx_flood_proc_reports_status; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_proc_reports_status ON flood_core.user_reports USING btree (status);


--
-- TOC entry 4537 (class 1259 OID 17904)
-- Name: idx_flood_proc_reports_user; Type: INDEX; Schema: flood_core; Owner: -
--

CREATE INDEX idx_flood_proc_reports_user ON flood_core.user_reports USING btree (user_id);


--
-- TOC entry 4556 (class 1259 OID 17905)
-- Name: idx_flood_proc_events_created; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_events_created ON flood_processor.flood_events USING btree (created_at DESC);


--
-- TOC entry 4557 (class 1259 OID 17906)
-- Name: idx_flood_proc_events_event_id; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_events_event_id ON flood_processor.flood_events USING btree (event_id);


--
-- TOC entry 4558 (class 1259 OID 17907)
-- Name: idx_flood_proc_events_expires; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_events_expires ON flood_processor.flood_events USING btree (expires_at) WHERE (expires_at IS NOT NULL);


--
-- TOC entry 4559 (class 1259 OID 17908)
-- Name: idx_flood_proc_events_severity; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_events_severity ON flood_processor.flood_events USING btree (severity_level);


--
-- TOC entry 4560 (class 1259 OID 17909)
-- Name: idx_flood_proc_events_source; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_events_source ON flood_processor.flood_events USING btree (source);


--
-- TOC entry 4561 (class 1259 OID 17910)
-- Name: idx_flood_proc_events_status; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_events_status ON flood_processor.flood_events USING btree (status);


--
-- TOC entry 4562 (class 1259 OID 17911)
-- Name: idx_flood_proc_readings_event; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_readings_event ON flood_processor.iot_readings USING btree (flood_event_id);


--
-- TOC entry 4563 (class 1259 OID 17912)
-- Name: idx_flood_proc_readings_measured; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_readings_measured ON flood_processor.iot_readings USING btree (measured_at DESC);


--
-- TOC entry 4564 (class 1259 OID 17913)
-- Name: idx_flood_proc_readings_reading_id; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_readings_reading_id ON flood_processor.iot_readings USING btree (reading_id);


--
-- TOC entry 4565 (class 1259 OID 17914)
-- Name: idx_flood_proc_readings_sensor; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_readings_sensor ON flood_processor.iot_readings USING btree (sensor_id);


--
-- TOC entry 4544 (class 1259 OID 17915)
-- Name: idx_flood_proc_triggers_event; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_triggers_event ON flood_processor.alert_triggers USING btree (flood_event_id);


--
-- TOC entry 4545 (class 1259 OID 17916)
-- Name: idx_flood_proc_triggers_time; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_triggers_time ON flood_processor.alert_triggers USING btree (triggered_at DESC);


--
-- TOC entry 4546 (class 1259 OID 17917)
-- Name: idx_flood_proc_triggers_trigger_id; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_triggers_trigger_id ON flood_processor.alert_triggers USING btree (trigger_id);


--
-- TOC entry 4547 (class 1259 OID 17918)
-- Name: idx_flood_proc_triggers_type; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_triggers_type ON flood_processor.alert_triggers USING btree (trigger_type);


--
-- TOC entry 4571 (class 1259 OID 17919)
-- Name: idx_flood_proc_trust_event; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_trust_event ON flood_processor.trust_scores USING btree (flood_event_id);


--
-- TOC entry 4572 (class 1259 OID 17920)
-- Name: idx_flood_proc_trust_score; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_flood_proc_trust_score ON flood_processor.trust_scores USING btree (final_score DESC);


--
-- TOC entry 4566 (class 1259 OID 17921)
-- Name: idx_iot_readings_status; Type: INDEX; Schema: flood_processor; Owner: -
--

CREATE INDEX idx_iot_readings_status ON flood_processor.iot_readings USING btree (status);


--
-- TOC entry 4578 (class 1259 OID 17922)
-- Name: idx_notif_channel; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notif_channel ON notification.notifications USING btree (channel);


--
-- TOC entry 4579 (class 1259 OID 17923)
-- Name: idx_notif_created; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notif_created ON notification.notifications USING btree (created_at DESC);


--
-- TOC entry 4575 (class 1259 OID 17924)
-- Name: idx_notif_prefs_enabled; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notif_prefs_enabled ON notification.notification_preferences USING btree (enabled) WHERE (enabled = true);


--
-- TOC entry 4580 (class 1259 OID 17925)
-- Name: idx_notif_retry; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notif_retry ON notification.notifications USING btree (next_retry_at) WHERE (((status)::text = 'FAILED'::text) AND (retry_count < max_retries));


--
-- TOC entry 4581 (class 1259 OID 17926)
-- Name: idx_notif_status; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notif_status ON notification.notifications USING btree (status);


--
-- TOC entry 4582 (class 1259 OID 17927)
-- Name: idx_notif_type; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notif_type ON notification.notifications USING btree (notification_type);


--
-- TOC entry 4583 (class 1259 OID 17928)
-- Name: idx_notif_user; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notif_user ON notification.notifications USING btree (user_id);


--
-- TOC entry 4596 (class 2620 OID 17929)
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: auth; Owner: -
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON auth.users FOR EACH ROW EXECUTE FUNCTION auth.update_updated_at_column();


--
-- TOC entry 4586 (class 2606 OID 17930)
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id);


--
-- TOC entry 4587 (class 2606 OID 17935)
-- Name: fcm_tokens fcm_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.fcm_tokens
    ADD CONSTRAINT fcm_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 4590 (class 2606 OID 17940)
-- Name: user_profiles fk_user_profile_user; Type: FK CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_profiles
    ADD CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES auth.users(id);


--
-- TOC entry 4588 (class 2606 OID 17945)
-- Name: invalidated_tokens invalidated_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.invalidated_tokens
    ADD CONSTRAINT invalidated_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 4589 (class 2606 OID 17950)
-- Name: user_addresses user_addresses_user_id_fkey; Type: FK CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_addresses
    ADD CONSTRAINT user_addresses_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 4591 (class 2606 OID 17955)
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES auth.roles(id) ON DELETE CASCADE;


--
-- TOC entry 4592 (class 2606 OID 17960)
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: auth; Owner: -
--

ALTER TABLE ONLY auth.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 4593 (class 2606 OID 17965)
-- Name: alert_triggers alert_triggers_flood_event_id_fkey; Type: FK CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.alert_triggers
    ADD CONSTRAINT alert_triggers_flood_event_id_fkey FOREIGN KEY (flood_event_id) REFERENCES flood_processor.flood_events(id);


--
-- TOC entry 4594 (class 2606 OID 17975)
-- Name: iot_readings iot_readings_flood_event_id_fkey; Type: FK CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.iot_readings
    ADD CONSTRAINT iot_readings_flood_event_id_fkey FOREIGN KEY (flood_event_id) REFERENCES flood_processor.flood_events(id);


--
-- TOC entry 4595 (class 2606 OID 17980)
-- Name: trust_scores trust_scores_flood_event_id_fkey; Type: FK CONSTRAINT; Schema: flood_processor; Owner: -
--

ALTER TABLE ONLY flood_processor.trust_scores
    ADD CONSTRAINT trust_scores_flood_event_id_fkey FOREIGN KEY (flood_event_id) REFERENCES flood_processor.flood_events(id) ON DELETE CASCADE;


-- Completed on 2026-06-24 11:58:16

--
-- PostgreSQL database dump complete
--

