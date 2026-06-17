
    create table customer (
        birthday date,
        deleted boolean default false not null,
        gender integer,
        gift_level integer,
        registered_at date,
        type integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        gift_received_at datetime(6),
        id bigint not null auto_increment,
        referrer_id bigint,
        status_updated_time datetime(6),
        phone varchar(20),
        name varchar(50) not null,
        email varchar(100),
        address varchar(200),
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table data_dict (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        group_code varchar(50) not null,
        group_name varchar(50) not null,
        param_code varchar(50) not null,
        param_name varchar(50) not null,
        param_value varchar(50),
        content_updated_by varchar(255),
        created_by varchar(255),
        description varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gift (
        deleted boolean default false not null,
        limit_enabled bit not null,
        limit_per_person integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        product_id bigint,
        status_updated_time datetime(6),
        code varchar(50) not null,
        name varchar(100) not null,
        description varchar(200),
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        type enum ('COUPON','NEW','PHYSICAL','POINTS','VIRTUAL') not null,
        primary key (id)
    ) engine=InnoDB;

    create table gift_log (
        deleted boolean default false not null,
        quantity integer not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        customer_id bigint not null,
        gift_id bigint not null,
        id bigint not null auto_increment,
        issue_at datetime(6),
        status_updated_time datetime(6),
        issue_notes varchar(50) comment '处理说明',
        operator varchar(50),
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table inventory_log (
        after_stock integer,
        before_stock integer,
        deleted boolean default false not null,
        quantity integer not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        product_id bigint not null,
        status_updated_time datetime(6),
        operator varchar(50),
        reason varchar(200),
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        type enum ('ADJUST','CREATE','IN','OUT','PARAM') not null,
        primary key (id)
    ) engine=InnoDB;

    create table operation_log (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        execution_time bigint,
        id bigint not null auto_increment,
        operation_time datetime(6),
        status_updated_time datetime(6),
        request_method varchar(10),
        ip_address varchar(50),
        module varchar(50),
        operator varchar(50),
        description varchar(500),
        request_url varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        error_message TEXT,
        request_params TEXT,
        status varchar(255),
        status_updated_by varchar(255),
        operation_type enum ('BOTH_UPDATE','CONTENT_UPDATE','CREATE','DELETE','OTHER','STATUS_UPDATE'),
        primary key (id)
    ) engine=InnoDB;

    create table product (
        current_stock integer,
        deleted boolean default false not null,
        price decimal(10,2),
        safe_stock integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        unit varchar(20),
        category varchar(50),
        code varchar(50),
        name varchar(100) not null,
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        description TEXT,
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table sys_user (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        last_login_at datetime(6),
        status_updated_time datetime(6),
        role varchar(20),
        email varchar(50),
        username varchar(50) not null,
        password varchar(100) not null,
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table system_config (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        config_group varchar(50),
        config_key varchar(100) not null,
        description varchar(200),
        config_value TEXT,
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    alter table customer 
       add constraint uk_phone_deleted unique (phone, deleted);

    create index idx_group_code 
       on data_dict (group_code);

    create index idx_param_code 
       on data_dict (param_code);

    create index idx_group_param_code 
       on data_dict (group_code, param_code);

    alter table data_dict 
       add constraint uk_group_param_code unique (group_code, param_code);

    create index idx_gift_name 
       on gift (name);

    create index idx_gift_code 
       on gift (code);

    create index idx_gift_status 
       on gift (gift_status);

    create index idx_gift_type 
       on gift (type);

    alter table gift 
       add constraint uk_gift_code unique (code);

    create index idx_gift_id 
       on gift_log (gift_id);

    create index idx_gift_log_status 
       on gift_log (status);

    create index idx_gift_log_customer 
       on gift_log (customer_id);

    alter table product 
       add constraint UKh3w5r1mx6d0e5c6um32dgyjej unique (code);

    alter table sys_user 
       add constraint UK51bvuyvihefoh4kp5syh2jpi4 unique (username);

    alter table system_config 
       add constraint UKnpsxm1erd0lbetjn5d3ayrsof unique (config_key);

    alter table customer 
       add constraint FKqimtoyywmdrafcpsxxl6rotnb 
       foreign key (referrer_id) 
       references customer (id);

    alter table gift 
       add constraint FK8lyja0ys3qg2kcb8sqokx9dib 
       foreign key (product_id) 
       references product (id);

    alter table gift_log 
       add constraint FKrbecj9w8a8ssdhs68m1yx09rq 
       foreign key (customer_id) 
       references customer (id);

    alter table gift_log 
       add constraint FK1u3bdbaggiaop9tjemqed6wsp 
       foreign key (gift_id) 
       references gift (id);

    alter table inventory_log 
       add constraint FKbqa2xa68wnb9cnbjdsxqqn0n0 
       foreign key (product_id) 
       references product (id);

    create table customer (
        birthday date,
        deleted boolean default false not null,
        gender integer,
        gift_level integer,
        registered_at date,
        type integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        gift_received_at datetime(6),
        id bigint not null auto_increment,
        referrer_id bigint,
        status_updated_time datetime(6),
        phone varchar(20),
        name varchar(50) not null,
        email varchar(100),
        address varchar(200),
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table data_dict (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        group_code varchar(50) not null,
        group_name varchar(50) not null,
        param_code varchar(50) not null,
        param_name varchar(50) not null,
        param_value varchar(50),
        content_updated_by varchar(255),
        created_by varchar(255),
        description varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gift (
        deleted boolean default false not null,
        limit_enabled bit not null,
        limit_per_person integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        product_id bigint,
        status_updated_time datetime(6),
        code varchar(50) not null,
        name varchar(100) not null,
        description varchar(200),
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        type enum ('COUPON','NEW','PHYSICAL','POINTS','VIRTUAL') not null,
        primary key (id)
    ) engine=InnoDB;

    create table gift_log (
        deleted boolean default false not null,
        quantity integer not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        customer_id bigint not null,
        gift_id bigint not null,
        id bigint not null auto_increment,
        issue_at datetime(6),
        status_updated_time datetime(6),
        issue_notes varchar(50) comment '处理说明',
        operator varchar(50),
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table inventory_log (
        after_stock integer,
        before_stock integer,
        deleted boolean default false not null,
        quantity integer not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        product_id bigint not null,
        status_updated_time datetime(6),
        operator varchar(50),
        reason varchar(200),
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        type enum ('ADJUST','CREATE','IN','OUT','PARAM') not null,
        primary key (id)
    ) engine=InnoDB;

    create table operation_log (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        execution_time bigint,
        id bigint not null auto_increment,
        operation_time datetime(6),
        status_updated_time datetime(6),
        request_method varchar(10),
        ip_address varchar(50),
        module varchar(50),
        operator varchar(50),
        description varchar(500),
        request_url varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        error_message TEXT,
        request_params TEXT,
        status varchar(255),
        status_updated_by varchar(255),
        operation_type enum ('BOTH_UPDATE','CONTENT_UPDATE','CREATE','DELETE','OTHER','STATUS_UPDATE'),
        primary key (id)
    ) engine=InnoDB;

    create table product (
        current_stock integer,
        deleted boolean default false not null,
        price decimal(10,2),
        safe_stock integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        unit varchar(20),
        category varchar(50),
        code varchar(50),
        name varchar(100) not null,
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        description TEXT,
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table sys_user (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        last_login_at datetime(6),
        status_updated_time datetime(6),
        role varchar(20),
        email varchar(50),
        username varchar(50) not null,
        password varchar(100) not null,
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table system_config (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        config_group varchar(50),
        config_key varchar(100) not null,
        description varchar(200),
        config_value TEXT,
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    alter table customer 
       add constraint uk_phone_deleted unique (phone, deleted);

    create index idx_group_code 
       on data_dict (group_code);

    create index idx_param_code 
       on data_dict (param_code);

    create index idx_group_param_code 
       on data_dict (group_code, param_code);

    alter table data_dict 
       add constraint uk_group_param_code unique (group_code, param_code);

    create index idx_gift_name 
       on gift (name);

    create index idx_gift_code 
       on gift (code);

    create index idx_gift_status 
       on gift (gift_status);

    create index idx_gift_type 
       on gift (type);

    alter table gift 
       add constraint uk_gift_code unique (code);

    create index idx_gift_id 
       on gift_log (gift_id);

    create index idx_gift_log_status 
       on gift_log (status);

    create index idx_gift_log_customer 
       on gift_log (customer_id);

    alter table product 
       add constraint UKh3w5r1mx6d0e5c6um32dgyjej unique (code);

    alter table sys_user 
       add constraint UK51bvuyvihefoh4kp5syh2jpi4 unique (username);

    alter table system_config 
       add constraint UKnpsxm1erd0lbetjn5d3ayrsof unique (config_key);

    alter table customer 
       add constraint FKqimtoyywmdrafcpsxxl6rotnb 
       foreign key (referrer_id) 
       references customer (id);

    alter table gift 
       add constraint FK8lyja0ys3qg2kcb8sqokx9dib 
       foreign key (product_id) 
       references product (id);

    alter table gift_log 
       add constraint FKrbecj9w8a8ssdhs68m1yx09rq 
       foreign key (customer_id) 
       references customer (id);

    alter table gift_log 
       add constraint FK1u3bdbaggiaop9tjemqed6wsp 
       foreign key (gift_id) 
       references gift (id);

    alter table inventory_log 
       add constraint FKbqa2xa68wnb9cnbjdsxqqn0n0 
       foreign key (product_id) 
       references product (id);

    create table customer (
        birthday date,
        deleted boolean default false not null,
        gender integer,
        gift_level integer,
        registered_at date,
        type integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        gift_received_at datetime(6),
        id bigint not null auto_increment,
        referrer_id bigint,
        status_updated_time datetime(6),
        phone varchar(20),
        name varchar(50) not null,
        email varchar(100),
        address varchar(200),
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table data_dict (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        group_code varchar(50) not null,
        group_name varchar(50) not null,
        param_code varchar(50) not null,
        param_name varchar(50) not null,
        param_value varchar(50),
        content_updated_by varchar(255),
        created_by varchar(255),
        description varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gift (
        deleted boolean default false not null,
        limit_enabled bit not null,
        limit_per_person integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        product_id bigint,
        status_updated_time datetime(6),
        code varchar(50) not null,
        name varchar(100) not null,
        description varchar(200),
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        type enum ('COUPON','NEW','PHYSICAL','POINTS','VIRTUAL') not null,
        primary key (id)
    ) engine=InnoDB;

    create table gift_log (
        deleted boolean default false not null,
        quantity integer not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        customer_id bigint not null,
        gift_id bigint not null,
        id bigint not null auto_increment,
        issue_at datetime(6),
        status_updated_time datetime(6),
        issue_notes varchar(50) comment '处理说明',
        operator varchar(50),
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table inventory_log (
        after_stock integer,
        before_stock integer,
        deleted boolean default false not null,
        quantity integer not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        product_id bigint not null,
        status_updated_time datetime(6),
        operator varchar(50),
        reason varchar(200),
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        type enum ('ADJUST','CREATE','IN','OUT','PARAM') not null,
        primary key (id)
    ) engine=InnoDB;

    create table operation_log (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        execution_time bigint,
        id bigint not null auto_increment,
        operation_time datetime(6),
        status_updated_time datetime(6),
        request_method varchar(10),
        ip_address varchar(50),
        module varchar(50),
        operator varchar(50),
        description varchar(500),
        request_url varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        error_message TEXT,
        request_params TEXT,
        status varchar(255),
        status_updated_by varchar(255),
        operation_type enum ('BOTH_UPDATE','CONTENT_UPDATE','CREATE','DELETE','OTHER','STATUS_UPDATE'),
        primary key (id)
    ) engine=InnoDB;

    create table product (
        current_stock integer,
        deleted boolean default false not null,
        price decimal(10,2),
        safe_stock integer,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        unit varchar(20),
        category varchar(50),
        code varchar(50),
        name varchar(100) not null,
        remark varchar(500),
        content_updated_by varchar(255),
        created_by varchar(255),
        description TEXT,
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table sys_user (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        last_login_at datetime(6),
        status_updated_time datetime(6),
        role varchar(20),
        email varchar(50),
        username varchar(50) not null,
        password varchar(100) not null,
        remark varchar(200),
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table system_config (
        deleted boolean default false not null,
        content_updated_time datetime(6),
        created_time datetime(6),
        id bigint not null auto_increment,
        status_updated_time datetime(6),
        config_group varchar(50),
        config_key varchar(100) not null,
        description varchar(200),
        config_value TEXT,
        content_updated_by varchar(255),
        created_by varchar(255),
        status varchar(255),
        status_updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    alter table customer 
       add constraint uk_phone_deleted unique (phone, deleted);

    create index idx_group_code 
       on data_dict (group_code);

    create index idx_param_code 
       on data_dict (param_code);

    create index idx_group_param_code 
       on data_dict (group_code, param_code);

    alter table data_dict 
       add constraint uk_group_param_code unique (group_code, param_code);

    create index idx_gift_name 
       on gift (name);

    create index idx_gift_code 
       on gift (code);

    create index idx_gift_status 
       on gift (gift_status);

    create index idx_gift_type 
       on gift (type);

    alter table gift 
       add constraint uk_gift_code unique (code);

    create index idx_gift_id 
       on gift_log (gift_id);

    create index idx_gift_log_status 
       on gift_log (status);

    create index idx_gift_log_customer 
       on gift_log (customer_id);

    alter table product 
       add constraint UKh3w5r1mx6d0e5c6um32dgyjej unique (code);

    alter table sys_user 
       add constraint UK51bvuyvihefoh4kp5syh2jpi4 unique (username);

    alter table system_config 
       add constraint UKnpsxm1erd0lbetjn5d3ayrsof unique (config_key);

    alter table customer 
       add constraint FKqimtoyywmdrafcpsxxl6rotnb 
       foreign key (referrer_id) 
       references customer (id);

    alter table gift 
       add constraint FK8lyja0ys3qg2kcb8sqokx9dib 
       foreign key (product_id) 
       references product (id);

    alter table gift_log 
       add constraint FKrbecj9w8a8ssdhs68m1yx09rq 
       foreign key (customer_id) 
       references customer (id);

    alter table gift_log 
       add constraint FK1u3bdbaggiaop9tjemqed6wsp 
       foreign key (gift_id) 
       references gift (id);

    alter table inventory_log 
       add constraint FKbqa2xa68wnb9cnbjdsxqqn0n0 
       foreign key (product_id) 
       references product (id);
