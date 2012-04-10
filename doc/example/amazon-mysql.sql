CREATE TABLE Publ (
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    primary key(name)
);

CREATE TABLE Recorder (
    recname VARCHAR(100) NOT NULL,
    primary key(recname)
);

CREATE TABLE Book (
    title VARCHAR(100) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    currency VARCHAR(20) NOT NULL,    
    isbn VARCHAR(20) NULL,
    author VARCHAR(100) NULL,
    publisher VARCHAR(100) NULL,
    PRIMARY KEY(title),
    FOREIGN KEY(publisher)
        REFERENCES Publ(name)
);

CREATE TABLE Music (
    title VARCHAR(100) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    currency VARCHAR(20) NOT NULL,    
    rec VARCHAR(100) NOT NULL,
    PRIMARY KEY(title),
    FOREIGN KEY(rec)
        REFERENCES Recorder(recname)
);

CREATE TABLE Video (
    title VARCHAR(100) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    currency VARCHAR(20) NOT NULL,
    PRIMARY KEY(title)
);

CREATE TABLE PC_HW (
    title VARCHAR(100) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    currency VARCHAR(20) NOT NULL,
    PRIMARY KEY(title)
);

INSERT INTO Publ
    VALUES ('CAMPUS', 'Rua 7 Setembro, 111 an 16- Centro - Rio de Janeiro - RJ');
    
INSERT INTO Book
    VALUES ('VERDADES FUNDAMENTAIS SOBRE A NATUREZA DO LIDER', 59.9, 'REAL', '978-85-352-4150-1', 'Barry Posner', 'CAMPUS');
    
INSERT INTO Book
    VALUES ('Biblia Sagrada Edicao Pastoral', 40.0, 'REAL', '978-85-352-4150-1', 'God', 'CAMPUS');

INSERT INTO Recorder
    VALUES ('Universal Music Argentina S.A.');
    
INSERT INTO Music
    VALUES ('TUDO AZUL', 1.5, 'REAL', 'Universal Music Argentina S.A.');
    
INSERT INTO PC_HW
    VALUES ('Kindle', 139, 'DOLAR');
