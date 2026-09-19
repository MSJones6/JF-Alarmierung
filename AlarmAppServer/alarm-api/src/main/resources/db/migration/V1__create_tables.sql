CREATE TABLE connections (
	id UUID PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE,
	use_ssl BOOLEAN NOT NULL DEFAULT FALSE,
	broker_host VARCHAR(255) NOT NULL,
	broker_port VARCHAR(16) NOT NULL,
	broker_path VARCHAR(255) NOT NULL DEFAULT '/mqtt',
	broker_user VARCHAR(255) NOT NULL DEFAULT '',
	password VARCHAR(255) NOT NULL DEFAULT '',
	mqtt_topic VARCHAR(255) NOT NULL,
	sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE keywords (
	id UUID PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE,
	sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE alarms (
	id UUID PRIMARY KEY,
	scheduled_at VARCHAR(32) NOT NULL,
	connection_name VARCHAR(255) NOT NULL,
	location VARCHAR(255) NOT NULL,
	keyword VARCHAR(255) NOT NULL,
	info TEXT NOT NULL DEFAULT '',
	status VARCHAR(32) NOT NULL,
	created_at TIMESTAMPTZ NOT NULL,
	updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_alarms_status ON alarms (status);
CREATE INDEX idx_alarms_scheduled_at ON alarms (scheduled_at);
