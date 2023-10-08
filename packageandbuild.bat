@echo off

cd ../monolith/blog
call mvn clean package

cd ../../post/post
call mvn clean package

cd ../../docker
call docker-compose up -d --build

PAUSE