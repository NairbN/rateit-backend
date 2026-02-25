create table view_events (
    id bigserial primary key,
    post_id bigint not null references posts(id),
    viewed_at timestamptz not null default now()
);