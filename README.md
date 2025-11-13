📖 Kafka & Debezium 기반 하이브리드 CDC/이벤트 아키텍처 구축 가이드
본 문서는 MySQL 데이터베이스 변경 사항을 실시간으로 감지하여, 일부 데이터는 **단순 복제(CDC)**하고 다른 데이터는 비즈니스 이벤트를 발생시켜 별도의 애플리케이션에서 처리하는 하이브리드 아키텍처 구축 과정과 트러블슈팅 경험을 상세히 기술합니다.


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
