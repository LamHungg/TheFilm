CREATE TABLE admin_module_catalog (
    module_code   VARCHAR2(40 CHAR) NOT NULL,
    module_name   VARCHAR2(120 CHAR) NOT NULL,
    description   VARCHAR2(500 CHAR),
    sort_order    NUMBER(4) DEFAULT 0 NOT NULL,
    status        VARCHAR2(20 CHAR) DEFAULT 'ACTIVE' NOT NULL,
    CONSTRAINT pk_admin_module_catalog PRIMARY KEY (module_code),
    CONSTRAINT ck_admin_module_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('OVERVIEW', 'Tổng quan', 'Xem số liệu và tình hình vận hành chung.', 10);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('ORGANIZATIONS', 'Tổ chức và rạp', 'Quản lý chuỗi, pháp nhân và rạp chiếu.', 20);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('MOVIES', 'Phim', 'Quản lý danh mục phim, nội dung và duyệt phim.', 30);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('ROOMS', 'Phòng và ghế', 'Quản lý phòng chiếu, loại phòng và sơ đồ ghế.', 40);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('SHOWTIMES', 'Lịch chiếu', 'Quản lý suất chiếu và lịch hoạt động rạp.', 50);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('PRICING', 'Giá và ưu đãi', 'Quản lý quy tắc giá và chương trình ưu đãi.', 60);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('TICKETING', 'Bán vé', 'Tra cứu, bán và kiểm soát vé.', 70);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('FNB', 'Đồ ăn và kho', 'Quản lý hàng hóa, tồn kho và bán đồ ăn.', 80);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('STAFF', 'Nhân sự', 'Quản lý hồ sơ, phân công và quyền nhân viên.', 90);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('CRM', 'Chăm sóc khách hàng', 'Quản lý khách hàng và xử lý yêu cầu hỗ trợ.', 100);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('TAX_INVOICES', 'Thuế và hóa đơn', 'Tra cứu và xử lý hóa đơn, dữ liệu thuế.', 110);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('REPORTS', 'Báo cáo', 'Xem và xuất báo cáo vận hành.', 120);
INSERT INTO admin_module_catalog (module_code, module_name, description, sort_order) VALUES ('SYSTEM', 'Hệ thống', 'Quản lý cấu hình và hoạt động hệ thống.', 130);

CREATE TABLE department_module_access (
    department_id NUMBER(19) NOT NULL,
    module_code   VARCHAR2(40 CHAR) NOT NULL,
    granted_by    NUMBER(19),
    granted_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_department_module_access PRIMARY KEY (department_id, module_code),
    CONSTRAINT fk_dept_module_department FOREIGN KEY (department_id) REFERENCES department (department_id),
    CONSTRAINT fk_dept_module_catalog FOREIGN KEY (module_code) REFERENCES admin_module_catalog (module_code),
    CONSTRAINT fk_dept_module_grantor FOREIGN KEY (granted_by) REFERENCES app_user (app_user_id)
);

CREATE INDEX ix_dept_module_access_module ON department_module_access (module_code, department_id);
