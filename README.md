# 패스트 캠퍼스 Spring Batch 프로젝트

Spring Batch를 사용한 프로젝트를 따라 만들어보고, 이후 실무 프로젝트에 적용하기 위해 만든 프로젝트.

### 사용 기술

- JAVA
- Spring Boot 3.2
- MySQL
- JPA

### 공부한 내용

- docker-compose.yml 작성법
  - https://hub.docker.com 에 이미지를 검색한 내용을 참고.
  - 컨테이너는 삭제될 때 마다 데이터도 삭제된다. 따라서 영구적 보관이 필요한 데이터는 볼륨을 만들어 관리한다.
    - DB 설정 파일 : custom.cnf
    - DDL 쿼리, 초키 데이터
- Batch 관련 설정
  - application.yml
  - 