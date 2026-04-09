CREATE TYPE user_type AS ENUM ('COMMON', 'MERCHANT');

CREATE TABLE users (
   id           bigserial       PRIMARY KEY,
   external_id  uuid            NOT NULL UNIQUE,
   full_name    varchar(150)    NOT NULL,
   document     varchar(50)     NOT NULL UNIQUE,
   email        varchar(50)     NOT NULL UNIQUE,
   password     varchar(150)    NOT NULL,
   type         user_type       NOT NULL,
   balance      numeric(19,2)   NOT NULL,
   created_at   timestamp       NOT NULL,
   updated_at   timestamp       NOT NULL,
   CONSTRAINT   BALANCE_NONNEGATIVE CHECK (balance >= 0)
);
