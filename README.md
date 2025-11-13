1. 🏗️ 아키텍처 개요
   
	1.1. 시스템 구성도

		본 프로젝트의 전체적인 데이터 흐름과 시스템 구성은 아래와 같습니다.
		
		(추후 아키텍처 다이어그램 이미지 링크를 여기에 삽입할 수 있습니다.)
		
		Source: MySQL (users, orders 테이블)
		
		Change Data Capture: Debezium MySQL 커넥터
		
		Message Broker: Apache Kafka
		
		Data Sink (CDC): MongoDB Sink 커넥터 (users 테이블 -> users 컬렉션)
		
		Event Consumer: Spring Boot 애플리케이션 (orders 테이블 -> 비즈니스 로직 처리)
		
		Orchestration: Docker Compose

	1.2. 핵심 목표
   
		users 테이블: 변경 시, MongoDB에 그대로 복제 (순수 CDC)
		
		orders 테이블: 변경 시, Spring Boot 앱이 이벤트를 수신하여 커스텀 로직 처리
		
		이중 통신 환경: Docker 내부(kafka-connect)와 로컬 개발 환경(Spring Boot App)에서 동시에 동일한 Kafka 클러스터 접속


2. 🛠️ 최종 구성 파일 (Final Configuration)
   
	2.1. 인프라 오케스트레이션: docker-compose.yml
   
		💡 Tip: KAFKA_LISTENERS와 KAFKA_ADVERTISED_LISTENERS를 내부/외부용으로 분리하는 것이 Docker와 로컬 앱의 동시 접속을 가능하게 하는 핵심입니다.
		# ./docker/docker-compose.yml
		version: '3.9'
		
		services:
		  zookeeper:
		    image: bitnami/zookeeper:latest
		    ports: ["2181:2181"]
		    environment: { ALLOW_ANONYMOUS_LOGIN: "yes" }
		    volumes: [zookeeper_data:/bitnami/zookeeper/data]
		
		  kafka:
		    image: bitnami/kafka:3.6.1
		    ports: ["9092:9092", "9093:9093"]
		    environment:
		      KAFKA_LISTENERS: PLAINTEXT_INTERNAL://:9093,PLAINTEXT_EXTERNAL://:9092
		      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT_INTERNAL://kafka:9093,PLAINTEXT_EXTERNAL://localhost:9092
		      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT_INTERNAL:PLAINTEXT,PLAINTEXT_EXTERNAL:PLAINTEXT
		      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
		      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
		      KAFKA_BROKER_ID: 1
		      ALLOW_PLAINTEXT_LISTENER: "yes"
		      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"
		    depends_on: [zookeeper]
		
		  mysql:
		    image: mysql:8.3
		    platform: linux/x86_64
		    environment: { MYSQL_ROOT_PASSWORD: root, MYSQL_DATABASE: dream }
		    ports: ["3306:3306"]
		    command: --binlog-format=ROW --binlog-row-image=FULL --gtid-mode=ON --enforce-gtid-consistency=ON
		    volumes: [mysql_data:/var/lib/mysql]
		
		  mongodb:
		    image: mongo:latest
		    ports: ["27017:27017"]
		    volumes: [mongo_data:/data/db]
		
		  kafka-connect:
		    build: .
		    ports: ["8083:8083"]
		    environment:
		      CONNECT_BOOTSTRAP_SERVERS: kafka:9093
		      CONNECT_REST_ADVERTISED_HOST_NAME: kafka-connect
		      CONNECT_GROUP_ID: connect-cluster
		      CONNECT_CONFIG_STORAGE_TOPIC: connect-configs
		      CONNECT_OFFSET_STORAGE_TOPIC: connect-offsets
		      CONNECT_STATUS_STORAGE_TOPIC: connect-status
		      CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR: "1"
		      CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR: "1"
		      CONNECT_STATUS_STORAGE_REPLICATION_FACTOR: "1"
		      CONNECT_KEY_CONVERTER: org.apache.kafka.connect.json.JsonConverter
		      CONNECT_VALUE_CONVERTER: org.apache.kafka.connect.json.JsonConverter
		      CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE: "false"
		      CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE: "false"
		      CONNECT_PLUGIN_PATH: /usr/share/java,/usr/share/confluent-hub-components
		    volumes: [kafka-connect-plugins:/usr/share/confluent-hub-components]
		    depends_on: [kafka]
		
		  kafka-ui:
		    image: provectuslabs/kafka-ui:latest
		    ports: ["8080:8080"]
		    environment:
		      KAFKA_CLUSTERS_0_NAME: local
		      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9093
		      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
		    depends_on: [kafka]
		
		volumes:
		  zookeeper_data:
		  mysql_data:
		  mongo_data:
		  kafka-connect-plugins:



