create table posts (
     id bigserial primary key,
     caption varchar(300),
     status varchar(30) not null,
     video_key varchar(255),
     created_at timestamptz not null default now()
 );