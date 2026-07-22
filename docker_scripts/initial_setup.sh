#!/bin/bash
docker run -d --name oracle-xe -p 1521:1521 \
-e ORACLE_PASSWORD=oracle \
-e APP_USER=sgfte \
-e APP_USER_PASSWORD=sgfte --platform linux/arm64 gvenzl/oracle-free:23-slim
