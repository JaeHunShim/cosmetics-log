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
   
	2.2. Debezium 커넥터 설정 (.json)
   
	kafka-connect에게 '어떤 데이터를 어디서 가져와서 어떻게 Kafka로 보낼지'를 지시하는 상세 명세서입니다.
	mysql-user-source.json (CDC용)
	users 테이블의 변경사항을 감지하여, unwrap 변환을 통해 순수 데이터만 Kafka로 전송합니다.

		# ./kafka-connect-config/mysql-user-source.json
		{
		  "name": "mysql-user-source",
		  "config": {
		    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
		    "tasks.max": "1",
		    "database.hostname": "mysql",
		    "database.port": "3306",
		    "database.user": "root",
		    "database.password": "root",
		    "database.server.id": "1",
		    "topic.prefix": "dream-mysql-users",
		    "database.include.list": "dream",
		    "table.include.list": "dream.users",
		    "schema.history.internal.kafka.bootstrap.servers": "kafka:9093",
		    "schema.history.internal.kafka.topic": "schema-changes.users",
		    "transforms": "unwrap",
		    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
		    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
		    "key.converter.schemas.enable": "false",
		    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
		    "value.converter.schemas.enable": "false"
		  }
		}

	mysql-orders-source.json (이벤트 발행용)
	orders 테이블의 변경사항을 감지하여, 애플리케이션이 처리할 수 있도록 Kafka로 이벤트를 전송합니다. decimal.handling.mode 설정이 핵심입니다.

		# ./kafka-connect-config/mongodb-sink.json
		{
		  "name": "mongodb-sink",
		  "config": {
		    "connector.class": "com.mongodb.kafka.connect.MongoSinkConnector",
		    "tasks.max": "1",
		    "topics": "dream-mysql-users.dream.users",
		    "connection.uri": "mongodb://mongodb:27017",
		    "database": "dream",
		    "collection": "users",
		    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
		    "key.converter.schemas.enable": "false",
		    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
		    "value.converter.schemas.enable": "false"
		  }
		}

3. 🧪 시스템 실행 및 검증 (Workflow)
   
	3.1. 시스템 초기화
   
	⚠️ 주의: -v 옵션은 모든 데이터를 삭제하므로, 기존 데이터 유지가 필요하다면 사용하지 마세요.

		# docker 폴더로 이동
		cd ./docker
		
		# 모든 컨테이너와 볼륨을 삭제
		docker-compose down -v


	3.2. 애플리케이션 빌드

		   # Spring Boot 프로젝트 폴더로 이동
		cd ../cosmetics-event-consumer
		
		# Gradle을 사용하여 프로젝트를 빌드
		./gradlew build

	3.3. 인프라 시작

		   # 다시 docker 폴더로 이동
		cd ../docker
		
		# 모든 인프라 서비스를 시작
		docker-compose up -d --build

	3.4. 커넥터 등록
	💡 kafka-connect 서비스가 완전히 시작된 후 실행해야 합니다. docker-compose logs -f kafka-connect로 로그를 확인하세요.

		# 프로젝트 루트 폴더로 이동
		cd ..
		
		# User Source 커넥터 등록
		curl -X POST -H "Content-Type: application/json" --data @kafka-connect-config/mysql-user-source.json http://localhost:8083/connectors
		sleep 5
		
		# MongoDB Sink 커넥터 등록
		curl -X POST -H "Content-Type: application/json" --data @kafka-connect-config/mongodb-sink.json http://localhost:8083/connectors
		sleep 5
		
		# Order Source 커넥터 등록
		curl -X POST -H "Content-Type: application/json" --data @kafka-connect-config/mysql-orders-source.json http://localhost:8083/connectors

	3.5. 애플리케이션 실행 및 검증
   
	IDE를 통해 CosmeticsEventConsumerApplication을 실행합니다.
	
	CDC 검증: MySQL users 테이블에 데이터를 삽입/수정하고, MongoDB dream.users 컬렉션에 실시간으로 반영되는지 확인합니다.
	
	이벤트 처리 검증: MySQL orders 테이블에 status가 PAID인 데이터를 삽입하고, Spring Boot 앱 콘솔 로그와 MongoDB cosmetics_events.order_summaries 컬렉션을 확인합니다.

4. 🔍 트러블슈팅 요약 (Troubleshooting Summary)

| 문제 현상                 | 원인 분석                                               | 해결책                                                                                         |
| --------------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| UnknownHostException  | Kafka 리스너가 Docker 내부용으로만 설정됨                        | Kafka 리스너를 내부용/외부용으로 분리하고, 외부용 advertised listener에localhost를 명시.                           |
| topic creation failed | 단일 노드 환경에서 복제 계수 미지정                                | kafka-connect환경 변수에REPLICATION_FACTOR를1로 명시적으로 설정.                                          |
| payload필드null         | Debezium의 Envelope 메시지 구조와 Java DTO 불일치             | Debezium 기본 메시지 구조와 1:1로 매핑되는 새로운 DTO(DebeziumMessage)를 정의하고,TypeReference를 사용하여 역직렬화.      |
| NumberFormatException | Debezium의decimal.handling.mode기본값이precise(인코딩된 문자열) | mysql-orders-source.json에decimal.handling.mode를double로 설정하고, Java DTO 필드 타입을BigDecimal로 수정. |
