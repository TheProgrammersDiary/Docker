# Global Tests/Docker

## Summary
Library used to test Programmer's diary blogging application.

Also acts as a launcher for all microservices.

## Docker launch
Clone all other repos: blog, post, security, frontend. Place them along this one as sibling folders.

You will need to setup env variables injected in docker-compose with ${env_variable_name}.

To run blogging system, use: `docker-compose up -d --build`.

To launch logging services along with the system, use: `docker-compose up -d --build --profile debug`.

## Open to contribution
This repo accepts pull requests from forks.