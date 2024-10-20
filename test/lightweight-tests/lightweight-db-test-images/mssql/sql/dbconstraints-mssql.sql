CREATE DATABASE [dbconstraints];
GO
USE [dbconstraints]
GO

CREATE TABLE Book (
	bk_code int NOT NULL,
	bk_title varchar(100) NOT NULL
);

CREATE TABLE BookWriter (
	bk_code int NOT NULL,
	wr_code int NOT NULL
);

CREATE TABLE Edition (
	ed_code int NOT NULL,
	ed_year int NULL,
	bk_code int NULL
);

CREATE TABLE Writer (
	wr_code int NOT NULL,
	wr_name varchar(100) NOT NULL
);

ALTER TABLE Book
    ADD CONSTRAINT pk_book PRIMARY KEY (bk_code);
ALTER TABLE Edition
    ADD CONSTRAINT pk_edition PRIMARY KEY (ed_code);
ALTER TABLE Writer
    ADD CONSTRAINT pk_writer PRIMARY KEY (wr_code);
ALTER TABLE BookWriter  WITH CHECK ADD  CONSTRAINT FK_BookWriter_Book FOREIGN KEY(bk_code)
    REFERENCES Book (bk_code);
ALTER TABLE BookWriter  WITH CHECK ADD  CONSTRAINT FK_BookWriter_Writer FOREIGN KEY(wr_code)
    REFERENCES Writer (wr_code);
ALTER TABLE Edition  WITH CHECK ADD  CONSTRAINT FK_Edition_Book FOREIGN KEY(bk_code)
    REFERENCES Book (bk_code);