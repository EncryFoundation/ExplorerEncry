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
  equihashSolution INTEGER ARRAY NOT NULL
);

CREATE TABLE headerToNode (
  id VARCHAR(64) REFERENCES headers (id),
  nodeIp VARCHAR(128) REFERENCES nodes (ip)
);

CREATE INDEX header_id_to_node_index ON headerToNode (id);

CREATE INDEX height_index ON headers (height);

CREATE TABLE transactions(
  id VARCHAR(64) PRIMARY KEY,
  fee BIGINT NOT NULL,
  blockId VARCHAR(64) REFERENCES headers (id),
  coinbase BOOLEAN NOT NULL,
  timestamp BIGINT NOT NULL,
  proof TEXT
);

CREATE INDEX block_id_index ON transactions (blockId);

CREATE TABLE inputs(
  bxId VARCHAR(64) PRIMARY KEY,
  txId VARCHAR(64) REFERENCES transactions (id),
  contract VARCHAR(1024) NOT NULL,
  proofs VARCHAR(1024) NOT NULL
);

CREATE INDEX tx_id_inputs_index ON inputs (txId);

CREATE TABLE inputsToNodes(
  inputId VARCHAR(64) REFERENCES inputs (bxId),
  nodeIp VARCHAR(128) REFERENCES nodes (ip)
);

CREATE INDEX inputId_inputsToNodes_index ON inputsToNodes (inputId);

CREATE INDEX nodeIp_inputsToNodes_index ON inputsToNodes (nodeIp);

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

CREATE TABLE outputsToNodes(
  outputId VARCHAR(64) REFERENCES outputs (id),
  nodeIp VARCHAR(128) REFERENCES nodes (ip)
);

CREATE TABLE directive(
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