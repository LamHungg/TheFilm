CREATE TABLE app_role (
    role_code   VARCHAR2(40 CHAR) NOT NULL,
    role_name   VARCHAR2(120 CHAR) NOT NULL,
    CONSTRAINT pk_app_role PRIMARY KEY (role_code)
);

CREATE TABLE app_user_role (
    app_user_id NUMBER(19) NOT NULL,
    role_code   VARCHAR2(40 CHAR) NOT NULL,
    granted_by  NUMBER(19),
    granted_at  TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_app_user_role PRIMARY KEY (app_user_id, role_code),
    CONSTRAINT fk_app_user_role_user FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id),
    CONSTRAINT fk_app_user_role_role FOREIGN KEY (role_code) REFERENCES app_role (role_code),
    CONSTRAINT fk_app_user_role_granter FOREIGN KEY (granted_by) REFERENCES app_user (app_user_id)
);

CREATE INDEX ix_app_user_role_code ON app_user_role (role_code, app_user_id);

INSERT INTO app_role (role_code, role_name) VALUES ('CUSTOMER', 'Khách hàng');
INSERT INTO app_role (role_code, role_name) VALUES ('CHAIN_ADMIN', 'Quản trị chuỗi rạp');
INSERT INTO app_role (role_code, role_name) VALUES ('CINEMA_MANAGER', 'Quản lý rạp');
INSERT INTO app_role (role_code, role_name) VALUES ('HR', 'Nhân sự');
INSERT INTO app_role (role_code, role_name) VALUES ('CASHIER', 'Thu ngân');
INSERT INTO app_role (role_code, role_name) VALUES ('USHER', 'Soát vé');
