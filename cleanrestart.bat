@echo off
call docker-compose stop
call docker-compose rm -f
call docker system prune -f
call docker volume prune -f

cd ../monolith/blog
call mvn clean package

cd ../../post/post
call mvn clean package

cd ../../docker
call docker-compose up -d --build

PAUSE