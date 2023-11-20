create table author
(
    id            serial primary key,
    fio           text      not null,
    creation_date timestamp not null
);

alter table budget add column author_id int;

alter table budget
add constraint fk_author
foreign key (author_id)
references author(id);
