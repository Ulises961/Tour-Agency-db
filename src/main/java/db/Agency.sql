
CREATE TABLE Guide (
    name VARCHAR (100),
    code SERIAL PRIMARY KEY,
    telephone NUMERIC UNIQUE,
    base VARCHAR(50) 
); 



CREATE TABLE Base(
    CAP VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100)
); 

CREATE TABLE Tour(
    code SERIAL,
    name VARCHAR (100),
    start_time TIME,
    end_time TIME,
    city VARCHAR(50),
    PRIMARY KEY (code, start_time, end_time)
);


CREATE TABLE City(
    CAP VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100)
); 

ALTER TABLE Tour ADD CONSTRAINT cityIsFk_inTour FOREIGN KEY (city) 
        REFERENCES City(CAP)
        ON UPDATE CASCADE ON DELETE RESTRICT
        DEFERRABLE INITIALLY DEFERRED;
 
ALTER TABLE Base ADD CONSTRAINT CityIsFk_inBase FOREIGN KEY (CAP) 
        REFERENCES City(CAP)
        ON UPDATE CASCADE ON DELETE RESTRICT
        DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE Guide ADD CONSTRAINT BaseIsFK_inGuide FOREIGN KEY (base) 
        REFERENCES Base(CAP)
        ON UPDATE CASCADE ON DELETE RESTRICT
        DEFERRABLE INITIALLY DEFERRED;



CREATE TABLE Client(
    tax_code NUMERIC PRIMARY KEY,
    name VARCHAR(100)
); 


CREATE TABLE Reserves(
    groups INTEGER,
    date DATE,
    client NUMERIC,
    tour INTEGER,
    start_time TIME,
    end_time TIME,
    price NUMERIC,


    CONSTRAINT ResevesPK PRIMARY KEY (groups, date),

    CONSTRAINT ClientFK_inReserves FOREIGN KEY (client) 
        REFERENCES Client(tax_code)
        ON UPDATE CASCADE ON DELETE CASCADE
        DEFERRABLE INITIALLY DEFERRED
);
        
CREATE TABLE Delivers(
    guide INTEGER,
    tour INTEGER,
    start_time TIME,
    end_time TIME,
    groups INTEGER, 
    date DATE,
    guide_honorary NUMERIC,
    
    PRIMARY KEY(groups, date),
    CONSTRAINT GuideIsFK_inDelivers FOREIGN KEY (guide)
        REFERENCES Guide(code) 
        ON UPDATE CASCADE 
        ON DELETE NO ACTION 
        DEFERRABLE INITIALLY DEFERRED,
    
    CONSTRAINT Tour_timesAreFK_inDelivers FOREIGN KEY (tour, start_time,end_time)
        REFERENCES Tour(code, start_time, end_time) 
        ON UPDATE CASCADE 
        ON DELETE NO ACTION 
        DEFERRABLE INITIALLY DEFERRED
);



CREATE TABLE Groups(
    group_id INTEGER,
    date DATE,
    num_pax NUMERIC,

    PRIMARY KEY (group_id, date),
   

    CONSTRAINT GroupDateAreFK_inGroup FOREIGN KEY (group_id, date) 
        REFERENCES Reserves(groups, date)
        ON UPDATE CASCADE ON DELETE CASCADE
        DEFERRABLE INITIALLY DEFERRED
);

ALTER TABLE Delivers ADD CONSTRAINT GroupIsFK_inDelivers FOREIGN KEY (groups, date)
        REFERENCES Groups(group_id, date) 
        ON UPDATE CASCADE 
        ON DELETE NO ACTION 
        DEFERRABLE INITIALLY DEFERRED;
        
ALTER TABLE Groups ADD CONSTRAINT IsFK_inDelivers FOREIGN KEY (group_id, date) 
    REFERENCES Delivers(groups, date)
    ON UPDATE CASCADE ON DELETE CASCADE
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE Reserves ADD CONSTRAINT GroupIsFK_inReserves FOREIGN KEY (groups, date) 
    REFERENCES Groups(group_id, date)
    ON UPDATE CASCADE ON DELETE CASCADE
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE Reserves ADD CONSTRAINT TourIsFK_inReserves FOREIGN KEY (tour, start_time, end_time) 
    REFERENCES Tour(code, start_time, end_time)
    ON UPDATE CASCADE ON DELETE CASCADE
    DEFERRABLE INITIALLY DEFERRED;





