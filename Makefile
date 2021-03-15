all: start

.PHONY: start

build:
	@echo "##### Build Ves client #####"
	mvn clean package -Pdocker
	@echo "##### DONE #####"

start:
	@echo "##### Start Ves client #####"
	docker-compose up -d
	@echo "##### DONE #####"

stop:
	@echo "##### Stop Ves client #####"
	docker-compose down
	@echo "##### DONE #####"
