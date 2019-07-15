DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

CREATE TABLE nodes(
-- idx SERIAL,
  ip VARCHAR(128) PRIMARY KEY,
  status BOOLEAN NOT NULL,
  lastFullBlock VARCHAR(64) NOT NULL,
  lastFullHeight INT NOT NULL
);

CREATE TABLE headers(
-- idx SERIAL,
  id VARCHAR(64) PRIMARY KEY,
  version INTEGER NOT NULL,
  parent_id VARCHAR(64) NOT NULL,
  transactionsRoot VARCHAR(64) NOT NULL,
  timestamp BIGINT NOT NULL,
  height INTEGER NOT NULL,
  nonce BIGINT NOT NULL,
  difficulty BIGINT NOT NULL,
  equihashSolution INTEGER ARRAY NOT NULL,
  txCount INTEGER NOT NULL,
  minerAddress VARCHAR NOT NULL,
  minerReward BIGINT NOT NULL
);

CREATE TABLE headerToNode(
-- idx SERIAL,
  id VARCHAR(64) REFERENCES headers (id),
  nodeIp VARCHAR(128) REFERENCES nodes (ip)
);

CREATE INDEX header_id_to_node_index ON headerToNode (id);

CREATE INDEX height_index ON headers (height);

CREATE TABLE transactions(
-- idx SERIAL,
  id VARCHAR(64) PRIMARY KEY,
  fee BIGINT NOT NULL,
  blockId VARCHAR(64) REFERENCES headers (id),
  coinbase BOOLEAN NOT NULL,
  timestamp BIGINT NOT NULL,
  proof TEXT
);

CREATE INDEX block_id_index ON transactions (blockId);

CREATE TABLE inputs(
-- idx SERIAL,
  bxId VARCHAR(64) PRIMARY KEY,
  txId VARCHAR(64) REFERENCES transactions (id),
  contract VARCHAR NOT NULL,
  proofs VARCHAR NOT NULL
);

CREATE INDEX tx_id_inputs_index ON inputs (txId);

CREATE TABLE accounts(
-- idx SERIAL,
  contractHash VARCHAR(64) PRIMARY KEY
);

CREATE TABLE accBalances(
  contractHash VARCHAR(64) PRIMARY KEY,
  balance BIGINT NOT NULL
);

CREATE TABLE tokens(
-- idx SERIAL,
  id VARCHAR(64) PRIMARY KEY
);

CREATE TABLE outputs(
  idx SERIAL,
  id VARCHAR(64) PRIMARY KEY,
  boxType INT NOT NULL,
  txId VARCHAR(64) REFERENCES transactions (id),
  monetaryValue BIGINT NOT NULL,
  nonce BIGINT NOT NULL,
  coinId VARCHAR(64) REFERENCES tokens (id),
  contractHash VARCHAR(64) REFERENCES accounts (contractHash),
  data VARCHAR(1024),
  isActive BOOLEAN NOT NULL,
  minerAddress VARCHAR(64) NOT NULL
);

CREATE INDEX txId_outputs_index ON outputs (txId);
CREATE INDEX coinId_outputs_index ON outputs (coinId);
CREATE INDEX contractHash_outputs_index ON outputs (contractHash);

CREATE TABLE directives(
-- idx SERIAL,
  tx_id VARCHAR(64) REFERENCES transactions (id),
  number_in_tx INTEGER NOT NULL,
  type_id SMALLINT NOT NULL,
  is_valid BOOLEAN NOT NULL,
  contract_hash TEXT NOT NULL,
  amount BIGINT NOT NULL,
  address TEXT NOT NULL,
  token_id_opt TEXT,
  data_field TEXT NOT NULL,
  PRIMARY KEY (tx_id, number_in_tx)
);

create table wallet(
-- idx SERIAL,
hash varchar(64) NOT NULL ,
amount bigint NOT NULL,
tokenId varchar(64) NOT NULL,
PRIMARY KEY (hash, tokenId));

CREATE INDEX tokenId_wallet_index ON wallet (tokenId);
CREATE INDEX hash_wallet_index ON wallet (hash);

CREATE FUNCTION emp_stamp() RETURNS trigger AS $emp_stamp$

DECLARE
oldVal bigint DEFAULT  0;
newVal bigint DEFAULT  0;
hashh varchar(64) DEFAULT '';
tok varchar(64) DEFAULT '';
rowss int;

BEGIN
IF (TG_OP = 'INSERT') THEN
        rowss := (select * from tmp11(NEW.contractHash, NEW.coinId));
             IF     rowss = 0 THEN oldVal := 0;
             ELSEIF rowss > 0 THEN oldVal := (SELECT amount FROM wallet where hash = NEW.contractHash AND tokenId = NEW.coinId);
        END IF;
        newVal := oldVal + NEW.monetaryValue;
		hashh := NEW.contractHash;
		tok := NEW.coinId;
		INSERT INTO wallet(hash, amount, tokenId) values(hashh, newVal, tok)
		ON CONFLICT (hash, tokenId) DO UPDATE SET amount = newVal;
		rowss := 0;
        RETURN NEW;

ELSEIF (TG_OP = 'DELETE') THEN
        rowss := (select * from tmp11(OLD.contractHash, OLD.coinId));
		oldVal := (SELECT amount FROM wallet where hash = OLD.contractHash AND tokenId = OLD.coinId);
        newVal := oldVal - OLD.monetaryValue;
		hashh := OLD.contractHash;
		tok := OLD.coinId;
		INSERT INTO wallet(hash, amount, tokenId) values(hashh, newVal, tok)
		ON CONFLICT (hash, tokenId) DO UPDATE SET amount = newVal;
		rowss := 0;
        RETURN OLD;
		END IF;
    END;
$emp_stamp$ LANGUAGE plpgsql;

CREATE TRIGGER emp_stamp AFTER INSERT OR DELETE ON outputs
    FOR EACH ROW EXECUTE PROCEDURE emp_stamp();

    CREATE OR REPLACE FUNCTION tmp11(i varchar(64), b varchar(64))
RETURNS int AS $$
DECLARE
numrows int;
BEGIN
perform amount from wallet where hash = i AND tokenId = b;
GET DIAGNOSTICS numrows := ROW_COUNT;
IF numrows = 0 THEN numrows := 0;
END IF;
RETURN numrows;
END;
$$ LANGUAGE 'plpgsql';
