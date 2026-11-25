CREATE TABLE staff_account (
    staff_account_id NUMBER(19) NOT NULL,
    employee_id      NUMBER(19),
    login_email      VARCHAR2(320 CHAR) NOT NULL,
    display_name     VARCHAR2(200 CHAR) NOT NULL,
    account_status   VARCHAR2(20 CHAR) DEFAULT 'ACTIVE' NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_staff_account PRIMARY KEY (staff_account_id),
    CONSTRAINT fk_staff_account_actor FOREIGN KEY (staff_account_id) REFERENCES app_user (app_user_id),
    CONSTRAINT uq_staff_account_employee UNIQUE (employee_id),
    CONSTRAINT fk_staff_account_employee FOREIGN KEY (employee_id) REFERENCES employee (employee_id),
    CONSTRAINT ck_staff_account_status CHECK (account_status IN ('ACTIVE', 'BLOCKED'))
);

CREATE UNIQUE INDEX ux_staff_account_login_email ON staff_account (LOWER(login_email));

CREATE TABLE staff_credential (
    staff_account_id NUMBER(19) NOT NULL,
    password_hash    VARCHAR2(255 CHAR) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_staff_credential PRIMARY KEY (staff_account_id),
    CONSTRAINT fk_staff_credential_account FOREIGN KEY (staff_account_id) REFERENCES staff_account (staff_account_id)
);

CREATE TABLE staff_account_role (
    staff_account_id NUMBER(19) NOT NULL,
    role_code        VARCHAR2(40 CHAR) NOT NULL,
    granted_by       NUMBER(19),
    granted_at       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_staff_account_role PRIMARY KEY (staff_account_id, role_code),
    CONSTRAINT fk_staff_role_account FOREIGN KEY (staff_account_id) REFERENCES staff_account (staff_account_id),
    CONSTRAINT fk_staff_role_role FOREIGN KEY (role_code) REFERENCES app_role (role_code),
    CONSTRAINT fk_staff_role_granter FOREIGN KEY (granted_by) REFERENCES app_user (app_user_id)
);

CREATE INDEX ix_staff_account_role_code ON staff_account_role (role_code, staff_account_id);

-- Create a separate employee identity for each legacy internal login. If the
-- legacy login also has a customer profile, retain its customer credential and
-- profile; staff receives a copy of the existing password hash and permissions.
DECLARE
    v_staff_user_id    NUMBER(19);
    v_employee_id      NUMBER(19);
    v_customer_profile NUMBER;
BEGIN
    FOR legacy_account IN (
        SELECT u.app_user_id,
               LOWER(TRIM(u.email)) AS login_email,
               NVL(u.display_name, u.email) AS display_name,
               u.status,
               c.password_hash
          FROM app_user u
          JOIN local_credential c ON c.app_user_id = u.app_user_id
         WHERE u.email IS NOT NULL
           AND EXISTS (
               SELECT 1 FROM app_user_role r
                WHERE r.app_user_id = u.app_user_id
                  AND r.role_code IN ('CHAIN_ADMIN', 'CINEMA_MANAGER', 'HR', 'CASHIER', 'USHER')
           )
    ) LOOP
        SELECT MIN(employee_id)
          INTO v_employee_id
          FROM employee
         WHERE app_user_id = legacy_account.app_user_id;

        INSERT INTO app_user (oidc_issuer, oidc_subject, display_name, email, phone, status)
        VALUES ('http://localhost/internal-staff', 'legacy-staff:' || legacy_account.app_user_id,
                'Tài khoản nội bộ', NULL, NULL,
                CASE WHEN legacy_account.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'BLOCKED' END)
        RETURNING app_user_id INTO v_staff_user_id;

        INSERT INTO staff_account (staff_account_id, employee_id, login_email, display_name, account_status)
        VALUES (v_staff_user_id, v_employee_id, legacy_account.login_email,
                legacy_account.display_name,
                CASE WHEN legacy_account.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'BLOCKED' END);

        INSERT INTO staff_credential (staff_account_id, password_hash)
        VALUES (v_staff_user_id, legacy_account.password_hash);

        INSERT INTO staff_account_role (staff_account_id, role_code, granted_by)
        SELECT v_staff_user_id, role_code, granted_by
          FROM app_user_role
         WHERE app_user_id = legacy_account.app_user_id
           AND role_code IN ('CHAIN_ADMIN', 'CINEMA_MANAGER', 'HR', 'CASHIER', 'USHER');

        UPDATE employee
           SET app_user_id = v_staff_user_id
         WHERE app_user_id = legacy_account.app_user_id;

        DELETE FROM app_user_role
         WHERE app_user_id = legacy_account.app_user_id
           AND role_code IN ('CHAIN_ADMIN', 'CINEMA_MANAGER', 'HR', 'CASHIER', 'USHER');

        SELECT COUNT(*)
          INTO v_customer_profile
          FROM customer_profile
         WHERE app_user_id = legacy_account.app_user_id;

        IF v_customer_profile = 0 THEN
            DELETE FROM local_credential WHERE app_user_id = legacy_account.app_user_id;
            UPDATE app_user
               SET email = NULL, phone = NULL, display_name = 'Tài khoản khách hàng đã tách'
             WHERE app_user_id = legacy_account.app_user_id;
        END IF;
    END LOOP;
    COMMIT;
END;
/
