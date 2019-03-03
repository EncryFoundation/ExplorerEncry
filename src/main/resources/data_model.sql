CREATE TABLE nodes(
  ip VARCHAR(128) PRIMARY KEY,
  status BOOLEAN NOT NULL,
  lastFullBlock VARCHAR(64) NOT NULL,
  lastFullHeight INT NOT NULL
);

CREATE TABLE headers(
  id VARCHAR(64) PRIMARY KEY,
  version SMALLINT NOT NULL,
  parent_id VARCHAR(64) NOT NULL,
  adProofsRoot VARCHAR(64) NOT NULL,
  stateRoot VARCHAR(66) NOT NULL,
  transactionsRoot VARCHAR(64) NOT NULL,
  timestamp BIGINT NOT NULL,
  height INTEGER NOT NULL,
  nonce BIGINT NOT NULL,
  difficulty BIGINT NOT NULL,
  equihashSolution INTEGER ARRAY NOT NULL,
  nodes VARCHAR(256) ARRAY NOT NULL
);

CREATE INDEX height_index ON headers (height);

CREATE TABLE transactions(
  id VARCHAR(64) PRIMARY KEY,
  fee BIGINT NOT NULL,
  blockId VARCHAR(64) REFERENCES headers (id),
  coinbase BOOLEAN NOT NULL,
  proof TEXT
);

CREATE INDEX block_id_index ON transactions (blockId);

CREATE TABLE inputs(
  id VARCHAR(64) PRIMARY KEY,
  txId VARCHAR(64) REFERENCES transactions (id),
  contract TEXT NOT NULL,
  proofs VARCHAR NOT NULL
);

CREATE INDEX tx_id_inputs_index ON inputs (txId);

CREATE TABLE accounts(
  contractHash VARCHAR(64) PRIMARY KEY
);

CREATE TABLE tokens(
  id VARCHAR(64) PRIMARY KEY
);

CREATE TABLE outputs(
  id VARCHAR(64) PRIMARY KEY,
  txId VARCHAR(64) REFERENCES transactions (id),
  monetaryValue BIGINT NOT NULL,
  coinId VARCHAR(64) REFERENCES tokens (id),
  contractHash VARCHAR(64) REFERENCES accounts (contractHash),
  data VARCHAR,
  isActive BOOLEAN NOT NULL
);

CREATE INDEX txId_outputs_index ON outputs (txId);
CREATE INDEX coinId_outputs_index ON outputs (coinId);
CREATE INDEX contractHash_outputs_index ON outputs (contractHash);