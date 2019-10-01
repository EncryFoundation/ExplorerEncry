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
  stateRoot VARCHAR(64) NOT NULL,
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

CREATE TABLE contracts(
   hash VARCHAR(64) PRIMARY KEY,
   contract TEXT
);

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

CREATE FUNCTION recount_wallet() RETURNS trigger AS $recount_wallet$

BEGIN
IF (TG_OP = 'INSERT') THEN
		INSERT INTO wallet AS w(hash, amount, tokenId) values(NEW.contractHash, NEW.monetaryValue, NEW.coinId)
		ON CONFLICT (hash, tokenId) DO UPDATE SET amount = w.amount + NEW.monetaryValue;
        RETURN NEW;

ELSEIF (TG_OP = 'DELETE') THEN
        INSERT INTO wallet AS w(hash, amount, tokenId) values(NEW.contractHash, NEW.monetaryValue, NEW.coinId)
        		ON CONFLICT (hash, tokenId) DO UPDATE SET amount = w.amount - NEW.monetaryValue;
        RETURN OLD;
		END IF;
    END;
$recount_wallet$ LANGUAGE plpgsql;

CREATE TRIGGER recount_wallet AFTER INSERT OR DELETE ON outputs
    FOR EACH ROW EXECUTE PROCEDURE recount_wallet();
