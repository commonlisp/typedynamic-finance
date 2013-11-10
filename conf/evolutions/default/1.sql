
# --- !Ups

CREATE TABLE bondPortfolio (
  id integer primary key,
  symbol varchar(255) NOT NULL,
  descr text,
  duration float
)

# --- !Downs

DROP TABLE bondPortfolio;
