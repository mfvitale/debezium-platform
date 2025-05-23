import { DatabaseType } from "./constants";

/* eslint-disable @typescript-eslint/no-explicit-any */
export const convertMapToObject = (
  map: Map<string, { key: string; value: string }>
) => {
  const obj: { [key: string]: string } = {};

  map.forEach(({ key, value }) => {
    obj[key] = value;
  });

  return obj;
};

export const isEmpty = (obj: any) => {
  return Object.keys(obj).length === 0;
};

export const openDBZJira = () => {
  const newWindow = window.open("https://issues.redhat.com/projects/DBZ/issues", '_blank');
  if (newWindow) {
    newWindow.focus();
  }
}

export const getDatabaseType = (connectorType: string)  => {
  let type =  "";
  switch (true) {
    case connectorType.includes("postgresql"):
      type = DatabaseType.POSTGRESQL;
      break;
    case connectorType.includes("mysql"): 
      type = DatabaseType.MYSQL;
      break;
    case connectorType.includes("mariadb"): 
      type = DatabaseType.MARIADB;
      break;
    case connectorType.includes("sqlserver"):
      type = DatabaseType.SQLSERVER;
      break;
    case connectorType.includes("oracle"):
      type = DatabaseType.ORACLE;
      break;
  }
  return type;
}

export const getConnectorTypeName = (connectorType: string) => {
  let name = "";

  switch (true) {
    case connectorType.includes("mongo"):
      name = "MongoDB";
      break;
    case connectorType.includes("postgre"):
      name = "PostgreSQL";
      break;
    case connectorType.includes("cassandra"):
      name = "Cassandra";
      break;
    case connectorType.includes("mysql"):
      name = "MySQL";
      break;
    case connectorType.includes("sqlserver"):
      name = "SQL Server";
      break;
    case connectorType.includes("apachepulsar"):
      name = "Apache Pulsar";
      break;
    case connectorType.includes("rocketmq"):
      name = "RocketMQ";
      break;
    case connectorType.includes("eventhub"):
      name = "Event Hub";
      break;

    case connectorType.includes("rabbitmq"):
      name = "RabbitMQ";
      break;
    case connectorType.includes("natsstreaming"):
      name = "NATS Streaming";
      break;
    case connectorType.includes("kafka"):
      name = "Kafka";
      break;
    case connectorType.includes("infinispan"):
      name = "Infinispan";
      break;
    case connectorType.includes("pubsublite"):
      name = "Pub/Sub Lite";
      break;
    case connectorType.includes("pubsub"):
      name = "Pub/Sub";
      break;
    case connectorType.includes("pravega"):
      name = "Pravega";
      break;
  }

  return name;
};
