# 패스트 캠퍼스 Spring Batch 프로젝트

Spring Batch를 사용한 프로젝트를 따라 만들어보고, Spring Boot 3.2에 맞게 변경된 부분을 수정함.
이후 실무 프로젝트에 적용하기 위해 만든 프로젝트.

### 사용 기술

- JAVA
- Spring Boot 3.2
- MySQL
- JPA
- Spring Batch 5

### 공부한 내용

- Docker compose
  - https://hub.docker.com 에 이미지를 검색 후 참고해서 작성.
  - 컨테이너는 삭제될 때 마다 데이터도 삭제된다. 따라서 영구적 보관이 필요한 데이터는 볼륨을 만들어 관리한다.
    - DB 설정 파일 : custom.cnf
    - DDL 쿼리, 초키 데이터
  - 로컬/테스트 환경 컨테이너 분리
  - 환경변수 설정
- Batch
  - Job 등록
  - 통합 테스트