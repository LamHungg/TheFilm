ALTER TABLE app_user ADD email_verified CHAR(1 CHAR) DEFAULT 'N' NOT NULL;

ALTER TABLE app_user ADD CONSTRAINT ck_app_user_email_verified
    CHECK (email_verified IN ('Y', 'N'));

CREATE TABLE email_verification_challenge (
    app_user_id NUMBER(19) NOT NULL,
    code_hash   VARCHAR2(255 CHAR) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts    NUMBER(2) DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_email_verification_challenge PRIMARY KEY (app_user_id),
    CONSTRAINT fk_email_verification_user FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id),
    CONSTRAINT ck_email_verification_attempts CHECK (attempts BETWEEN 0 AND 5)
);
