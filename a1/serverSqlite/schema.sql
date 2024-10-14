/**
create table bitly (
	shorturl text primary key,
	longurl text not null
);
**/
create table bitly (
	shorturl varchar(128) primary key,
	longurl varchar(128) not null,
	hash text not null
);

CREATE INDEX IF NOT EXISTS idx_shorturl ON bitly(shorturl);
CREATE INDEX IF NOT EXISTS idx_hash ON bitly(hash);