CREATE TABLE Cruise_ship(
    port_of_departure VARCHAR(100),
    num_pax NUMERIC,
    Tax_Code SERIAL PRIMARY KEY
);


CREATE TABLE has_Email( 
    guide INTEGER,
    email VARCHAR (100) PRIMARY KEY,

    
    CONSTRAINT guideIsFK_inEmail FOREIGN KEY (guide) 
        REFERENCES Guide(code)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE  Language(
    language_id SERIAL PRIMARY KEY,
    name VARCHAR(20)


);

CREATE TABLE is_fluent_in(
    guide INTEGER,
    language INTEGER,
    PRIMARY KEY(guide,language),

    CONSTRAINT languageFK_inIsFluentIn FOREIGN KEY (language) 
        REFERENCES Language(language_id)
        ON UPDATE CASCADE ON DELETE CASCADE
        DEFERRABLE INITIALLY DEFERRED,

    CONSTRAINT guideFK_inIsFluent_In FOREIGN KEY (guide) 
        REFERENCES Guide (code)
        ON UPDATE CASCADE ON DELETE CASCADE
        DEFERRABLE INITIALLY DEFERRED
);


CREATE TABLE  Speaks(
    groups INTEGER,
    date DATE,
    language INTEGER,
    PRIMARY KEY (groups, date, language),

    CONSTRAINT groupIsFK_inSpeaks FOREIGN KEY (groups,date) 
        REFERENCES Groups(group_id,date)
        ON UPDATE CASCADE ON DELETE CASCADE
         DEFERRABLE INITIALLY DEFERRED,

    CONSTRAINT languageIsFK_inSpeaks FOREIGN KEY (language) 
        REFERENCES Language(language_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
        DEFERRABLE INITIALLY DEFERRED
);

CREATE OR REPLACE FUNCTION AvoidOverlappingTours()
    RETURNS TRIGGER AS $$
    DECLARE tour RECORD; oldTour RECORD;
    BEGIN 
        SELECT COUNT(T.guide) AS countTours, T.guide,  date FROM Delivers T INTO tour
        WHERE T.guide = NEW.guide and T.date = NEW.date GROUP BY(T.guide, date);
        SELECT O.start_time AS startTime, O.end_time AS endTime from Delivers O into oldTour
        WHERE  O.guide = NEW.guide and O.date = NEW.date;
    
    --NO MORE THAN 2 TOURS ON A SPECIFIC DATE
        IF tour.countTours > 2 THEN RAISE EXCEPTION 'The guide % has been assigned two tours on the date, please try again', tour.guide; 
        END IF;

    --IF THERE IS ALREADY A FULL DAY TOUR ON THAT DATE WE CANCEL TRANSACTION
        IF oldTour.endTime - oldTour.startTime > '03:30' THEN RAISE EXCEPTION 'The guide % has been assigned a full day tour on the date, please try again', tour.guide; 
        END IF;
    
    --IF THERE IS A HALF DAY TOUR ALREADY BOOKED THE NEW TOUR CANNOT BE FULL DAY 
        IF tour.countTours = 1 AND NEW.end_time - NEW.start_time > '03:30' THEN  RAISE EXCEPTION 'The guide % has been assigned a half day tour on the date, please try again', tour.guide; 
            END IF;
    --TWO HALF DAY TOURS MUST NOT OVERLAP 
        IF oldTour.startTime < NEW.start_time THEN
            IF (oldTour.endTime > NEW.start_time) THEN RAISE EXCEPTION 'The guide % is on tour, please try again', tour.guide; 
            
            ELSE RETURN NEW;
            END IF;
    
        ELSE 
            IF ( NEW.end_time > oldTour.startTime  ) THEN RAISE EXCEPTION 'The guide % is on tour, please try again', tour.guide;
            
            ELSE RETURN NEW;
            END IF;
        END IF;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION monthlyTranseferLimit()
    RETURNS TRIGGER AS $$
    DECLARE transfers INTEGER;
    BEGIN        
    SELECT count(*)
    FROM Guide G, Delivers D, Tour T INTO transfers
    WHERE G.code = NEW.guide 
    AND NEW.tour = T.code AND G.code = NEW.guide 
    AND G.base <> T.city AND 
    EXTRACT (MONTH FROM NEW.date)  IN (
        SELECT EXTRACT (MONTH FROM D.date) 
        WHERE D.guide = NEW.guide
    ) group by G.name;
        
    IF transfers >= 3 
        THEN RAISE EXCEPTION 'The guide % has reached the limit of % monthly transfers, please select another guide', NEW.guide, transfers;
        RETURN NULL;
    ELSE 
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER checkConstraintsOnDelivers BEFORE INSERT ON Delivers
FOR EACH ROW EXECUTE PROCEDURE AvoidOverlappingTours();

CREATE TRIGGER CheckMontlhyTransferLimit BEFORE INSERT ON Delivers
FOR EACH ROW EXECUTE PROCEDURE monthlyTranseferLimit();


BEGIN TRANSACTION;


INSERT INTO Tour (name, start_time, end_time,city,code) VALUES

('Visite chiese di Bari', '08:00', '11:30','FF443', DEFAULT),
('Tour in Barca nel Golfo di Napoli ', '10:00', '17:30', 'AE623', DEFAULT),
('Visite alle rovine di Pompei ed Ercolano', '08:30', '16:00', 'EF576', DEFAULT),
('City tour centro storico Roma', '14:00', '17:30', '2A547', DEFAULT),
('Tour alla fortezza Michelangelo', '15:00', '18:30', 'FK766',DEFAULT),
('Visita centro storico Pisa', '15:00', '18:30','BY988', DEFAULT);


INSERT INTO Base(CAP, name) Values

('FK766', 'Civita Vecchia'),
('2A547', 'Ostia'),
('BY988', 'Tirrenia'),
('KT692', 'Genova'),
('EP484', 'Pompei'),
('RZ329', 'Massa'),
('FU332', 'La Spezia'),
('FY987', 'Sestri Levante'),
('U4679', 'Savona');

INSERT INTO City(CAP, name) VALUES

('FF443', 'Bari'),
('AE623', 'Capri'),
('EF576', 'Napoli'),
('2A547', 'Ostia'),
('FK766', 'Civita Vecchia'),
('BY988', 'Tirrenia'),
('KT692', 'Genova'),
('EP484', 'Pompei'),
('RZ329', 'Massa'),
('FU332', 'La Spezia'),
('FY987', 'Sestri Levante'),
('U4679', 'Savona'),
('8F962', 'Catania'),
('N4294', 'Palermo'),
('9Z758', 'Siracusa');

INSERT INTO Guide(name,code, telephone,base) VALUES

('Carlo', 101, 470747766, 'FK766'),
('Mario', 102, 470152767, 'FK766'),
('Jane', 103, 470858796, 'FK766'),
('Lana', 104, 470348801, '2A547'),
('Steve', 105, 470839239, '2A547'),
('Jenny', 106, 470501118, '2A547'),
('Maria', 107, 470484590, 'BY988'),
('Silke', 108, 470502161, 'BY988'),
('Ivan', 109, 470261124, 'KT692'),
('Giovanni', 110, 470407676, 'KT692'),
('Kjell', 111, 470104889, 'EP484'),
('Anna', 112, 470511430, 'EP484'),
('Cesar', 113, 470351254, 'RZ329'),
('Eduard', 114, 470308547, 'RZ329'),
('Riccardo', 115, 470699742, 'FU332'),
('Octavio', 116, 470198830, 'FU332'),
('Franz', 117, 470742408, 'FY987'),
('Louis', 118, 470948730, 'FY987'),
('Flavio', 119, 470628352, 'U4679'),
('Barbara', 120, 470778414, 'U4679'),
('Guido', 121, 475456124, 'U4679'),
('Bob', 122, 475456133, 'FY987'),
('Clara', 123, 475456113, 'EP484'),
('Matt', 124, 470000001, 'BY988'),
('Matteo', 125, 478996456, 'KT692'),
('Giorgio', 126, 476123456, 'FU332'),
('Robert', 127, 475698654, 'EP484'),
('Chiara', 128, 475654987, 'U4679');

INSERT INTO has_Email(guide, email) VALUES

(104,'Lana@yahoo.com'),
(104,'Lana_1@hotmail.com'),
(118,'Louis@yahoo.com'),
(107,'Maria@hotmail.com'),
(112,'Anna@alice.com'),
(109,'Ivan@alice.com'),
(110,'Giovanni@yahoo.com'),
(120,'Barbara@hotmail.com'),
(117,'Franz@yahoo.com');

INSERT INTO is_fluent_In(guide,language) VALUES

(101, 2),
(101, 9),
(101, 7),
(102, 4),
(102, 3),
(102, 2),
(103, 11),
(103, 9),
(103, 6),
(103, 10),
(103, 3),
(104, 3),
(104, 7),
(104, 1),
(104, 6),
(105, 7),
(105, 5),
(105, 11),
(106, 5),
(106, 1),
(106, 3),
(107, 7),
(107, 9),
(107, 8),
(107, 3),
(108, 3),
(108, 11),
(108, 6),
(108, 10),
(109, 2),
(109, 4),
(109, 8),
(110, 9),
(110, 1),
(110, 8),
(110, 7),
(110, 5),
(111, 2),
(111, 1),
(111, 8),
(112, 6),
(112, 11),
(112, 2),
(113, 9),
(113, 4),
(113, 3),
(113, 1),
(114, 10),
(114, 7),
(114, 2),
(115, 8),
(115, 2),
(116, 8),
(116, 10),
(116, 6),
(116, 1),
(117, 4),
(117, 5),
(117, 6),
(117, 7),
(117, 8),
(118, 2),
(118, 1),
(118, 7),
(118, 10),
(119, 8),
(119, 10),
(119, 5),
(119, 6),
(119, 2),
(120, 8),
(120, 3),
(120, 10),
(120, 4),
(121, 1),
(121, 2),
(121, 3),
(122, 1),
(122, 2),
(122, 3),
(123, 2),
(123, 4),
(123, 5),
(124, 1),
(124, 11),
(124, 8),
(124, 5),
(125, 5),
(125, 2),
(125, 1),
(126, 2),
(126, 9),
(126, 11),
(127, 1),
(127, 2),
(127, 3),
(128, 1),
(128, 2),
(128, 9);

INSERT INTO Language(name, language_id) VALUES

('Italian', 1),
('German', 2),
('English', 3),
('Spanish', 4),
('French', 5),
('Dutch', 6),
('Portuguese', 7),
('Danish', 8),
('Chinese', 9),
('Russian', 10),
('Japanese', 11);

INSERT INTO Client(tax_code, name) VALUES

(25, 'Aida Aura'),
(99,'Costa Deliziosa'),
(52, 'MSC Musica'),
(38, 'MSC Opera'  ),
(97, 'MSC Orchestra'),
(32, 'Costa Luminosa'),
(87, 'Costa Favolosa'),
(33,'Costa Magica'),
(85,'Cunard Elisabeth'),
(54, 'Mein Schiff'),
(75,'Agentour'),
(83, 'VisitUs'),
(67, 'TUI'),
(59, 'ShoreToShore'),
(23, 'Vistiing'),
(77, 'LastMinute'),
(57, 'FlyToGo'),
(55, 'Discover'),
(22, 'The World'),
(92, 'Wondertrips'),
(65, 'WonderLost');

INSERT INTO Cruise_ship (tax_code,port_of_departure,num_pax) VALUES
(25, 'Hamburg',3500),
(99,'Genova',2800),
(52, 'Napoli',2600),
(38,'Napoli', 2800),
(97, 'Napoli',2500),
(32, 'Napoli',2900),
(87, 'Genova',2450),
(33,'Genova',2300),
(85,'Devon',1800),
(54, 'Kiel',1900);

INSERT INTO Delivers (guide, tour,start_time,end_time,groups, date, guide_honorary) VALUES
(116, 1, '08:00:00', '11:30:00', 1, '2021-10-10', 700),
(113, 2, '10:00:00', '17:30:00', 2, '2021-10-12', 700),
(120, 4, '14:00:00', '17:30:00', 3, '2021-10-31', 500),
(110, 6, '15:00:00', '18:30:00', 4, '2021-10-10', 700),
(102, 4, '14:00:00', '17:30:00', 5, '2021-10-10', 700),
(101, 2, '10:00:00', '17:30:00', 6, '2021-10-13', 500),
(113, 2, '10:00:00', '17:30:00', 7, '2021-10-19', 500),
(113, 2, '10:00:00', '17:30:00', 8, '2021-10-18', 700),
(101, 6, '15:00:00', '18:30:00', 9, '2021-10-14', 700),
(101, 1, '08:00:00', '11:30:00', 10,' 2021-10-02', 1000),
(103, 1, '08:00:00', '11:30:00', 11,' 2021-10-10', 700),
(118, 1, '08:00:00', '11:30:00', 12,' 2021-12-10', 1000),
(103, 1, '08:00:00', '11:30:00', 13,' 2021-10-12', 700),
(101, 1, '08:00:00', '11:30:00', 14,' 2021-12-04', 700),
(124, 4, '14:00:00', '17:30:00', 15,' 2021-12-03', 1000),
(104, 6, '15:00:00', '18:30:00', 16,' 2021-11-10', 700),
(104, 2, '10:00:00', '17:30:00', 17,' 2021-11-25', 700),
(107, 6, '15:00:00', '18:30:00', 18,' 2021-03-14', 600),
(125, 6, '15:00:00', '18:30:00', 19,' 2021-10-12', 700);

INSERT INTO Reserves(groups,date,client,tour,start_time, end_time, price) VALUES
(1, '2021-10-10', 99, 1, '08:00:00', '11:30:00', 15),
(2, '2021-10-12', 87, 2, '10:00:00', '17:30:00', 15),
(3, '2021-10-31', 83, 4, '14:00:00', '17:30:00', 10),
(4, '2021-10-10', 87, 6, '15:00:00', '18:30:00', 15),
(5, '2021-10-10', 97, 4, '14:00:00', '17:30:00', 15),
(6, '2021-10-13', 92, 2, '10:00:00', '17:30:00', 10),
(7, '2021-10-19', 65, 2, '10:00:00', '17:30:00', 10),
(8, '2021-10-18', 87, 2, '10:00:00', '17:30:00', 15),
(9, '2021-10-14', 97, 6, '15:00:00', '18:30:00', 15),
(10, '2021-10-02', 99, 1, '08:00:00', '11:30:00', 15),
(11, '2021-10-10', 67, 1, '08:00:00', '11:30:00', 10),
(12, '2021-12-10', 87, 1, '08:00:00', '11:30:00', 15),
(13, '2021-10-12', 57, 1, '08:00:00', '11:30:00', 10),
(14, '2021-12-04', 22, 1, '08:00:00', '11:30:00', 10),
(15, '2021-12-03', 25, 4, '14:00:00', '17:30:00', 15),
(16, '2021-11-10', 23, 6, '15:00:00', '18:30:00', 10),
(17, '2021-11-25', 67, 2, '10:00:00', '17:30:00', 10),
(18, '2021-03-14', 83, 6, '15:00:00', '18:30:00', 10),
(19, '2021-10-12', 57, 6, '15:00:00', '18:30:00', 10);


INSERT INTO Groups  VALUES
(1, '2021-10-10', 45),
(2, '2021-10-12', 35),
(3, '2021-10-31', 49),
(4, '2021-10-10', 36),
(5, '2021-10-10', 45),
(6, '2021-10-13', 45),
(7, '2021-10-19', 35),
(8, '2021-10-18', 16),
(9, '2021-10-14', 32),
(10, '2021-10-02', 25),
(11, '2021-10-10', 15),
(12, '2021-12-10', 16),
(13, '2021-10-12', 35),
(14, '2021-12-04', 45),
(15, '2021-12-03', 48),
(16, '2021-11-10', 45),
(17, '2021-11-25', 26),
(18, '2021-03-14', 45),
(19, '2021-10-12', 45);

INSERT INTO Speaks VALUES
(1,'2021-10-10',1),
(2,'2021-10-12',4),
(2,'2021-10-12',1),
(3,'2021-10-31',4),
(4,'2021-10-10',5),
(5,'2021-10-10',4),
(6,'2021-10-13',9);


END TRANSACTION;
