# VNPAY Recurring Demo

Dang ky, thanh toan ky, cap nhat va huy recurring.

## Requirements

- Java 17
- Maven 3.8+
- PostgreSQL 13+

## Configure

Edit src/main/resources/application.properties directly before running the demo.

H2 database defaults are already included, so partners do not need to create PostgreSQL:

``text
server.port=9999
spring.datasource.url=jdbc:h2:file:./data/vnpay-demo-db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
spring.datasource.username=sa
spring.datasource.password=
``

H2 console is available at http://localhost:9999/h2-console.

Fill VNPAY properties that match these prefixes:

- $prefix

Credential fields are intentionally blank. Sandbox URLs and versions are prefilled in pplication.properties.

## Run

``bash
mvn spring-boot:run
``

Open http://localhost:9999.

## Demo endpoints

- $(System.Collections.Hashtable.Path) - Dang ky thanh toan dinh ky
- $(System.Collections.Hashtable.Path) - Thanh toan ky
- $(System.Collections.Hashtable.Path) - Cap nhat the
- $(System.Collections.Hashtable.Path) - Cap nhat so ky
- $(System.Collections.Hashtable.Path) - Huy dang ky