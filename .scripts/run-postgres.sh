docker rm -f postgres-demo
docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres --name postgres-demo -d postgres -c "shared_preload_libraries=pg_stat_statements"